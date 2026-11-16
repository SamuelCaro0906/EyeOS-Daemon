-- EyeOS Daemon - Esquema PostgreSQL
-- Ejecutar: psql -U postgres -d eyeos -f sql/schema.sql

CREATE TABLE IF NOT EXISTS equipos (
    id              SERIAL PRIMARY KEY,
    hostname        TEXT NOT NULL UNIQUE,
    usuario         TEXT,
    so_nombre       TEXT,
    so_version      TEXT,
    so_edicion      TEXT,
    bios_serial     TEXT,
    baseboard_uuid  TEXT
);

CREATE TABLE IF NOT EXISTS reportes (
    id                SERIAL PRIMARY KEY,
    equipo_id         INTEGER NOT NULL REFERENCES equipos(id) ON DELETE CASCADE,
    fecha             TIMESTAMP NOT NULL,
    consentimiento    TEXT,
    ram_total_mb      BIGINT,
    ram_libre_mb      BIGINT,
    uptime_segundos   BIGINT,
    foreground_app    TEXT
);

CREATE TABLE IF NOT EXISTS red (
    id          SERIAL PRIMARY KEY,
    reporte_id  INTEGER NOT NULL REFERENCES reportes(id) ON DELETE CASCADE,
    tipo        TEXT NOT NULL,
    valor       TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS conexiones (
    id             SERIAL PRIMARY KEY,
    reporte_id     INTEGER NOT NULL REFERENCES reportes(id) ON DELETE CASCADE,
    estado         TEXT,
    protocolo      TEXT,
    puerto         INTEGER,
    remota         TEXT,
    puerto_remoto  INTEGER
);

CREATE TABLE IF NOT EXISTS procesos (
    id          SERIAL PRIMARY KEY,
    reporte_id  INTEGER NOT NULL REFERENCES reportes(id) ON DELETE CASCADE,
    pid         INTEGER,
    nombre      TEXT,
    cpu_pct     NUMERIC(6,2),
    mem_mb      BIGINT
);

CREATE INDEX IF NOT EXISTS idx_reportes_equipo ON reportes(equipo_id);
CREATE INDEX IF NOT EXISTS idx_reportes_fecha ON reportes(fecha DESC);
CREATE INDEX IF NOT EXISTS idx_red_reporte ON red(reporte_id);
CREATE INDEX IF NOT EXISTS idx_conexiones_reporte ON conexiones(reporte_id);
CREATE INDEX IF NOT EXISTS idx_procesos_reporte ON procesos(reporte_id);

-- Migracion para bases existentes
ALTER TABLE reportes ADD COLUMN IF NOT EXISTS uptime_segundos BIGINT;
ALTER TABLE reportes ADD COLUMN IF NOT EXISTS foreground_app TEXT;
ALTER TABLE conexiones ADD COLUMN IF NOT EXISTS remota TEXT;
ALTER TABLE conexiones ADD COLUMN IF NOT EXISTS puerto_remoto INTEGER;
