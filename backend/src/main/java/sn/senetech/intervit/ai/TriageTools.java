package sn.senetech.intervit.ai;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sn.senetech.intervit.domain.*;
import sn.senetech.intervit.repository.KnowledgeArticleRepository;
import sn.senetech.intervit.repository.TicketRepository;
import sn.senetech.intervit.repository.UserRepository;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Outils mis à disposition de l'agent. Principe du moindre privilège : tous sont en LECTURE SEULE,
 * ne renvoient que des données nécessaires au triage et jamais de données personnelles sensibles
 * (pas d'e-mail, pas de mot de passe, pas de téléphone). Les contenus renvoyés sont tronqués.
 */
@Component
public class TriageTools {

    public static final String SEARCH_KB = "search_knowledge_base";
    public static final String LIST_TECHNICIANS = "list_technicians";
    public static final String FIND_SIMILAR = "find_similar_tickets";

    private static final EnumSet<TicketStatus> ACTIVE = EnumSet.of(TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS, TicketStatus.ON_HOLD);
    private static final int MAX_TEXT = 400;

    private final KnowledgeArticleRepository articles;
    private final UserRepository users;
    private final TicketRepository tickets;

    public TriageTools(KnowledgeArticleRepository articles, UserRepository users, TicketRepository tickets) {
        this.articles = articles; this.users = users; this.tickets = tickets;
    }

    /** Déclaration des outils au format attendu par l'API (JSON Schema des paramètres). */
    public List<AnthropicApi.Tool> definitions() {
        return List.of(
            new AnthropicApi.Tool(SEARCH_KB,
                "Recherche des articles publiés dans la base de connaissances interne. Renvoie id, titre et extrait.",
                Map.of("type", "object",
                    "properties", Map.of("query", Map.of("type", "string", "description", "Mots clés (3 à 6 mots)")),
                    "required", List.of("query"))),
            new AnthropicApi.Tool(LIST_TECHNICIANS,
                "Liste les techniciens actifs avec leurs compétences et leur nombre de tickets actifs (charge).",
                Map.of("type", "object", "properties", Map.of())),
            new AnthropicApi.Tool(FIND_SIMILAR,
                "Recherche des tickets déjà résolus ressemblant à la demande. Renvoie référence, titre, catégorie et résumé de résolution.",
                Map.of("type", "object",
                    "properties", Map.of("query", Map.of("type", "string", "description", "Mots clés du symptôme")),
                    "required", List.of("query")))
        );
    }

    /** Exécute un outil ; toute erreur est renvoyée à l'agent comme texte, jamais propagée. */
    @Transactional(readOnly = true)
    public String execute(String name, Map<String, Object> input) {
        try {
            return switch (name) {
                case SEARCH_KB -> searchKb(stringArg(input, "query"));
                case LIST_TECHNICIANS -> listTechnicians();
                case FIND_SIMILAR -> findSimilar(stringArg(input, "query"));
                default -> "Erreur : outil inconnu « " + name + " »";
            };
        } catch (Exception e) {
            return "Erreur lors de l'exécution de l'outil : " + e.getClass().getSimpleName();
        }
    }

    String searchKb(String query) {
        if (query.isBlank()) return "Aucun résultat (requête vide).";
        var page = articles.search(sanitize(query), true, PageRequest.of(0, 5));
        if (page.isEmpty()) return "Aucun article trouvé.";
        return page.getContent().stream()
            .map(a -> "- id=%d | %s | %s".formatted(a.getId(), a.getTitle(), truncate(a.getContent())))
            .collect(Collectors.joining("\n"));
    }

    String listTechnicians() {
        List<User> techs = users.findByRoleAndActiveTrueOrderByFullName(Role.TECHNICIAN);
        if (techs.isEmpty()) return "Aucun technicien actif.";
        return techs.stream()
            .map(u -> "- id=%d | %s | compétences : %s | tickets actifs : %d".formatted(
                u.getId(), u.getFullName(), u.getSkills() == null ? "non renseignées" : u.getSkills(),
                tickets.countByAssigneeIdAndStatusIn(u.getId(), ACTIVE)))
            .collect(Collectors.joining("\n"));
    }

    String findSimilar(String query) {
        if (query.isBlank()) return "Aucun résultat (requête vide).";
        var page = tickets.searchResolved(sanitize(query), PageRequest.of(0, 5));
        if (page.isEmpty()) return "Aucun ticket similaire résolu.";
        return page.getContent().stream()
            .map(t -> "- %s | %s | catégorie=%s | priorité=%s | résolution : %s".formatted(
                t.getReference(), t.getTitle(), t.getCategory() == null ? "?" : t.getCategory().getCode(), t.getPriority(),
                truncate(t.getResolutionSummary() == null ? "" : t.getResolutionSummary())))
            .collect(Collectors.joining("\n"));
    }

    private static String stringArg(Map<String, Object> input, String key) {
        Object v = input == null ? null : input.get(key);
        return v == null ? "" : String.valueOf(v).trim();
    }

    /** Limite la taille et neutralise les caractères de contrôle d'une requête fournie par le modèle. */
    private static String sanitize(String s) {
        String cleaned = s.replaceAll("[\\p{Cntrl}%_]", " ").trim();
        return cleaned.length() > 80 ? cleaned.substring(0, 80) : cleaned;
    }

    private static String truncate(String s) {
        String flat = s.replaceAll("\\s+", " ").trim();
        return flat.length() > MAX_TEXT ? flat.substring(0, MAX_TEXT) + "…" : flat;
    }
}
