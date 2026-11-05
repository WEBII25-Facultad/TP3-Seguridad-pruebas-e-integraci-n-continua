package ar.edu.unvime.tp1favoritos.usuarios.repository;

import ar.edu.unvime.tp1favoritos.usuarios.domain.Usuario;
import ar.edu.unvime.tp1favoritos.usuarios.entity.UsuarioEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UsuarioRepositoryAdapter implements UsuarioRepository {

    private final UsuarioJpaRepository usuarioJpaRepository;

    public UsuarioRepositoryAdapter(UsuarioJpaRepository usuarioJpaRepository) {
        this.usuarioJpaRepository = usuarioJpaRepository;
    }

    @Override
    public Usuario save(Usuario usuario) {
        UsuarioEntity entity = new UsuarioEntity(
                usuario.id(),
                usuario.email(),
                usuario.passwordHash(),
                usuario.rol()
        );
        UsuarioEntity guardado = usuarioJpaRepository.save(entity);
        return aDominio(guardado);
    }

    @Override
    public Optional<Usuario> findById(Long id) {
        return usuarioJpaRepository.findById(id).map(this::aDominio);
    }

    @Override
    public Optional<Usuario> findByEmail(String email) {
        return usuarioJpaRepository.findByEmail(email).map(this::aDominio);
    }

    @Override
    public boolean existsByEmail(String email) {
        return usuarioJpaRepository.existsByEmail(email);
    }

    private Usuario aDominio(UsuarioEntity entity) {
        return new Usuario(
                entity.getId(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getRol()
        );
    }
}
