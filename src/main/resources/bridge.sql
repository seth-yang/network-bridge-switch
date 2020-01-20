CREATE TABLE t_user (
    user_name       TEXT        NOT NULL PRIMARY KEY,
    password        TEXT
);

CREATE TABLE t_device (
    id              INTEGER     NOT NULL PRIMARY KEY,
    name            TEXT,
    host            TEXT,
    port            INTEGER,
    user            TEXT,
    password        TEXT
);

CREATE TABLE t_nat (
    id              INTEGER     NOT NULL PRIMARY KEY,
    src_port        INTEGER     NOT NULL,
    dest_host       TEXT        NOT NULL,
    dest_port       TEXT        NOT NULL,
    auto_bind       INTEGER     NOT NULL DEFAULT 0
);

CREATE TABLE t_tunnel_client (
    token           TEXT       NOT NULL PRIMARY KEY,
    name            TEXT       NOT NULL,
    password        TEXT       NOT NULL,
    last_conn       TEXT
);

CREATE TABLE t_sys_conf (
    id              TEXT        NOT NULL PRIMARY KEY,
    _value          TEXT,
    editable        INTEGER
);