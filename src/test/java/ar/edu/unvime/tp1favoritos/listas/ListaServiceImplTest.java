package ar.edu.unvime.tp1favoritos.listas;

import ar.edu.unvime.tp1favoritos.error.AccesoDenegadoException;
import ar.edu.unvime.tp1favoritos.favoritos.repository.FavoritoRepository;
import ar.edu.unvime.tp1favoritos.listas.domain.Lista;
import ar.edu.unvime.tp1favoritos.listas.dto.ListaRequest;
import ar.edu.unvime.tp1favoritos.listas.dto.ListaResponse;
import ar.edu.unvime.tp1favoritos.listas.repository.ListaRepository;
import ar.edu.unvime.tp1favoritos.usuarios.domain.Usuario;
import ar.edu.unvime.tp1favoritos.usuarios.service.UsuarioActualService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListaServiceImplTest {

    @Mock
    private ListaRepository listaRepository;

    @Mock
    private FavoritoRepository favoritoRepository;

    @Mock
    private UsuarioActualService usuarioActualService;

    @InjectMocks
    private ListaService listaService;

    private Usuario usuarioAutenticado;

    @BeforeEach
    void setUp() {
        usuarioAutenticado = new Usuario(10L, "usuario@test.com", "hash123", "USER");
        lenient().doCallRealMethod().when(usuarioActualService).validarPropiedadOAdmin(any(), any(), any());
    }

    @Test
    @DisplayName("crear: asocia automáticamente el id del usuario autenticado actual a la lista")
    void crear_usuarioAutenticado_asociaUsuarioIdCorrectamente() {
        // Arrange
        ListaRequest request = new ListaRequest("Tecnología");
        when(usuarioActualService.obtenerUsuarioActual()).thenReturn(usuarioAutenticado);
        when(listaRepository.save(any(Lista.class))).thenAnswer(invocation -> {
            Lista pasada = invocation.getArgument(0);
            return new Lista(1L, pasada.nombre(), pasada.usuarioId());
        });

        // Act
        ListaResponse response = listaService.crear(request);

        // Assert
        ArgumentCaptor<Lista> captor = ArgumentCaptor.forClass(Lista.class);
        verify(listaRepository).save(captor.capture());
        Lista guardada = captor.getValue();

        assertThat(guardada.nombre()).isEqualTo("Tecnología");
        assertThat(guardada.usuarioId()).isEqualTo(10L);
        assertThat(response.usuarioId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("obtenerPorId: lanza AccesoDenegadoException si la lista pertenece a otro usuario y no es ADMIN")
    void obtenerPorId_listaAjenaYNoAdmin_lanzaAccesoDenegado() {
        // Arrange
        Long listaId = 5L;
        Lista listaDeOtro = new Lista(listaId, "Lista Ajena", 99L);
        when(listaRepository.findById(listaId)).thenReturn(Optional.of(listaDeOtro));
        when(usuarioActualService.obtenerUsuarioActual()).thenReturn(usuarioAutenticado);
        when(usuarioActualService.esAdmin(usuarioAutenticado)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> listaService.obtenerPorId(listaId))
                .isInstanceOf(AccesoDenegadoException.class)
                .hasMessageContaining("No tiene permisos");
    }

    @Test
    @DisplayName("obtenerPorId: permite acceso a una lista ajena si el usuario tiene rol ADMIN")
    void obtenerPorId_listaAjenaYRolAdmin_retornaListaCorrectamente() {
        // Arrange
        Long listaId = 5L;
        Lista listaDeOtro = new Lista(listaId, "Lista Ajena", 99L);
        Usuario admin = new Usuario(20L, "admin@test.com", "hashAdmin", "ADMIN");

        when(listaRepository.findById(listaId)).thenReturn(Optional.of(listaDeOtro));
        when(usuarioActualService.obtenerUsuarioActual()).thenReturn(admin);
        when(usuarioActualService.esAdmin(admin)).thenReturn(true);

        // Act
        ListaResponse response = listaService.obtenerPorId(listaId);

        // Assert
        assertThat(response.id()).isEqualTo(listaId);
        assertThat(response.nombre()).isEqualTo("Lista Ajena");
        assertThat(response.usuarioId()).isEqualTo(99L);
    }
}
