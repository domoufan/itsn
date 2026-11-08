package sn.senetech.intervit.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Sortie structurée attendue de l'agent (schéma imposé dans le prompt système).
 * Tous les champs sont ensuite validés côté serveur contre les référentiels réels.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TriageOutput(
        String categoryCode,
        Integer impact,
        Integer urgency,
        Long suggestedTechnicianId,
        String summary,
        String rationale,
        List<Long> relatedArticleIds,
        Double confidence) {
}
