package ar.edu.unvime.tp1favoritos.usuarios;

import ar.edu.unvime.tp1favoritos.error.CredencialesInvalidasException;
import ar.edu.unvime.tp1favoritos.error.EmailYaRegistradoException;
import ar.edu.unvime.tp1favoritos.usuarios.domain.Usuario;
import ar.edu.unvime.tp1favoritos.usuarios.dto.LoginRequest;
import ar.edu.unvime.tp1favoritos.usuarios.dto.LoginResponse;
import ar.edu.unvime.tp1favoritos.usuarios.dto.RegistroRequest;
import ar.edu.unvime.tp1favoritos.usuarios.dto.UsuarioResponse;
import ar.edu.unvime.tp1favoritos.usuarios.repository.UsuarioRepository;
import ar.edu.unvime.tp1favoritos.usuarios.service.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private static final String ROL_DEFAULT = "USER";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UsuarioResponse registrar(RegistroRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new EmailYaRegistradoException(request.email());
        }

        String passwordHasheada = passwordEncoder.encode(request.password());
        Usuario nuevoUsuario = new Usuario(null, request.email(), passwordHasheada, ROL_DEFAULT);
        Usuario guardado = usuarioRepository.save(nuevoUsuario);

        return new UsuarioResponse(guardado.id(), guardado.email(), guardado.rol());
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(CredencialesInvalidasException::new);

        if (!passwordEncoder.matches(request.password(), usuario.passwordHash())) {
            throw new CredencialesInvalidasException();
        }

        String token = jwtService.generarToken(usuario.id(), usuario.email(), usuario.rol());
        UsuarioResponse usuarioResponse = new UsuarioResponse(usuario.id(), usuario.email(), usuario.rol());

        return new LoginResponse(token, usuarioResponse);
    }
}
