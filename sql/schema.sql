-- ===========================================================================
--  Relational schema that stores the ontology A-Box (instances).
--
--  Target: TDSQL (MySQL 5.7/8.0 wire protocol) in production.
--  The same script also runs on H2 in MySQL compatibility mode, which is what
--  the zero-install demo profile uses.
--
--  Usage:
--    mysql -h <tdsql-host> -P <port> -u <user> -p ontology_db < sql/schema.sql
--
--  NOTE: this script is destructive (DROP + CREATE).  Do not point it at a
--  database that already contains production data.
-- ===========================================================================

DROP TABLE IF EXISTS assignment;
DROP TABLE IF EXISTS project;
DROP TABLE IF EXISTS employee;
DROP TABLE IF EXISTS department;
DROP TABLE IF EXISTS inferred_triple;

-- Organisation units: departments and divisions live in the same table,
-- parent_id builds the hierarchy that the ontology exposes as :partOf.
CREATE TABLE department (
    id         INT         NOT NULL,
    name       VARCHAR(64) NOT NULL,
    parent_id  INT         NULL,
    head_id    INT         NULL,
    PRIMARY KEY (id)
);

CREATE TABLE employee (
    id            INT            NOT NULL,
    name          VARCHAR(64)    NOT NULL,
    salary        DECIMAL(12,2)  NOT NULL,
    start_date    DATE           NULL,
    department_id INT            NULL,
    manager_id    INT            NULL,
    PRIMARY KEY (id)
);

CREATE TABLE project (
    id            INT            NOT NULL,
    name          VARCHAR(64)    NOT NULL,
    budget        DECIMAL(14,2)  NOT NULL,
    department_id INT            NULL,
    PRIMARY KEY (id)
);

-- Many-to-many employee <-> project assignments.
CREATE TABLE assignment (
    employee_id INT NOT NULL,
    project_id  INT NOT NULL,
    PRIMARY KEY (employee_id, project_id)
);

-- Materialisation target for triples derived by the Jena reasoner.
-- The base tables above stay the authoritative A-Box; this table is a
-- queryable cache of the inferred closure (rebuildable at any time).
CREATE TABLE inferred_triple (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    subject     VARCHAR(512)  NOT NULL,
    predicate   VARCHAR(512)  NOT NULL,
    object      VARCHAR(2048) NOT NULL,
    object_kind VARCHAR(16)   NOT NULL,   -- IRI | LITERAL | BLANK
    datatype    VARCHAR(256)  NULL,
    lang        VARCHAR(32)   NULL,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX idx_inferred_subject ON inferred_triple (subject);
CREATE INDEX idx_inferred_predicate ON inferred_triple (predicate);
