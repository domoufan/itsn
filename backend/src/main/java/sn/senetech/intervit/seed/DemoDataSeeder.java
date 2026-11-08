package sn.senetech.intervit.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sn.senetech.intervit.config.AppProperties;
import sn.senetech.intervit.domain.*;
import sn.senetech.intervit.repository.*;
import sn.senetech.intervit.ticket.PriorityMatrix;
import sn.senetech.intervit.ticket.SlaService;
import sn.senetech.intervit.ticket.TicketReferenceService;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * DONNÉES FICTIVES de démonstration (aucune donnée réelle). Chargées une seule fois si la base est vide
 * et si {@code intervit.demo.seed-enabled=true}. Comptes de démonstration : voir README.
 */
@Component
@Profile("!test")
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    public static final String DEMO_PASSWORD = "Demo@2026!";

    private final AppProperties properties;
    private final PasswordEncoder encoder;
    private final UserRepository users;
    private final SiteRepository sites;
    private final CategoryRepository categories;
    private final EquipmentRepository equipment;
    private final TicketRepository tickets;
    private final TicketCommentRepository comments;
    private final TicketHistoryRepository history;
    private final InterventionRepository interventions;
    private final KnowledgeArticleRepository articles;
    private final SlaService slaService;
    private final TicketReferenceService references;
    private final SeedTransaction tx;

    public DemoDataSeeder(AppProperties properties, PasswordEncoder encoder, UserRepository users, SiteRepository sites,
                          CategoryRepository categories, EquipmentRepository equipment, TicketRepository tickets,
                          TicketCommentRepository comments, TicketHistoryRepository history, InterventionRepository interventions,
                          KnowledgeArticleRepository articles, SlaService slaService, TicketReferenceService references, SeedTransaction tx) {
        this.properties = properties; this.encoder = encoder; this.users = users; this.sites = sites; this.categories = categories;
        this.equipment = equipment; this.tickets = tickets; this.comments = comments; this.history = history;
        this.interventions = interventions; this.articles = articles; this.slaService = slaService; this.references = references; this.tx = tx;
    }

    @Override
    public void run(String... args) {
        if (!properties.demo().seedEnabled()) return;
        if (users.count() > 0) { log.info("Données déjà présentes : aucun chargement de démonstration"); return; }
        tx.run(this::seed);
        log.info("Jeu de données fictives chargé : {} utilisateurs, {} tickets", users.count(), tickets.count());
    }

    private void seed() {
        Random rnd = new Random(42); // reproductible
        String hash = encoder.encode(DEMO_PASSWORD);

        // --- Sites clients (fictifs)
        Site hq = site("Siège SénéTech", "SénéTech Services", "Sacré-Cœur 3, Villa 9800", "Dakar", "+221 33 800 00 00");
        Site s1 = site("Agence Plateau", "Banque Atlantique du Sahel (fictif)", "Avenue Léopold Sédar Senghor", "Dakar", "+221 33 821 00 01");
        Site s2 = site("Usine Rufisque", "Agro-Sénégal Industries (fictif)", "Zone industrielle", "Rufisque", "+221 33 836 00 02");
        Site s3 = site("Clinique Mermoz", "Clinique Les Mamelles (fictif)", "Route de la Corniche Ouest", "Dakar", "+221 33 860 00 03");
        Site s4 = site("Agence Thiès", "Assur'Teranga (fictif)", "Avenue Lamine Guèye", "Thiès", "+221 33 951 00 04");

        // --- Personnel
        User admin = user("Awa Ndiaye", "admin@senetech.example", Role.ADMIN, hq, null, hash);
        User manager = user("Moussa Diop", "manager@senetech.example", Role.MANAGER, hq, null, hash);
        User t1 = user("Ibrahima Sarr", "tech.reseau@senetech.example", Role.TECHNICIAN, hq, "NETWORK,SERVER,SECURITY", hash);
        User t2 = user("Fatou Bâ", "tech.materiel@senetech.example", Role.TECHNICIAN, hq, "HARDWARE,PRINTER,UPS", hash);
        User t3 = user("Cheikh Fall", "tech.logiciel@senetech.example", Role.TECHNICIAN, hq, "SOFTWARE,EMAIL,ACCOUNT", hash);
        User t4 = user("Mariama Sow", "tech.systeme@senetech.example", Role.TECHNICIAN, hq, "SERVER,SECURITY,ACCOUNT", hash);
        User t5 = user("Ousmane Gueye", "tech.terrain@senetech.example", Role.TECHNICIAN, hq, "HARDWARE,NETWORK", hash);
        List<User> techs = List.of(t1, t2, t3, t4, t5);

        // --- Demandeurs côté clients
        User r1 = user("Aminata Faye", "a.faye@client-bas.example", Role.REQUESTER, s1, null, hash);
        User r2 = user("Pape Mbaye", "p.mbaye@client-bas.example", Role.REQUESTER, s1, null, hash);
        User r3 = user("Khady Diallo", "k.diallo@agro-senegal.example", Role.REQUESTER, s2, null, hash);
        User r4 = user("Lamine Cissé", "l.cisse@agro-senegal.example", Role.REQUESTER, s2, null, hash);
        User r5 = user("Dr Ndèye Thiam", "n.thiam@clinique-mamelles.example", Role.REQUESTER, s3, null, hash);
        User r6 = user("Abdou Kane", "a.kane@assur-teranga.example", Role.REQUESTER, s4, null, hash);
        User demoReq = user("Demandeur Démo", "user@client.example", Role.REQUESTER, s1, null, hash);
        List<User> requesters = List.of(r1, r2, r3, r4, r5, r6, demoReq);

        // --- Équipements
        List<Equipment> eq = new ArrayList<>();
        String[][] eqData = {
            {"BAS-PC-001", "DESKTOP", "Dell", "OptiPlex 7010"}, {"BAS-PC-002", "LAPTOP", "HP", "ProBook 450 G9"},
            {"BAS-PRN-001", "PRINTER", "HP", "LaserJet Pro M428"}, {"BAS-SW-001", "SWITCH", "Cisco", "Catalyst 1000-24T"},
            {"AGS-SRV-001", "SERVER", "Dell", "PowerEdge T350"}, {"AGS-UPS-001", "UPS", "APC", "Smart-UPS 1500"},
            {"AGS-PC-010", "DESKTOP", "Lenovo", "ThinkCentre M70q"}, {"AGS-AP-003", "ACCESS_POINT", "Ubiquiti", "UniFi U6 Lite"},
            {"CLM-PC-004", "LAPTOP", "Lenovo", "ThinkPad E14"}, {"CLM-PRN-002", "PRINTER", "Canon", "i-SENSYS MF445dw"},
            {"CLM-RT-001", "ROUTER", "MikroTik", "hEX S"}, {"AST-PC-007", "DESKTOP", "HP", "ProDesk 400 G9"},
            {"AST-SRV-001", "SERVER", "HPE", "ProLiant ML30"}, {"AST-PRN-001", "PRINTER", "Epson", "EcoTank L15150"},
        };
        Site[] eqSites = {s1, s1, s1, s1, s2, s2, s2, s2, s3, s3, s3, s4, s4, s4};
        for (int i = 0; i < eqData.length; i++) {
            Equipment e = new Equipment();
            e.setAssetTag(eqData[i][0]); e.setType(EquipmentType.valueOf(eqData[i][1])); e.setBrand(eqData[i][2]); e.setModel(eqData[i][3]);
            e.setSerialNumber("SN" + (100000 + rnd.nextInt(900000))); e.setSite(eqSites[i]);
            e.setPurchaseDate(LocalDate.of(2022 + rnd.nextInt(3), 1 + rnd.nextInt(12), 1 + rnd.nextInt(28)));
            e.setWarrantyEnd(e.getPurchaseDate().plusYears(3));
            eq.add(equipment.save(e));
        }

        // --- Base de connaissances
        Map<String, Category> cat = new HashMap<>();
        categories.findAll().forEach(c -> cat.put(c.getCode(), c));
        article("Réinitialiser un mot de passe Active Directory", cat.get("ACCOUNT"), t4, "mot de passe,AD,compte",
            "1. Vérifier l'identité du demandeur (rappel sur le numéro du site).\n2. Ouvrir « Utilisateurs et ordinateurs AD ».\n3. Clic droit > Réinitialiser le mot de passe, cocher « doit changer à la prochaine ouverture ».\n4. Déverrouiller le compte si nécessaire.\n5. Informer l'utilisateur et consigner dans le ticket.");
        article("Imprimante réseau hors ligne : procédure de diagnostic", cat.get("HARDWARE"), t2, "imprimante,réseau,spooler",
            "1. Vérifier l'alimentation et l'écran de l'imprimante.\n2. Imprimer la page de configuration réseau et contrôler l'adresse IP.\n3. Depuis un poste : ping de l'IP ; si échec, contrôler le câble et le port du switch.\n4. Redémarrer le service Spouleur d'impression sur le poste.\n5. Si l'IP a changé, corriger le port TCP/IP du pilote ou fixer une réservation DHCP.");
        article("Wi-Fi lent ou instable sur un site client", cat.get("NETWORK"), t1, "wifi,débit,canal",
            "1. Mesurer le débit au plus près de la borne puis en zone de plainte.\n2. Contrôler le nombre de clients par borne et les interférences (canaux 1/6/11 en 2,4 GHz).\n3. Vérifier le firmware des bornes et le lien montant.\n4. Basculer les postes compatibles en 5 GHz.\n5. Proposer une borne supplémentaire si la couverture est insuffisante.");
        article("Suspicion de phishing : conduite à tenir", cat.get("SECURITY"), t4, "phishing,sécurité,incident",
            "1. Ne pas cliquer, ne pas répondre, ne pas transférer le message à des collègues.\n2. Isoler le poste du réseau si un lien a été ouvert ou une pièce jointe exécutée.\n3. Changer immédiatement les mots de passe depuis un autre poste.\n4. Analyser le poste avec l'antivirus à jour.\n5. Signaler l'incident au responsable support et conserver l'e-mail en preuve.");
        article("Outlook : réparer un profil qui ne s'ouvre plus", cat.get("EMAIL"), t3, "outlook,profil,messagerie",
            "1. Lancer Outlook en mode sans échec (outlook.exe /safe) pour écarter un complément défectueux.\n2. Panneau de configuration > Courrier > Afficher les profils > Ajouter un nouveau profil.\n3. Reconfigurer le compte ; conserver l'ancien fichier OST en sauvegarde.\n4. Vérifier la taille de la boîte et archiver si > 50 Go.");
        article("Sauvegarde serveur : vérification quotidienne", cat.get("SERVER"), t1, "sauvegarde,backup,restauration",
            "1. Contrôler le rapport de sauvegarde de la nuit (statut, volume, durée).\n2. En cas d'échec : espace disque de la destination, service de sauvegarde, journaux d'événements.\n3. Relancer manuellement la tâche et vérifier.\n4. Un test de restauration d'un fichier est réalisé chaque mois et consigné.");
        article("Poste qui ne démarre plus : check-list terrain", cat.get("HARDWARE"), t5, "démarrage,alimentation,disque",
            "1. Vérifier prise, multiprise et câble d'alimentation ; tester sur une autre prise.\n2. Observer les voyants et bips au démarrage.\n3. Débrancher les périphériques USB puis réessayer.\n4. Tester avec un autre bloc d'alimentation / batterie.\n5. Si le disque n'est pas détecté : sauvegarder les données via un boîtier externe avant remplacement.");
        article("Créer un accès VPN pour un télétravailleur", cat.get("NETWORK"), t1, "vpn,accès distant,télétravail",
            "1. Demande validée par le responsable du client (e-mail conservé dans le ticket).\n2. Créer le compte VPN avec authentification à deux facteurs.\n3. Transmettre le guide d'installation et la configuration par canal sécurisé.\n4. Tester la connexion avec l'utilisateur et consigner.");

        // --- Tickets : scénarios réalistes répartis sur 60 jours
        Instant now = Instant.now();
        Object[][] data = {
            // titre, description, catégorie, impact, urgence, demandeur, site, équipement idx (ou -1), tech, état, âge jours
            {"Imprimante du guichet hors ligne", "L'imprimante HP du guichet 2 affiche « hors ligne » depuis ce matin. Les clients attendent leurs reçus. Redémarrage déjà tenté.", "HARDWARE", 2, 2, r1, s1, 2, t2, TicketStatus.RESOLVED, 40},
            {"Wi-Fi très lent dans l'open space", "Tous les collaborateurs de l'open space se plaignent de la lenteur du Wi-Fi depuis l'installation des nouveaux postes. Le filaire fonctionne normalement.", "NETWORK", 1, 2, r2, s1, 3, t1, TicketStatus.CLOSED, 38},
            {"Mot de passe oublié après congés", "Je reviens de congés et mon mot de passe Windows est refusé. Le compte semble verrouillé.", "ACCOUNT", 2, 2, r3, s2, -1, t3, TicketStatus.CLOSED, 35},
            {"Serveur de fichiers inaccessible", "Plus personne à l'usine ne peut ouvrir le lecteur partagé P:. La production est bloquée pour l'impression des bons de livraison. URGENT.", "SERVER", 1, 1, r4, s2, 4, t4, TicketStatus.CLOSED, 33},
            {"E-mail suspect avec pièce jointe", "J'ai reçu un e-mail d'un soi-disant fournisseur avec une facture .zip. J'ai ouvert le fichier mais rien ne s'est passé. Est-ce dangereux ?", "SECURITY", 2, 1, r5, s3, 8, t4, TicketStatus.CLOSED, 30},
            {"Outlook ne s'ouvre plus", "Depuis la mise à jour d'hier, Outlook se ferme immédiatement au lancement avec une erreur de profil.", "EMAIL", 2, 2, r6, s4, 11, t3, TicketStatus.RESOLVED, 28},
            {"Demande d'installation d'un logiciel de comptabilité", "Merci d'installer Sage Compta sur le poste de la nouvelle comptable et de transférer la licence de l'ancien poste.", "SOFTWARE", 3, 3, r1, s1, 0, t3, TicketStatus.CLOSED, 26},
            {"Onduleur en alarme", "L'onduleur de la salle serveur bipe en continu depuis la coupure de courant d'hier soir.", "HARDWARE", 1, 1, r3, s2, 5, t2, TicketStatus.CLOSED, 25},
            {"Création de compte pour un nouvel agent", "Un nouvel agent commence lundi : créer compte Windows, messagerie et accès au logiciel d'assurance.", "ACCOUNT", 3, 2, r6, s4, -1, t3, TicketStatus.CLOSED, 22},
            {"Écran noir au démarrage du PC d'accueil", "Le poste de l'accueil ne démarre plus : les voyants s'allument mais l'écran reste noir.", "HARDWARE", 2, 2, r5, s3, 8, t5, TicketStatus.CLOSED, 20},
            {"Routeur redémarre tout seul", "Le routeur de la clinique redémarre plusieurs fois par jour, ce qui coupe le dossier patient informatisé.", "NETWORK", 1, 1, r5, s3, 10, t1, TicketStatus.RESOLVED, 18},
            {"Accès VPN pour télétravail", "Je souhaite pouvoir accéder au réseau de l'agence depuis mon domicile deux jours par semaine.", "NETWORK", 3, 3, r2, s1, -1, t1, TicketStatus.CLOSED, 16},
            {"Imprimante EcoTank : traits sur les impressions", "Les impressions couleur présentent des traînées et des traits horizontaux depuis une semaine.", "HARDWARE", 3, 3, r6, s4, 13, t2, TicketStatus.RESOLVED, 14},
            {"Sauvegarde en échec depuis 3 nuits", "Le rapport de sauvegarde indique « destination inaccessible » depuis trois nuits consécutives.", "SERVER", 1, 2, r4, s2, 4, t1, TicketStatus.CLOSED, 12},
            {"Excel plante à l'ouverture d'un fichier", "Un fichier Excel de 40 Mo fait planter Excel à chaque ouverture, sur deux postes différents.", "SOFTWARE", 2, 3, r1, s1, 1, t3, TicketStatus.IN_PROGRESS, 9},
            {"Point d'accès atelier injoignable", "La borne Wi-Fi de l'atelier n'apparaît plus ; les tablettes de pointage ne se connectent pas.", "NETWORK", 1, 2, r3, s2, 7, t5, TicketStatus.IN_PROGRESS, 6},
            {"Demande de deuxième écran", "Merci de fournir et installer un second écran pour le poste de la gestionnaire sinistres.", "HARDWARE", 3, 3, r6, s4, 11, t2, TicketStatus.ON_HOLD, 5},
            {"Mot de passe messagerie expiré", "Le webmail me demande de changer le mot de passe mais la page tourne indéfiniment.", "EMAIL", 2, 2, r2, s1, -1, t3, TicketStatus.ASSIGNED, 3},
            {"Lenteur générale du serveur", "Les applications métiers hébergées sur le serveur sont lentes depuis deux jours, surtout l'après-midi.", "SERVER", 2, 2, r4, s2, 4, t4, TicketStatus.ASSIGNED, 2},
            {"Clavier qui ne répond plus", "Le clavier du poste de consultation ne répond plus par intermittence.", "HARDWARE", 3, 3, r5, s3, 8, null, TicketStatus.NEW, 1},
            {"Impossible d'imprimer depuis le portable", "Depuis mon portable, l'imprimante du service n'apparaît plus dans la liste alors qu'elle fonctionne pour les autres.", "HARDWARE", 3, 2, demoReq, s1, 2, null, TicketStatus.NEW, 0},
            {"Plusieurs postes infectés ? Pop-ups partout", "Sur trois postes du guichet des fenêtres publicitaires s'ouvrent sans arrêt et l'antivirus affiche des alertes.", "SECURITY", 1, 1, r1, s1, 0, null, TicketStatus.NEW, 0},
        };
        for (Object[] d : data) {
            Ticket t = new Ticket();
            String catCode = (String) d[2];
            int impact = (int) d[3], urgency = (int) d[4];
            User req = (User) d[5];
            Site site = (Site) d[6];
            int eqIdx = (int) d[7];
            User tech = (User) d[8];
            TicketStatus st = (TicketStatus) d[9];
            int ageDays = (int) d[10];

            Instant created = now.minus(Duration.ofDays(ageDays)).minus(Duration.ofHours(rnd.nextInt(8)));
            t.setType("SECURITY".equals(catCode) || impact <= 2 ? TicketType.INCIDENT : TicketType.REQUEST);
            if (((String) d[0]).startsWith("Demande") || ((String) d[0]).startsWith("Création") || ((String) d[0]).startsWith("Accès")) t.setType(TicketType.REQUEST);
            t.setReference(references.next(t.getType()));
            t.setTitle((String) d[0]); t.setDescription((String) d[1]);
            t.setCategory(cat.get(catCode)); t.setImpact((short) impact); t.setUrgency((short) urgency);
            t.setPriority(PriorityMatrix.compute(impact, urgency));
            t.setRequester(req); t.setSite(site); t.setEquipment(eqIdx >= 0 ? eq.get(eqIdx) : null);
            t.setCreatedAt(created);
            slaService.applyDeadlines(t);
            t.setStatus(st);
            t.setAssignee(tech);

            SlaPolicy pol = slaService.policyFor(t.getPriority());
            if (st != TicketStatus.NEW) {
                // première prise en compte : généralement dans le délai, parfois en retard (réalisme des indicateurs)
                long respMin = Math.round(pol.getResponseMinutes() * (rnd.nextInt(10) < 8 ? 0.3 + rnd.nextDouble() * 0.6 : 1.1 + rnd.nextDouble()));
                Instant first = created.plus(Duration.ofMinutes(respMin));
                t.setFirstResponseAt(first);
                t.setSlaResponseBreached(first.isAfter(t.getResponseDueAt()));
            }
            if (st == TicketStatus.RESOLVED || st == TicketStatus.CLOSED) {
                long resMin = Math.round(pol.getResolutionMinutes() * (rnd.nextInt(10) < 7 ? 0.3 + rnd.nextDouble() * 0.6 : 1.05 + rnd.nextDouble() * 0.6));
                Instant resolved = created.plus(Duration.ofMinutes(Math.max(resMin, 20)));
                t.setResolvedAt(resolved);
                t.setSlaResolutionBreached(resolved.isAfter(t.getResolutionDueAt()));
                t.setResolutionSummary(resolutionFor(catCode));
                if (st == TicketStatus.CLOSED) { t.setClosedAt(resolved.plus(Duration.ofHours(6 + rnd.nextInt(40)))); t.setSatisfactionScore((short) (3 + rnd.nextInt(3))); }
            }
            t = tickets.save(t);
            history.save(TicketHistory.of(t, req, "status", null, TicketStatus.NEW));
            if (tech != null) history.save(TicketHistory.of(t, manager, "assignee", null, tech.getFullName()));
            if (st.ordinal() >= TicketStatus.IN_PROGRESS.ordinal() && st != TicketStatus.CANCELLED) {
                comment(t, tech, "Prise en charge. Diagnostic en cours.", false);
            }
            if (t.getResolvedAt() != null) comment(t, tech, t.getResolutionSummary(), false);

            // interventions sur site pour les tickets matériels/réseau pris en charge
            if (tech != null && ("HARDWARE".equals(catCode) || "NETWORK".equals(catCode)) && st != TicketStatus.NEW) {
                Intervention i = new Intervention();
                i.setTicket(t); i.setTechnician(tech); i.setMode(InterventionMode.ONSITE);
                Instant startI = (st == TicketStatus.ASSIGNED || st == TicketStatus.ON_HOLD) ? now.plus(Duration.ofHours(20 + rnd.nextInt(48))) : created.plus(Duration.ofHours(3 + rnd.nextInt(10)));
                i.setScheduledStart(startI); i.setScheduledEnd(startI.plus(Duration.ofHours(2)));
                if (t.getResolvedAt() != null) { i.setStatus(InterventionStatus.DONE); i.setActualStart(startI); i.setActualEnd(startI.plus(Duration.ofMinutes(60 + rnd.nextInt(90)))); i.setReport(t.getResolutionSummary()); }
                else if (st == TicketStatus.IN_PROGRESS) i.setStatus(InterventionStatus.IN_PROGRESS);
                interventions.save(i);
            }
        }
    }

    private String resolutionFor(String cat) {
        return switch (cat) {
            case "HARDWARE" -> "Composant défectueux identifié et remplacé ; tests de bon fonctionnement réalisés avec l'utilisateur.";
            case "NETWORK" -> "Configuration réseau corrigée (canal/DHCP/firmware) ; débit et stabilité vérifiés sur site.";
            case "ACCOUNT" -> "Compte déverrouillé et mot de passe réinitialisé après vérification d'identité.";
            case "SERVER" -> "Service redémarré, espace disque libéré et supervision renforcée ; sauvegarde relancée avec succès.";
            case "SECURITY" -> "Poste isolé et analysé, aucune infection active ; mots de passe changés et sensibilisation faite.";
            case "EMAIL" -> "Profil Outlook recréé ; messagerie fonctionnelle, archives conservées.";
            case "SOFTWARE" -> "Logiciel installé/réparé et licence transférée ; validation avec l'utilisateur.";
            default -> "Demande traitée et validée avec le demandeur.";
        };
    }

    private Site site(String name, String client, String address, String city, String phone) {
        Site s = new Site(); s.setName(name); s.setClientName(client); s.setAddress(address); s.setCity(city); s.setContactPhone(phone);
        return sites.save(s);
    }

    private User user(String name, String email, Role role, Site site, String skills, String hash) {
        User u = new User(); u.setFullName(name); u.setEmail(email); u.setRole(role); u.setSite(site); u.setSkills(skills); u.setPasswordHash(hash);
        return users.save(u);
    }

    private void article(String title, Category c, User author, String tags, String content) {
        KnowledgeArticle a = new KnowledgeArticle(); a.setTitle(title); a.setCategory(c); a.setAuthor(author); a.setTags(tags); a.setContent(content); a.setPublished(true);
        articles.save(a);
    }

    private void comment(Ticket t, User author, String content, boolean internal) {
        if (author == null) return;
        TicketComment c = new TicketComment(); c.setTicket(t); c.setAuthor(author); c.setContent(content); c.setInternal(internal);
        comments.save(c);
    }

    /** Petite indirection pour exécuter le chargement dans une transaction (les appels internes ne sont pas proxifiés). */
    @Component
    public static class SeedTransaction {
        @Transactional
        public void run(Runnable r) { r.run(); }
    }
}
