package ar.edu.unvime.tp1favoritos.listas.dto;

public record ListaResponse(
        Long id,
        String nombre,
        Long usuarioId
) {
    public ListaResponse(Long id, String nombre) {
        this(id, nombre, null);
    }
}
