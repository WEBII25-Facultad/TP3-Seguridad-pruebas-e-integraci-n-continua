package ar.edu.unvime.tp1favoritos.usuarios.service;

import ar.edu.unvime.tp1favoritos.error.AccesoDenegadoException;
import ar.edu.unvime.tp1favoritos.usuarios.domain.Usuario;
import ar.edu.unvime.tp1favoritos.usuarios.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UsuarioActualService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioActualService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario obtenerUsuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AccesoDenegadoException("No hay un usuario autenticado en la sesión");
        }

        String email = authentication.getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new AccesoDenegadoException("Usuario autenticado no encontrado en el sistema"));
    }

    public boolean esAdmin(Usuario usuario) {
        return usuario != null && "ADMIN".equalsIgnoreCase(usuario.rol());
    }
}
