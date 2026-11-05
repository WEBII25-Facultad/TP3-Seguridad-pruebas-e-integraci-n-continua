package ar.edu.unvime.tp1favoritos.favoritos;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import ar.edu.unvime.tp1favoritos.error.RecursoNoEncontradoException;
import ar.edu.unvime.tp1favoritos.favoritos.domain.Favorito;
import ar.edu.unvime.tp1favoritos.favoritos.dto.FavoritoRequest;
import ar.edu.unvime.tp1favoritos.favoritos.dto.FavoritoResponse;
import ar.edu.unvime.tp1favoritos.favoritos.repository.FavoritoRepository;
import ar.edu.unvime.tp1favoritos.listas.domain.Lista;
import ar.edu.unvime.tp1favoritos.listas.repository.ListaRepository;
import ar.edu.unvime.tp1favoritos.usuarios.domain.Usuario;
import ar.edu.unvime.tp1favoritos.usuarios.service.UsuarioActualService;

@Service
public class FavoritoService {

    private final FavoritoRepository repository;
    private final ListaRepository listaRepository;
    private final UsuarioActualService usuarioActualService;

    public FavoritoService(
            FavoritoRepository repository,
            ListaRepository listaRepository,
            UsuarioActualService usuarioActualService
    ) {
        this.repository = repository;
        this.listaRepository = listaRepository;
        this.usuarioActualService = usuarioActualService;
    }

    public FavoritoResponse crear(FavoritoRequest request) {
        Lista lista = buscarListaOFallar(request.listaId());
        Usuario usuarioActual = usuarioActualService.obtenerUsuarioActual();
        usuarioActualService.validarPropiedadOAdmin(usuarioActual, lista.usuarioId(), lista.id());

        Favorito nuevo = new Favorito(
                null,
                request.productoId(),
                request.nota(),
                LocalDateTime.now(),
                request.listaId());

        return aResponse(repository.save(nuevo));
    }

    public List<FavoritoResponse> listar() {
        Usuario usuarioActual = usuarioActualService.obtenerUsuarioActual();

        if (usuarioActualService.esAdmin(usuarioActual)) {
            return repository.findAll().stream()
                    .map(this::aResponse)
                    .toList();
        }

        List<Lista> misListas = listaRepository.findByUsuarioId(usuarioActual.id());
        return misListas.stream()
                .flatMap(l -> repository.findByListaId(l.id()).stream())
                .map(this::aResponse)
                .toList();
    }

    public FavoritoResponse obtenerPorId(Long id) {
        Favorito f = buscarOFallar(id);
        Lista lista = buscarListaOFallar(f.listaId());
        Usuario usuarioActual = usuarioActualService.obtenerUsuarioActual();
        usuarioActualService.validarPropiedadOAdmin(usuarioActual, lista.usuarioId(), lista.id());

        return aResponse(f);
    }

    public FavoritoResponse actualizar(Long id, FavoritoRequest request) {
        Lista listaDestino = buscarListaOFallar(request.listaId());
        Usuario usuarioActual = usuarioActualService.obtenerUsuarioActual();
        usuarioActualService.validarPropiedadOAdmin(usuarioActual, listaDestino.usuarioId(), listaDestino.id());

        Favorito existente = buscarOFallar(id);
        if (!existente.listaId().equals(request.listaId())) {
            Lista listaOrigen = buscarListaOFallar(existente.listaId());
            usuarioActualService.validarPropiedadOAdmin(usuarioActual, listaOrigen.usuarioId(), listaOrigen.id());
        }

        Favorito actualizado = new Favorito(
                existente.id(),
                request.productoId(),
                request.nota(),
                existente.fechaAgregado(), // no se pisa la fecha original
                request.listaId());

        return aResponse(repository.save(actualizado));
    }

    public void eliminar(Long id) {
        Favorito existente = buscarOFallar(id); // si no existe, lanza 404
        Lista lista = buscarListaOFallar(existente.listaId());
        Usuario usuarioActual = usuarioActualService.obtenerUsuarioActual();
        usuarioActualService.validarPropiedadOAdmin(usuarioActual, lista.usuarioId(), lista.id());

        repository.deleteById(id);
    }

    private Favorito buscarOFallar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el favorito con id " + id));
    }

    private Lista buscarListaOFallar(Long listaId) {
        return listaRepository.findById(listaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la lista con id " + listaId));
    }

    private FavoritoResponse aResponse(Favorito f) {
        return new FavoritoResponse(f.id(), f.productoId(), f.nota(), f.fechaAgregado(), f.listaId());
    }
}
