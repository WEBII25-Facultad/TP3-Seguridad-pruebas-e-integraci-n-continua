package ar.edu.unvime.tp1favoritos.listas.repository;

import ar.edu.unvime.tp1favoritos.listas.domain.Lista;
import ar.edu.unvime.tp1favoritos.listas.entity.ListaEntity;
import ar.edu.unvime.tp1favoritos.usuarios.repository.UsuarioJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ListaRepositoryAdapter implements ListaRepository {

    private final ListaJpaRepository jpaRepository;
    private final UsuarioJpaRepository usuarioJpaRepository;

    public ListaRepositoryAdapter(ListaJpaRepository jpaRepository, UsuarioJpaRepository usuarioJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.usuarioJpaRepository = usuarioJpaRepository;
    }

    @Override
    public List<Lista> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::aDominio)
                .collect(Collectors.toList());
    }

    @Override
    public List<Lista> findByUsuarioId(Long usuarioId) {
        return jpaRepository.findByUsuarioId(usuarioId).stream()
                .map(this::aDominio)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Lista> findById(Long id) {
        return jpaRepository.findById(id).map(this::aDominio);
    }

    @Override
    public Lista save(Lista lista) {
        ListaEntity entity = new ListaEntity();
        entity.setId(lista.id());
        entity.setNombre(lista.nombre());

        if (lista.usuarioId() != null) {
            entity.setUsuario(usuarioJpaRepository.getReferenceById(lista.usuarioId()));
        }

        ListaEntity savedEntity = jpaRepository.save(entity);
        return aDominio(savedEntity);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    private Lista aDominio(ListaEntity entity) {
        Long usuarioId = entity.getUsuario() != null ? entity.getUsuario().getId() : null;
        return new Lista(entity.getId(), entity.getNombre(), usuarioId);
    }
}
