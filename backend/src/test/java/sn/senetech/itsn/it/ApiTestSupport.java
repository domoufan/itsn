package sn.senetech.itsn.it;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import sn.senetech.itsn.domain.*;
import sn.senetech.itsn.repository.SiteRepository;
import sn.senetech.itsn.repository.UserRepository;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Socle des tests d'intégration : application démarrée sur un port aléatoire, base PostgreSQL réelle
 * (schéma recréé par Flyway), comptes de test créés en base, appels HTTP via le client JDK.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class ApiTestSupport {

    protected static final String PASSWORD = "Test@2026-secure";

    @Autowired protected Environment env;
    @Autowired protected ObjectMapper json;
    @Autowired protected UserRepository users;
    @Autowired protected SiteRepository sites;
    @Autowired protected PasswordEncoder encoder;
    @Autowired protected Flyway flyway;

    protected final HttpClient http = HttpClient.newHttpClient();
    protected User admin, manager, tech, tech2, requester, otherRequester;
    protected Site site;

    @BeforeAll
    void resetDatabaseAndUsers() {
        flyway.clean();
        flyway.migrate();
        site = new Site(); site.setName("Agence Test"); site.setClientName("Client Test"); site.setCity("Dakar");
        site = sites.save(site);
        String hash = encoder.encode(PASSWORD);
        admin = user("Admin Test", "admin@test.example", Role.ADMIN, hash, null);
        manager = user("Manager Test", "manager@test.example", Role.MANAGER, hash, null);
        tech = user("Tech Réseau", "tech@test.example", Role.TECHNICIAN, hash, "NETWORK,SECURITY");
        tech2 = user("Tech Matériel", "tech2@test.example", Role.TECHNICIAN, hash, "HARDWARE");
        requester = user("Demandeur Test", "user@test.example", Role.REQUESTER, hash, null);
        otherRequester = user("Autre Demandeur", "other@test.example", Role.REQUESTER, hash, null);
    }

    private User user(String name, String email, Role role, String hash, String skills) {
        User u = new User(); u.setFullName(name); u.setEmail(email); u.setRole(role); u.setPasswordHash(hash); u.setSite(site); u.setSkills(skills);
        return users.save(u);
    }

    protected String baseUrl() {
        return "http://localhost:" + env.getProperty("local.server.port", Integer.class);
    }

    protected String login(String email) {
        HttpResponse<String> r = post("/api/auth/login", Map.of("email", email, "password", PASSWORD), null);
        if (r.statusCode() != 200) throw new IllegalStateException("Connexion impossible : " + r.statusCode() + " " + r.body());
        return json.readTree(r.body()).path("accessToken").asString();
    }

    protected HttpResponse<String> get(String path, String token) {
        return send(HttpRequest.newBuilder(URI.create(baseUrl() + path)).GET(), token);
    }

    protected HttpResponse<String> post(String path, Object body, String token) {
        return send(HttpRequest.newBuilder(URI.create(baseUrl() + path))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))), token);
    }

    protected HttpResponse<String> patch(String path, Object body, String token) {
        return send(HttpRequest.newBuilder(URI.create(baseUrl() + path))
            .header("Content-Type", "application/json")
            .method("PATCH", HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))), token);
    }

    private HttpResponse<String> send(HttpRequest.Builder b, String token) {
        if (token != null) b.header("Authorization", "Bearer " + token);
        try {
            return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    protected String field(HttpResponse<String> r, String path) {
        return json.readTree(r.body()).path(path).asString();
    }

    protected long id(HttpResponse<String> r) {
        return json.readTree(r.body()).path("id").asLong();
    }
}
