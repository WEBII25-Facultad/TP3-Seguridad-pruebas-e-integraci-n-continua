package ar.edu.unvime.tp1favoritos.usuarios;

import ar.edu.unvime.tp1favoritos.error.EmailYaRegistradoException;
import ar.edu.unvime.tp1favoritos.usuarios.domain.Usuario;
import ar.edu.unvime.tp1favoritos.usuarios.dto.RegistroRequest;
import ar.edu.unvime.tp1favoritos.usuarios.dto.UsuarioResponse;
import ar.edu.unvime.tp1favoritos.usuarios.repository.UsuarioRepository;
import ar.edu.unvime.tp1favoritos.usuarios.service.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    @DisplayName("registrar: hashea la contraseña y nunca persiste el password en texto plano")
    void registrar_datosValidos_hasheaPasswordYNuncaAlmacenaTextoPlano() {
        // Arrange
        String email = "nuevo@unvime.edu.ar";
        String passwordPlana = "secreto123";
        String passwordHasheada = "$2a$10$e8w...hashBCryptGenerado";

        RegistroRequest request = new RegistroRequest(email, passwordPlana);

        when(usuarioRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(passwordPlana)).thenReturn(passwordHasheada);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            return new Usuario(1L, u.email(), u.passwordHash(), u.rol());
        });

        // Act
        UsuarioResponse response = usuarioService.registrar(request);

        // Assert
        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        Usuario guardado = usuarioCaptor.getValue();

        assertThat(guardado.email()).isEqualTo(email);
        assertThat(guardado.passwordHash()).isEqualTo(passwordHasheada);
        assertThat(guardado.passwordHash()).isNotEqualTo(passwordPlana);
        assertThat(guardado.rol()).isEqualTo("USER");
        assertThat(response.email()).isEqualTo(email);
        assertThat(response.rol()).isEqualTo("USER");

        verify(passwordEncoder).encode(passwordPlana);
    }

    @Test
    @DisplayName("registrar: si el email ya existe lanza EmailYaRegistradoException y no guarda nada")
    void registrar_emailDuplicado_lanzaEmailYaRegistradoException() {
        // Arrange
        String email = "duplicado@unvime.edu.ar";
        RegistroRequest request = new RegistroRequest(email, "password123");

        when(usuarioRepository.existsByEmail(email)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> usuarioService.registrar(request))
                .isInstanceOf(EmailYaRegistradoException.class)
                .hasMessageContaining(email);

        verify(usuarioRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }
}
