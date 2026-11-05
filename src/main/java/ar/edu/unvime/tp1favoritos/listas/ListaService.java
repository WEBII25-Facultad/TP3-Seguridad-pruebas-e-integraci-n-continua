package ar.edu.unvime.tp1favoritos.listas;

import ar.edu.unvime.tp1favoritos.error.AccesoDenegadoException;
import ar.edu.unvime.tp1favoritos.error.ListaNoVaciaException;
import ar.edu.unvime.tp1favoritos.error.OperacionInvalidaException;
import ar.edu.unvime.tp1favoritos.error.RecursoNoEncontradoException;
import ar.edu.unvime.tp1favoritos.favoritos.domain.Favorito;
import ar.edu.unvime.tp1favoritos.favoritos.dto.FavoritoResponse;
import ar.edu.unvime.tp1favoritos.favoritos.repository.FavoritoRepository;
import ar.edu.unvime.tp1favoritos.listas.domain.Lista;
import ar.edu.unvime.tp1favoritos.listas.dto.ListaRequest;
import ar.edu.unvime.tp1favoritos.listas.dto.ListaResponse;
import ar.edu.unvime.tp1favoritos.listas.dto.MoverFavoritosRequest;
import ar.edu.unvime.tp1favoritos.listas.repository.ListaRepository;
import ar.edu.unvime.tp1favoritos.usuarios.domain.Usuario;
import ar.edu.unvime.tp1favoritos.usuarios.service.UsuarioActualService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListaService {

    private final ListaRepository listaRepository;
    private final FavoritoRepository favoritoRepository;
    private final UsuarioActualService usuarioActualService;

    public ListaService(
            ListaRepository listaRepository,
            FavoritoRepository favoritoRepository,
            UsuarioActualService usuarioActualService
    ) {
        this.listaRepository = listaRepository;
        this.favoritoRepository = favoritoRepository;
        this.usuarioActualService = usuarioActualService;
    }

    public ListaResponse crear(ListaRequest request) {
        Usuario usuarioActual = usuarioActualService.obtenerUsuarioActual();
        Lista nueva = new Lista(null, request.nombre(), usuarioActual.id());
        return aResponse(listaRepository.save(nueva));
    }

    public List<ListaResponse> listar() {
        Usuario usuarioActual = usuarioActualService.obtenerUsuarioActual();

        List<Lista> listas;
        if (usuarioActualService.esAdmin(usuarioActual)) {
            listas = listaRepository.findAll();
        } else {
            listas = listaRepository.findByUsuarioId(usuarioActual.id());
        }

        return listas.stream()
                .map(this::aResponse)
                .toList();
    }

    public ListaResponse obtenerPorId(Long id) {
        Lista lista = buscarOFallar(id);
        Usuario usuarioActual = usuarioActualService.obtenerUsuarioActual();
        validarPropiedadOAdmin(lista, usuarioActual);

        return aResponse(lista);
    }

    public List<FavoritoResponse> favoritosDeLista(Long id) {
        Lista lista = buscarOFallar(id);
        Usuario usuarioActual = usuarioActualService.obtenerUsuarioActual();
        validarPropiedadOAdmin(lista, usuarioActual);

        return favoritoRepository.findByListaId(id).stream()
                .map(f -> new FavoritoResponse(f.id(), f.productoId(), f.nota(), f.fechaAgregado(), f.listaId()))
                .toList();
    }

    public void eliminar(Long id) {
        Lista lista = buscarOFallar(id);
        Usuario usuarioActual = usuarioActualService.obtenerUsuarioActual();
        validarPropiedadOAdmin(lista, usuarioActual);

        if (!favoritoRepository.findByListaId(id).isEmpty()) {
            throw new ListaNoVaciaException("La lista " + id + " todavía tiene favoritos, no se puede eliminar");
        }
        listaRepository.deleteById(id);
    }

    @Transactional
    public void moverFavoritos(Long origenId, MoverFavoritosRequest request) {
        if (origenId.equals(request.listaDestinoId())) {
            throw new OperacionInvalidaException("La lista origen y la lista destino no pueden ser la misma");
        }

        Lista origen = buscarOFallar(origenId);
        Lista destino = buscarOFallar(request.listaDestinoId());

        Usuario usuarioActual = usuarioActualService.obtenerUsuarioActual();
        validarPropiedadOAdmin(origen, usuarioActual);
        validarPropiedadOAdmin(destino, usuarioActual);

        List<Favorito> favoritos = favoritoRepository.findByListaId(origenId);
        for (Favorito f : favoritos) {
            favoritoRepository.save(new Favorito(f.id(), f.productoId(), f.nota(), f.fechaAgregado(), destino.id()));
        }

        listaRepository.deleteById(origenId);
    }

    private void validarPropiedadOAdmin(Lista lista, Usuario usuario) {
        usuarioActualService.validarPropiedadOAdmin(usuario, lista.usuarioId(), lista.id());
    }

    private Lista buscarOFallar(Long id) {
        return listaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la lista con id " + id));
    }

    private ListaResponse aResponse(Lista lista) {
        return new ListaResponse(lista.id(), lista.nombre(), lista.usuarioId());
    }
}
