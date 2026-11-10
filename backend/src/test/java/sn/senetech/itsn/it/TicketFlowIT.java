package sn.senetech.itsn.it;

import org.junit.jupiter.api.*;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TI-01 à TI-10 : cycle de vie complet d'un ticket via l'API, contrôle d'accès par rôle,
 * validation des entrées (exigences EF-01, EF-02, EF-03, EF-04, EF-05, EF-07, ENF-01, ENF-02).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TicketFlowIT extends ApiTestSupport {

    private static long ticketId;
    private static String reference;

    @Test @Order(1)
    @DisplayName("TI-01 Connexion : identifiants valides acceptés, invalides refusés (401)")
    void login() {
        assertThat(login("user@test.example")).isNotBlank();
        HttpResponse<String> bad = post("/api/auth/login", Map.of("email", "user@test.example", "password", "mauvais-mdp-123"), null);
        assertThat(bad.statusCode()).isEqualTo(401);
    }

    @Test @Order(2)
    @DisplayName("TI-10 Accès sans jeton refusé (401)")
    void unauthenticated() {
        assertThat(get("/api/tickets", null).statusCode()).isEqualTo(401);
    }

    @Test @Order(3)
    @DisplayName("TI-09 Création avec données invalides rejetée (400) avec détail des champs")
    void invalidPayload() {
        String token = login("user@test.example");
        HttpResponse<String> r = post("/api/tickets", Map.of("type", "INCIDENT", "title", "abc", "description", "court", "impact", 5, "urgency", 2), token);
        assertThat(r.statusCode()).isEqualTo(400);
        assertThat(r.body()).contains("fieldErrors").contains("title").contains("impact");
    }

    @Test @Order(4)
    @DisplayName("TI-02 Un demandeur crée un ticket : référence, priorité P3 (impact 2 / urgence 2) et échéances SLA calculées")
    void createTicket() {
        String token = login("user@test.example");
        Map<String, Object> body = new HashMap<>();
        body.put("type", "INCIDENT"); body.put("title", "Wi-Fi coupé dans l'agence");
        body.put("description", "Plus de Wi-Fi depuis ce matin dans toute l'agence, le filaire fonctionne.");
        body.put("impact", 2); body.put("urgency", 2);
        HttpResponse<String> r = post("/api/tickets", body, token);
        assertThat(r.statusCode()).isEqualTo(201);
        ticketId = id(r);
        reference = field(r, "reference");
        assertThat(reference).matches("INC-\\d{4}-\\d{5}");
        assertThat(field(r, "priority")).isEqualTo("P3");
        assertThat(field(r, "status")).isEqualTo("NEW");
        assertThat(field(r, "responseDueAt")).isNotBlank();
        assertThat(field(r, "resolutionDueAt")).isNotBlank();
        assertThat(json.readTree(r.body()).path("category").path("code").asString()).isEqualTo("OTHER");
    }

    @Test @Order(5)
    @DisplayName("TI-08 Un autre demandeur ne peut pas consulter ce ticket (403)")
    void otherRequesterForbidden() {
        String other = login("other@test.example");
        assertThat(get("/api/tickets/" + ticketId, other).statusCode()).isEqualTo(403);
        // et sa liste ne le contient pas
        HttpResponse<String> list = get("/api/tickets", other);
        assertThat(list.statusCode()).isEqualTo(200);
        assertThat(json.readTree(list.body()).path("totalElements").asLong()).isZero();
    }

    @Test @Order(6)
    @DisplayName("TI-03 Un demandeur ne peut pas affecter un ticket (403)")
    void requesterCannotAssign() {
        String token = login("user@test.example");
        HttpResponse<String> r = post("/api/tickets/" + ticketId + "/assign", Map.of("assigneeId", tech.getId()), token);
        assertThat(r.statusCode()).isEqualTo(403);
    }

    @Test @Order(7)
    @DisplayName("TI-04 Le responsable affecte le ticket à un technicien : état ASSIGNED, historique tracé")
    void managerAssigns() {
        String token = login("manager@test.example");
        HttpResponse<String> r = post("/api/tickets/" + ticketId + "/assign", Map.of("assigneeId", tech.getId(), "comment", "Merci d'intervenir ce matin"), token);
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(field(r, "status")).isEqualTo("ASSIGNED");
        assertThat(json.readTree(r.body()).path("assignee").path("id").asLong()).isEqualTo(tech.getId());
        HttpResponse<String> h = get("/api/tickets/" + ticketId + "/history", token);
        assertThat(h.body()).contains("\"field\":\"assignee\"").contains("\"newValue\":\"ASSIGNED\"");
    }

    @Test @Order(8)
    @DisplayName("TI-05 Le technicien affecté prend en charge : IN_PROGRESS et date de première réponse")
    void technicianStarts() {
        String token = login("tech@test.example");
        HttpResponse<String> r = post("/api/tickets/" + ticketId + "/status", Map.of("status", "IN_PROGRESS", "comment", "Diagnostic en cours sur site"), token);
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(field(r, "status")).isEqualTo("IN_PROGRESS");
        assertThat(field(r, "firstResponseAt")).isNotBlank();
        assertThat(field(r, "slaResponseBreached")).isEqualTo("false");
    }

    @Test @Order(9)
    @DisplayName("TI-06 Transition interdite et résolution sans résumé rejetées (409)")
    void invalidTransitions() {
        String token = login("tech@test.example");
        assertThat(post("/api/tickets/" + ticketId + "/status", Map.of("status", "CLOSED"), token).statusCode()).isEqualTo(403);
        HttpResponse<String> noSummary = post("/api/tickets/" + ticketId + "/status", Map.of("status", "RESOLVED"), token);
        assertThat(noSummary.statusCode()).isEqualTo(409);
        String other = login("tech2@test.example");
        assertThat(post("/api/tickets/" + ticketId + "/status", Map.of("status", "ON_HOLD"), other).statusCode()).isEqualTo(403);
    }

    @Test @Order(10)
    @DisplayName("TI-06b Résolution avec résumé : RESOLVED, date de résolution, indicateur SLA")
    void resolve() {
        String token = login("tech@test.example");
        HttpResponse<String> r = post("/api/tickets/" + ticketId + "/status",
            Map.of("status", "RESOLVED", "resolutionSummary", "Borne Wi-Fi redémarrée et canal changé ; débit vérifié."), token);
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(field(r, "status")).isEqualTo("RESOLVED");
        assertThat(field(r, "resolvedAt")).isNotBlank();
        assertThat(field(r, "slaResolutionBreached")).isEqualTo("false");
    }

    @Test @Order(11)
    @DisplayName("TI-07 Le demandeur évalue puis clôture ; le ticket devient non modifiable")
    void requesterCloses() {
        String token = login("user@test.example");
        HttpResponse<String> rate = post("/api/tickets/" + ticketId + "/satisfaction", Map.of("score", 5), token);
        assertThat(rate.statusCode()).isEqualTo(200);
        HttpResponse<String> r = post("/api/tickets/" + ticketId + "/status", Map.of("status", "CLOSED"), token);
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(field(r, "status")).isEqualTo("CLOSED");
        HttpResponse<String> c = post("/api/tickets/" + ticketId + "/comments", Map.of("content", "Encore un commentaire"), token);
        assertThat(c.statusCode()).isEqualTo(409);
        // le demandeur ne voit pas les notes internes
        HttpResponse<String> comments = get("/api/tickets/" + ticketId + "/comments", token);
        assertThat(comments.body()).doesNotContain("Merci d'intervenir ce matin");
    }

    @Test @Order(12)
    @DisplayName("TI-11 Tableau de bord : indicateurs calculés et réservés au personnel")
    void dashboard() {
        String token = login("manager@test.example");
        HttpResponse<String> r = get("/api/dashboard/kpis", token);
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(json.readTree(r.body()).path("resolvedLast30Days").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(json.readTree(r.body()).path("slaResolutionComplianceRate").asDouble()).isEqualTo(100.0);
        assertThat(get("/api/dashboard/kpis", login("user@test.example")).statusCode()).isEqualTo(403);
    }

    @Test @Order(13)
    @DisplayName("TI-12 Santé de l'application exposée sans authentification, métriques protégées")
    void actuator() {
        assertThat(get("/actuator/health", null).statusCode()).isEqualTo(200);
        assertThat(get("/actuator/metrics", null).statusCode()).isEqualTo(401);
        assertThat(get("/actuator/metrics", login("admin@test.example")).statusCode()).isEqualTo(200);
    }
}
