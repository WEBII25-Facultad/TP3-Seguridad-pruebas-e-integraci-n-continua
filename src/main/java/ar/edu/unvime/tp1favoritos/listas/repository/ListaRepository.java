package ar.edu.unvime.tp1favoritos.listas.repository;

import ar.edu.unvime.tp1favoritos.listas.domain.Lista;
import java.util.List;
import java.util.Optional;

public interface ListaRepository {
    List<Lista> findAll();
    List<Lista> findByUsuarioId(Long usuarioId);
    Optional<Lista> findById(Long id);
    Lista save(Lista lista);
    void deleteById(Long id);
}
