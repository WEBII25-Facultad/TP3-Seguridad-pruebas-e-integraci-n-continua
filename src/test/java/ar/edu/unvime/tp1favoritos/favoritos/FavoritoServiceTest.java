package ar.edu.unvime.tp1favoritos.favoritos;

import ar.edu.unvime.tp1favoritos.error.AccesoDenegadoException;
import ar.edu.unvime.tp1favoritos.error.RecursoNoEncontradoException;
import ar.edu.unvime.tp1favoritos.favoritos.domain.Favorito;
import ar.edu.unvime.tp1favoritos.favoritos.dto.FavoritoRequest;
import ar.edu.unvime.tp1favoritos.favoritos.dto.FavoritoResponse;
import ar.edu.unvime.tp1favoritos.favoritos.repository.FavoritoRepository;
import ar.edu.unvime.tp1favoritos.listas.domain.Lista;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoritoServiceTest {

    @Mock
    private FavoritoRepository repository;

    @Mock
    private ListaRepository listaRepository;

    @Mock
    private UsuarioActualService usuarioActualService;

    @InjectMocks
    private FavoritoService favoritoService;

    private Usuario usuarioAutenticado;
    private Usuario usuarioAdmin;
    private Lista listaPropia;
    private Lista listaAjena;

    @BeforeEach
    void setUp() {
        usuarioAutenticado = new Usuario(10L, "usuario@test.com", "hash123", "USER");
        usuarioAdmin = new Usuario(99L, "admin@test.com", "hashAdmin", "ADMIN");
        listaPropia = new Lista(1L, "Lista Propia", 10L);
        listaAjena = new Lista(2L, "Lista Ajena", 20L);
    }

    @Test
    @DisplayName("crear_listaPropia_creaYRetornaFavorito")
    void crear_listaPropia_creaYRetornaFavorito() {
        // Arrange
        FavoritoRequest request = new FavoritoRequest(100L, "Muy buen producto", 1L);
        when(listaRepository.findById(1L)).thenReturn(Optional.of(listaPropia));
        when(usuarioActualService.obtenerUsuarioActual()).thenReturn(usuarioAutenticado);
        doNothing().when(usuarioActualService).validarPropiedadOAdmin(usuarioAutenticado, 10L, 1L);

        when(repository.save(any(Favorito.class))).thenAnswer(inv -> {
            Favorito f = inv.getArgument(0);
            return new Favorito(50L, f.productoId(), f.nota(), f.fechaAgregado(), f.listaId());
        });

        // Act
        FavoritoResponse response = favoritoService.crear(request);

        // Assert
        ArgumentCaptor<Favorito> captor = ArgumentCaptor.forClass(Favorito.class);
        verify(repository).save(captor.capture());
        Favorito guardado = captor.getValue();

        assertThat(guardado.productoId()).isEqualTo(100L);
        assertThat(guardado.nota()).isEqualTo("Muy buen producto");
        assertThat(guardado.listaId()).isEqualTo(1L);
        assertThat(response.id()).isEqualTo(50L);
        verify(usuarioActualService).validarPropiedadOAdmin(usuarioAutenticado, 10L, 1L);
    }

    @Test
    @DisplayName("crear_listaAjenaYNoAdmin_lanzaAccesoDenegado")
    void crear_listaAjenaYNoAdmin_lanzaAccesoDenegado() {
        // Arrange
        FavoritoRequest request = new FavoritoRequest(100L, "Nota", 2L);
        when(listaRepository.findById(2L)).thenReturn(Optional.of(listaAjena));
        when(usuarioActualService.obtenerUsuarioActual()).thenReturn(usuarioAutenticado);
        doThrow(new AccesoDenegadoException("No tiene permisos para operar sobre la lista con id 2"))
                .when(usuarioActualService).validarPropiedadOAdmin(usuarioAutenticado, 20L, 2L);

        // Act & Assert
        assertThatThrownBy(() -> favoritoService.crear(request))
                .isInstanceOf(AccesoDenegadoException.class)
                .hasMessageContaining("No tiene permisos");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("crear_listaInexistente_lanzaRecursoNoEncontrado")
    void crear_listaInexistente_lanzaRecursoNoEncontrado() {
        // Arrange
        FavoritoRequest request = new FavoritoRequest(100L, "Nota", 999L);
        when(listaRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> favoritoService.crear(request))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("999");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("listar_usuarioComun_retornaFavoritosDeSusListas")
    void listar_usuarioComun_retornaFavoritosDeSusListas() {
        // Arrange
        when(usuarioActualService.obtenerUsuarioActual()).thenReturn(usuarioAutenticado);
        when(usuarioActualService.esAdmin(usuarioAutenticado)).thenReturn(false);
        when(listaRepository.findByUsuarioId(10L)).thenReturn(List.of(listaPropia));

        Favorito fav1 = new Favorito(1L, 101L, "Nota 1", LocalDateTime.now(), 1L);
        when(repository.findByListaId(1L)).thenReturn(List.of(fav1));

        // Act
        List<FavoritoResponse> lista = favoritoService.listar();

        // Assert
        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).productoId()).isEqualTo(101L);
        verify(repository, never()).findAll();
    }

    @Test
    @DisplayName("listar_usuarioAdmin_retornaTodosLosFavoritos")
    void listar_usuarioAdmin_retornaTodosLosFavoritos() {
        // Arrange
        when(usuarioActualService.obtenerUsuarioActual()).thenReturn(usuarioAdmin);
        when(usuarioActualService.esAdmin(usuarioAdmin)).thenReturn(true);

        Favorito fav1 = new Favorito(1L, 101L, "Nota 1", LocalDateTime.now(), 1L);
        Favorito fav2 = new Favorito(2L, 102L, "Nota 2", LocalDateTime.now(), 2L);
        when(repository.findAll()).thenReturn(List.of(fav1, fav2));

        // Act
        List<FavoritoResponse> lista = favoritoService.listar();

        // Assert
        assertThat(lista).hasSize(2);
        verify(listaRepository, never()).findByUsuarioId(any());
    }

    @Test
    @DisplayName("obtenerPorId_favoritoEnListaPropia_retornaFavorito")
    void obtenerPorId_favoritoEnListaPropia_retornaFavorito() {
        // Arrange
        Favorito fav = new Favorito(1L, 101L, "Nota 1", LocalDateTime.now(), 1L);
        when(repository.findById(1L)).thenReturn(Optional.of(fav));
        when(listaRepository.findById(1L)).thenReturn(Optional.of(listaPropia));
        when(usuarioActualService.obtenerUsuarioActual()).thenReturn(usuarioAutenticado);
        doNothing().when(usuarioActualService).validarPropiedadOAdmin(usuarioAutenticado, 10L, 1L);

        // Act
        FavoritoResponse response = favoritoService.obtenerPorId(1L);

        // Assert
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.productoId()).isEqualTo(101L);
        verify(usuarioActualService).validarPropiedadOAdmin(usuarioAutenticado, 10L, 1L);
    }

    @Test
    @DisplayName("obtenerPorId_favoritoEnListaAjenaYNoAdmin_lanzaAccesoDenegado")
    void obtenerPorId_favoritoEnListaAjenaYNoAdmin_lanzaAccesoDenegado() {
        // Arrange
        Favorito fav = new Favorito(2L, 102L, "Nota 2", LocalDateTime.now(), 2L);
        when(repository.findById(2L)).thenReturn(Optional.of(fav));
        when(listaRepository.findById(2L)).thenReturn(Optional.of(listaAjena));
        when(usuarioActualService.obtenerUsuarioActual()).thenReturn(usuarioAutenticado);
        doThrow(new AccesoDenegadoException("No tiene permisos para operar sobre la lista con id 2"))
                .when(usuarioActualService).validarPropiedadOAdmin(usuarioAutenticado, 20L, 2L);

        // Act & Assert
        assertThatThrownBy(() -> favoritoService.obtenerPorId(2L))
                .isInstanceOf(AccesoDenegadoException.class)
                .hasMessageContaining("No tiene permisos");
    }

    @Test
    @DisplayName("obtenerPorId_favoritoInexistente_lanzaRecursoNoEncontrado")
    void obtenerPorId_favoritoInexistente_lanzaRecursoNoEncontrado() {
        // Arrange
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> favoritoService.obtenerPorId(999L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("actualizar_favoritoEnListaPropia_retornaFavoritoActualizado")
    void actualizar_favoritoEnListaPropia_retornaFavoritoActualizado() {
        // Arrange
        FavoritoRequest request = new FavoritoRequest(200L, "Nota actualizada", 1L);
        LocalDateTime fechaOriginal = LocalDateTime.now().minusDays(1);
        Favorito existente = new Favorito(1L, 100L, "Nota previa", fechaOriginal, 1L);

        when(listaRepository.findById(1L)).thenReturn(Optional.of(listaPropia));
        when(usuarioActualService.obtenerUsuarioActual()).thenReturn(usuarioAutenticado);
        doNothing().when(usuarioActualService).validarPropiedadOAdmin(usuarioAutenticado, 10L, 1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.save(any(Favorito.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        FavoritoResponse response = favoritoService.actualizar(1L, request);

        // Assert
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.productoId()).isEqualTo(200L);
        assertThat(response.nota()).isEqualTo("Nota actualizada");
        assertThat(response.fechaAgregado()).isEqualTo(fechaOriginal);
    }

    @Test
    @DisplayName("actualizar_listaDestinoAjenaYNoAdmin_lanzaAccesoDenegado")
    void actualizar_listaDestinoAjenaYNoAdmin_lanzaAccesoDenegado() {
        // Arrange
        FavoritoRequest request = new FavoritoRequest(200L, "Nota actualizada", 2L);
        when(listaRepository.findById(2L)).thenReturn(Optional.of(listaAjena));
        when(usuarioActualService.obtenerUsuarioActual()).thenReturn(usuarioAutenticado);
        doThrow(new AccesoDenegadoException("No tiene permisos para operar sobre la lista con id 2"))
                .when(usuarioActualService).validarPropiedadOAdmin(usuarioAutenticado, 20L, 2L);

        // Act & Assert
        assertThatThrownBy(() -> favoritoService.actualizar(1L, request))
                .isInstanceOf(AccesoDenegadoException.class)
                .hasMessageContaining("No tiene permisos");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("actualizar_favoritoInexistente_lanzaRecursoNoEncontrado")
    void actualizar_favoritoInexistente_lanzaRecursoNoEncontrado() {
        // Arrange
        FavoritoRequest request = new FavoritoRequest(200L, "Nota", 1L);
        when(listaRepository.findById(1L)).thenReturn(Optional.of(listaPropia));
        when(usuarioActualService.obtenerUsuarioActual()).thenReturn(usuarioAutenticado);
        doNothing().when(usuarioActualService).validarPropiedadOAdmin(usuarioAutenticado, 10L, 1L);
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> favoritoService.actualizar(999L, request))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("eliminar_favoritoEnListaPropia_eliminaExitosamente")
    void eliminar_favoritoEnListaPropia_eliminaExitosamente() {
        // Arrange
        Favorito existente = new Favorito(1L, 100L, "Nota", LocalDateTime.now(), 1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(listaRepository.findById(1L)).thenReturn(Optional.of(listaPropia));
        when(usuarioActualService.obtenerUsuarioActual()).thenReturn(usuarioAutenticado);
        doNothing().when(usuarioActualService).validarPropiedadOAdmin(usuarioAutenticado, 10L, 1L);

        // Act
        favoritoService.eliminar(1L);

        // Assert
        verify(repository).deleteById(1L);
    }

    @Test
    @DisplayName("eliminar_favoritoEnListaAjenaYNoAdmin_lanzaAccesoDenegado")
    void eliminar_favoritoEnListaAjenaYNoAdmin_lanzaAccesoDenegado() {
        // Arrange
        Favorito existente = new Favorito(2L, 100L, "Nota", LocalDateTime.now(), 2L);
        when(repository.findById(2L)).thenReturn(Optional.of(existente));
        when(listaRepository.findById(2L)).thenReturn(Optional.of(listaAjena));
        when(usuarioActualService.obtenerUsuarioActual()).thenReturn(usuarioAutenticado);
        doThrow(new AccesoDenegadoException("No tiene permisos para operar sobre la lista con id 2"))
                .when(usuarioActualService).validarPropiedadOAdmin(usuarioAutenticado, 20L, 2L);

        // Act & Assert
        assertThatThrownBy(() -> favoritoService.eliminar(2L))
                .isInstanceOf(AccesoDenegadoException.class)
                .hasMessageContaining("No tiene permisos");

        verify(repository, never()).deleteById(any());
    }

    @Test
    @DisplayName("eliminar_favoritoInexistente_lanzaRecursoNoEncontrado")
    void eliminar_favoritoInexistente_lanzaRecursoNoEncontrado() {
        // Arrange
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> favoritoService.eliminar(999L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("999");

        verify(repository, never()).deleteById(any());
    }
}
