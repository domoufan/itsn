-- =====================================================================
-- IntervIT - Schéma initial (PostgreSQL 16)
-- Plateforme de gestion des interventions techniques (ITSM)
-- =====================================================================

CREATE TABLE sites (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(120) NOT NULL,
    client_name     VARCHAR(120) NOT NULL,
    address         VARCHAR(255),
    city            VARCHAR(80),
    contact_phone   VARCHAR(40),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    full_name       VARCHAR(120) NOT NULL,
    email           VARCHAR(160) NOT NULL UNIQUE,
    password_hash   VARCHAR(100) NOT NULL,
    role            VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN','MANAGER','TECHNICIAN','REQUESTER')),
    phone           VARCHAR(40),
    site_id         BIGINT REFERENCES sites(id),
    skills          VARCHAR(255),              -- compétences techniques (liste séparée par des virgules)
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_login_at   TIMESTAMPTZ
);
CREATE INDEX idx_users_role ON users(role);

CREATE TABLE categories (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(30) NOT NULL UNIQUE,
    name            VARCHAR(80) NOT NULL,
    description     VARCHAR(255),
    active          BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE sla_policies (
    id                      BIGSERIAL PRIMARY KEY,
    priority                VARCHAR(2) NOT NULL UNIQUE CHECK (priority IN ('P1','P2','P3','P4')),
    label                   VARCHAR(40) NOT NULL,
    response_minutes        INTEGER NOT NULL CHECK (response_minutes > 0),
    resolution_minutes      INTEGER NOT NULL CHECK (resolution_minutes > 0)
);

CREATE TABLE equipment (
    id              BIGSERIAL PRIMARY KEY,
    asset_tag       VARCHAR(40) NOT NULL UNIQUE,
    type            VARCHAR(30) NOT NULL,       -- LAPTOP, DESKTOP, PRINTER, SERVER, SWITCH, ROUTER, ACCESS_POINT, UPS, OTHER
    brand           VARCHAR(60),
    model           VARCHAR(80),
    serial_number   VARCHAR(80),
    site_id         BIGINT REFERENCES sites(id),
    status          VARCHAR(20) NOT NULL DEFAULT 'IN_SERVICE',   -- IN_SERVICE, IN_REPAIR, RETIRED
    purchase_date   DATE,
    warranty_end    DATE,
    notes           VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_equipment_site ON equipment(site_id);

CREATE TABLE ticket_sequences (
    year            INTEGER PRIMARY KEY,
    last_value      INTEGER NOT NULL
);

CREATE TABLE tickets (
    id                      BIGSERIAL PRIMARY KEY,
    reference               VARCHAR(20) NOT NULL UNIQUE,     -- ex. INC-2026-00042 / REQ-2026-00007
    type                    VARCHAR(10) NOT NULL CHECK (type IN ('INCIDENT','REQUEST')),
    title                   VARCHAR(160) NOT NULL,
    description             TEXT NOT NULL,
    category_id             BIGINT REFERENCES categories(id),
    impact                  SMALLINT NOT NULL CHECK (impact BETWEEN 1 AND 3),    -- 1 = élevé, 3 = faible
    urgency                 SMALLINT NOT NULL CHECK (urgency BETWEEN 1 AND 3),   -- 1 = élevée, 3 = faible
    priority                VARCHAR(2) NOT NULL CHECK (priority IN ('P1','P2','P3','P4')),
    status                  VARCHAR(15) NOT NULL CHECK (status IN ('NEW','ASSIGNED','IN_PROGRESS','ON_HOLD','RESOLVED','CLOSED','CANCELLED')),
    requester_id            BIGINT NOT NULL REFERENCES users(id),
    assignee_id             BIGINT REFERENCES users(id),
    site_id                 BIGINT REFERENCES sites(id),
    equipment_id            BIGINT REFERENCES equipment(id),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    response_due_at         TIMESTAMPTZ NOT NULL,
    resolution_due_at       TIMESTAMPTZ NOT NULL,
    first_response_at       TIMESTAMPTZ,
    resolved_at             TIMESTAMPTZ,
    closed_at               TIMESTAMPTZ,
    sla_response_breached   BOOLEAN NOT NULL DEFAULT FALSE,
    sla_resolution_breached BOOLEAN NOT NULL DEFAULT FALSE,
    resolution_summary      TEXT,
    satisfaction_score      SMALLINT CHECK (satisfaction_score BETWEEN 1 AND 5)
);
CREATE INDEX idx_tickets_status ON tickets(status);
CREATE INDEX idx_tickets_assignee ON tickets(assignee_id);
CREATE INDEX idx_tickets_requester ON tickets(requester_id);
CREATE INDEX idx_tickets_created ON tickets(created_at);
CREATE INDEX idx_tickets_priority ON tickets(priority);

CREATE TABLE ticket_comments (
    id              BIGSERIAL PRIMARY KEY,
    ticket_id       BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    author_id       BIGINT NOT NULL REFERENCES users(id),
    content         TEXT NOT NULL,
    internal        BOOLEAN NOT NULL DEFAULT FALSE,     -- note interne (non visible du demandeur)
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_comments_ticket ON ticket_comments(ticket_id);

CREATE TABLE ticket_history (
    id              BIGSERIAL PRIMARY KEY,
    ticket_id       BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    changed_by_id   BIGINT REFERENCES users(id),
    field           VARCHAR(40) NOT NULL,
    old_value       VARCHAR(255),
    new_value       VARCHAR(255),
    changed_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_history_ticket ON ticket_history(ticket_id);

CREATE TABLE interventions (
    id                  BIGSERIAL PRIMARY KEY,
    ticket_id           BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    technician_id       BIGINT NOT NULL REFERENCES users(id),
    mode                VARCHAR(10) NOT NULL CHECK (mode IN ('ONSITE','REMOTE')),
    status              VARCHAR(12) NOT NULL CHECK (status IN ('PLANNED','IN_PROGRESS','DONE','CANCELLED')),
    scheduled_start     TIMESTAMPTZ NOT NULL,
    scheduled_end       TIMESTAMPTZ NOT NULL,
    actual_start        TIMESTAMPTZ,
    actual_end          TIMESTAMPTZ,
    report              TEXT,
    parts_used          VARCHAR(500),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_intervention_window CHECK (scheduled_end > scheduled_start)
);
CREATE INDEX idx_interventions_tech_start ON interventions(technician_id, scheduled_start);

CREATE TABLE knowledge_articles (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(160) NOT NULL,
    content         TEXT NOT NULL,
    category_id     BIGINT REFERENCES categories(id),
    tags            VARCHAR(255),
    author_id       BIGINT REFERENCES users(id),
    published       BOOLEAN NOT NULL DEFAULT FALSE,
    views           INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_kb_published ON knowledge_articles(published);

-- Propositions de l'agent IA de triage (toujours soumises à validation humaine)
CREATE TABLE ai_suggestions (
    id                      BIGSERIAL PRIMARY KEY,
    ticket_id               BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    status                  VARCHAR(12) NOT NULL CHECK (status IN ('PENDING','ACCEPTED','PARTIAL','REJECTED','FAILED')),
    suggested_category_id   BIGINT REFERENCES categories(id),
    suggested_impact        SMALLINT CHECK (suggested_impact BETWEEN 1 AND 3),
    suggested_urgency       SMALLINT CHECK (suggested_urgency BETWEEN 1 AND 3),
    suggested_priority      VARCHAR(2) CHECK (suggested_priority IN ('P1','P2','P3','P4')),
    suggested_assignee_id   BIGINT REFERENCES users(id),
    summary                 TEXT,
    rationale               TEXT,
    related_article_ids     VARCHAR(255),
    model                   VARCHAR(60),
    tool_calls              INTEGER NOT NULL DEFAULT 0,
    input_tokens            INTEGER NOT NULL DEFAULT 0,
    output_tokens           INTEGER NOT NULL DEFAULT 0,
    latency_ms              INTEGER NOT NULL DEFAULT 0,
    estimated_cost_usd      NUMERIC(10,6) NOT NULL DEFAULT 0,
    error_message           VARCHAR(500),
    raw_response            TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    reviewed_by_id          BIGINT REFERENCES users(id),
    reviewed_at             TIMESTAMPTZ,
    review_comment          VARCHAR(500)
);
CREATE INDEX idx_ai_suggestions_ticket ON ai_suggestions(ticket_id);
CREATE INDEX idx_ai_suggestions_status ON ai_suggestions(status);

-- Journal d'audit des actions sensibles
CREATE TABLE audit_log (
    id              BIGSERIAL PRIMARY KEY,
    actor_id        BIGINT REFERENCES users(id),
    action          VARCHAR(60) NOT NULL,
    target_type     VARCHAR(40),
    target_id       BIGINT,
    details         VARCHAR(1000),
    ip_address      VARCHAR(45),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_created ON audit_log(created_at);

-- Données de référence : matrice SLA (valeurs par défaut, modifiables par l'administrateur)
INSERT INTO sla_policies (priority, label, response_minutes, resolution_minutes) VALUES
    ('P1', 'Critique',  30,   240),
    ('P2', 'Élevée',    60,   480),
    ('P3', 'Moyenne',   240,  1440),
    ('P4', 'Faible',    480,  4320);

INSERT INTO categories (code, name, description) VALUES
    ('HARDWARE',  'Matériel',            'Pannes et remplacements de postes, imprimantes, périphériques'),
    ('NETWORK',   'Réseau',              'Connectivité, Wi-Fi, switches, routeurs, VPN'),
    ('SOFTWARE',  'Logiciel',            'Applications bureautiques et métiers, mises à jour, licences'),
    ('ACCOUNT',   'Comptes et accès',    'Création, réinitialisation, droits d''accès'),
    ('EMAIL',     'Messagerie',          'Boîtes mail, envoi/réception, configuration'),
    ('SECURITY',  'Sécurité',            'Virus, phishing, incidents de sécurité'),
    ('SERVER',    'Serveurs et sauvegardes', 'Serveurs, stockage, sauvegardes, restauration'),
    ('OTHER',     'Autre',               'Demandes ne relevant d''aucune autre catégorie');
