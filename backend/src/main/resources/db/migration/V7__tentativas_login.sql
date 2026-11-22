CREATE TABLE tentativa_login (
    conta          VARCHAR(200) NOT NULL PRIMARY KEY,
    falhas         INT NOT NULL,
    inicio         DATETIME(6) NOT NULL,
    bloqueado_ate  DATETIME(6) NULL
);
