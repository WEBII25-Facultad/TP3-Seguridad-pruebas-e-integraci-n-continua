package ar.edu.unvime.tp1favoritos.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail manejarNoEncontrado(RecursoNoEncontradoException e) {
        ProblemDetail detalle = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        detalle.setDetail(e.getMessage());
        return detalle;
    }

    @ExceptionHandler(ServicioExternoException.class)
    public ProblemDetail manejarServicioExterno(ServicioExternoException e) {
        ProblemDetail detalle = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
        detalle.setTitle("Falla al consumir un servicio externo");
        detalle.setDetail(e.getMessage());
        return detalle;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail manejarValidacion(MethodArgumentNotValidException e) {
        String detalle = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problema.setTitle("Error de validación");
        problema.setDetail(detalle);
        return problema;
    }

    @ExceptionHandler(ListaNoVaciaException.class)
    public ProblemDetail manejarListaNoVacia(ListaNoVaciaException e) {
        ProblemDetail detalle = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        detalle.setDetail(e.getMessage());
        return detalle;
    }

    @ExceptionHandler(OperacionInvalidaException.class)
    public ProblemDetail manejarOperacionInvalida(OperacionInvalidaException e) {
        ProblemDetail detalle = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        detalle.setDetail(e.getMessage());
        return detalle;
    }

    @ExceptionHandler(EmailYaRegistradoException.class)
    public ProblemDetail manejarEmailYaRegistrado(EmailYaRegistradoException e) {
        ProblemDetail detalle = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        detalle.setTitle("Conflicto de registro");
        detalle.setDetail(e.getMessage());
        return detalle;
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ProblemDetail manejarCredencialesInvalidas(CredencialesInvalidasException e) {
        ProblemDetail detalle = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        detalle.setTitle("No autorizado");
        detalle.setDetail(e.getMessage());
        return detalle;
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ProblemDetail manejarAccesoDenegado(AccesoDenegadoException e) {
        ProblemDetail detalle = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        detalle.setTitle("Acceso denegado");
        detalle.setDetail(e.getMessage());
        return detalle;
    }
}
