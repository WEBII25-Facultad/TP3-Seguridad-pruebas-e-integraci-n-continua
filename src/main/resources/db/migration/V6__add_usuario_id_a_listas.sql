-- V6__add_usuario_id_a_listas.sql
-- Agrega columna usuario_id a la tabla listas con su foreign key a usuarios

ALTER TABLE listas
    ADD COLUMN usuario_id BIGINT;

ALTER TABLE listas
    ADD CONSTRAINT fk_listas_usuario
    FOREIGN KEY (usuario_id) REFERENCES usuarios (id);
