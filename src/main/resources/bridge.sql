CREATE TABLE t_user (
    user_name       TEXT        PRIMARY KEY,
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