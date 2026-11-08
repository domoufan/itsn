package sn.senetech.intervit.ai;

import org.springframework.stereotype.Component;
import sn.senetech.intervit.domain.Role;
import sn.senetech.intervit.domain.Ticket;
import sn.senetech.intervit.domain.TicketStatus;
import sn.senetech.intervit.domain.User;
import sn.senetech.intervit.repository.TicketRepository;
import sn.senetech.intervit.repository.UserRepository;

import java.util.*;

/**
 * SOLUTION DE REPLI (simulation locale, sans IA) : triage par mots clés.
 * Utilisée quand l'API est indisponible, hors budget ou désactivée, et pour les tests automatisés.
 * Les propositions sont marquées avec le fournisseur « heuristic » afin de ne jamais être confondues
 * avec une sortie du modèle.
 */
@Component
public class HeuristicTriageAgent implements TriageAgent {

    private static final Map<String, List<String>> KEYWORDS = new LinkedHashMap<>();
    static {
        KEYWORDS.put("SECURITY", List.of("virus", "phishing", "hameçonnage", "ransomware", "piraté", "malware", "suspect"));
        KEYWORDS.put("NETWORK",  List.of("wifi", "wi-fi", "réseau", "internet", "connexion", "vpn", "switch", "routeur", "ping", "lent"));
        KEYWORDS.put("EMAIL",    List.of("mail", "messagerie", "outlook", "courriel", "boîte"));
        KEYWORDS.put("ACCOUNT",  List.of("mot de passe", "compte", "accès", "identifiant", "connexion refusée", "droits", "verrouillé"));
        KEYWORDS.put("SERVER",   List.of("serveur", "sauvegarde", "backup", "restauration", "nas", "partage"));
        KEYWORDS.put("HARDWARE", List.of("imprimante", "écran", "clavier", "souris", "disque", "batterie", "ne s'allume", "bruit", "onduleur", "pc", "ordinateur"));
        KEYWORDS.put("SOFTWARE", List.of("logiciel", "application", "excel", "word", "mise à jour", "licence", "plante", "erreur", "installer"));
    }

    private static final EnumSet<TicketStatus> ACTIVE = EnumSet.of(TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS, TicketStatus.ON_HOLD);

    private final UserRepository users;
    private final TicketRepository tickets;

    public HeuristicTriageAgent(UserRepository users, TicketRepository tickets) {
        this.users = users; this.tickets = tickets;
    }

    @Override
    public String providerName() { return "heuristic"; }

    @Override
    public TriageResult triage(Ticket ticket) {
        long start = System.currentTimeMillis();
        String text = (ticket.getTitle() + " " + ticket.getDescription()).toLowerCase(Locale.ROOT);

        String category = "OTHER";
        int best = 0;
        for (var e : KEYWORDS.entrySet()) {
            int hits = (int) e.getValue().stream().filter(text::contains).count();
            if (hits > best) { best = hits; category = e.getKey(); }
        }

        boolean many = text.contains("tous") || text.contains("toute l'équipe") || text.contains("plusieurs") || text.contains("site") || text.contains("agence");
        boolean blocked = text.contains("bloqué") || text.contains("impossible") || text.contains("ne fonctionne plus") || text.contains("arrêt") || text.contains("urgent");
        int impact = many ? 1 : (blocked ? 2 : 3);
        int urgency = "SECURITY".equals(category) || text.contains("urgent") || text.contains("production") ? 1 : (blocked ? 2 : 3);

        Long techId = null;
        long minLoad = Long.MAX_VALUE;
        for (User u : users.findByRoleAndActiveTrueOrderByFullName(Role.TECHNICIAN)) {
            long load = tickets.countByAssigneeIdAndStatusIn(u.getId(), ACTIVE);
            boolean skilled = u.getSkills() != null && u.getSkills().toLowerCase(Locale.ROOT).contains(category.toLowerCase(Locale.ROOT));
            long score = skilled ? load : load + 100; // priorité à la compétence, puis à la charge la plus faible
            if (score < minLoad) { minLoad = score; techId = u.getId(); }
        }

        String summary = ticket.getTitle().length() > 120 ? ticket.getTitle().substring(0, 120) : ticket.getTitle();
        String rationale = "Simulation par mots clés (%d correspondance(s) pour %s) ; impact %d / urgence %d déduits des termes du ticket."
            .formatted(best, category, impact, urgency);
        TriageOutput out = new TriageOutput(category, impact, urgency, techId, summary, rationale, List.of(), best == 0 ? 0.2 : Math.min(0.9, 0.4 + 0.15 * best));
        return new TriageResult(out, providerName(), 0, 0, 0, System.currentTimeMillis() - start, "heuristic", null);
    }
}
