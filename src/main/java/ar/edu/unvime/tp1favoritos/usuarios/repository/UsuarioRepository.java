package ar.edu.unvime.tp1favoritos.usuarios.repository;

import ar.edu.unvime.tp1favoritos.usuarios.domain.Usuario;

import java.util.Optional;

public interface UsuarioRepository {
    Usuario save(Usuario usuario);
    Optional<Usuario> findById(Long id);
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
}
