package ar.edu.unvime.tp1favoritos.usuarios.dto;

public record LoginResponse(
        String token,
        String tokenType,
        UsuarioResponse usuario
) {
    public LoginResponse(String token, UsuarioResponse usuario) {
        this(token, "Bearer", usuario);
    }
}
