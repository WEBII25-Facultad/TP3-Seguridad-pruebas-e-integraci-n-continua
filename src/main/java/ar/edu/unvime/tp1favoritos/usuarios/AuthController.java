package ar.edu.unvime.tp1favoritos.usuarios;

import ar.edu.unvime.tp1favoritos.usuarios.dto.LoginRequest;
import ar.edu.unvime.tp1favoritos.usuarios.dto.LoginResponse;
import ar.edu.unvime.tp1favoritos.usuarios.dto.RegistroRequest;
import ar.edu.unvime.tp1favoritos.usuarios.dto.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Endpoints públicos de registro e inicio de sesión con JWT")
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/registro")
    @Operation(
            summary = "Registrar un nuevo usuario",
            description = "Crea un usuario nuevo con email y contraseña hasheada (rol 'USER' por defecto). Endpoint público.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Usuario registrado exitosamente"),
                    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
                    @ApiResponse(responseCode = "409", description = "El email ya se encuentra registrado")
            }
    )
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        UsuarioResponse nuevoUsuario = usuarioService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoUsuario);
    }

    @PostMapping("/login")
    @Operation(
            summary = "Iniciar sesión y obtener token JWT",
            description = "Valida credenciales contra la base de datos y retorna un token JWT Bearer. Endpoint público.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Autenticación exitosa, token JWT retornado"),
                    @ApiResponse(responseCode = "400", description = "Formato de credenciales inválido"),
                    @ApiResponse(responseCode = "401", description = "Credenciales incorrectas")
            }
    )
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = usuarioService.login(request);
        return ResponseEntity.ok(response);
    }
}
