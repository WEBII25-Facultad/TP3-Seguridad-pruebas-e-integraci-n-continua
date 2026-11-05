package ar.edu.unvime.tp1favoritos.listas.domain;

public record Lista(
        Long id,
        String nombre,
        Long usuarioId
) {
    public Lista(Long id, String nombre) {
        this(id, nombre, null);
    }
}
