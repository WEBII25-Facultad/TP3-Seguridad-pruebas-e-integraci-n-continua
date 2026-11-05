package ar.edu.unvime.tp1favoritos.usuarios.domain;

public record Usuario(
        Long id,
        String email,
        String passwordHash,
        String rol
) {}
