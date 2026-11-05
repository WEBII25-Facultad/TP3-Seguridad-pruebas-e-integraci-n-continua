package ar.edu.unvime.tp1favoritos.error;

public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException() {
        super("Credenciales inválidas: email o contraseña incorrectos");
    }
}
