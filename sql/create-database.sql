-- ===========================================================================
--  Local MySQL / single-instance TDSQL bootstrap (optional).
--
--  On managed TDSQL the database and account are usually created from the
--  console instead, in which case only sql/schema.sql + sql/seed.sql are
--  needed.  This file is for a plain MySQL-compatible server (including the
--  docker-compose.yml stand-in).
-- ===========================================================================

CREATE DATABASE IF NOT EXISTS ontology_db
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

CREATE USER IF NOT EXISTS 'ontology'@'%' IDENTIFIED BY 'ontology123';
GRANT ALL PRIVILEGES ON ontology_db.* TO 'ontology'@'%';
FLUSH PRIVILEGES;
