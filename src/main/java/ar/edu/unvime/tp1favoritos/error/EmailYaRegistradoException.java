package ar.edu.unvime.tp1favoritos.error;

public class EmailYaRegistradoException extends RuntimeException {
    public EmailYaRegistradoException(String email) {
        super("El email " + email + " ya se encuentra registrado");
    }
}
