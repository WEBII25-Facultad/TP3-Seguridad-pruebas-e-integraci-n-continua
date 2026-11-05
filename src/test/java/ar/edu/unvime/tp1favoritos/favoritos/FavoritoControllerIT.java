package ar.edu.unvime.tp1favoritos.favoritos;

import ar.edu.unvime.tp1favoritos.BaseIntegrationTest;
import ar.edu.unvime.tp1favoritos.usuarios.domain.Usuario;
import ar.edu.unvime.tp1favoritos.usuarios.repository.UsuarioRepository;
import ar.edu.unvime.tp1favoritos.usuarios.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FavoritoControllerIT extends BaseIntegrationTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private Usuario usuarioA;
    private Usuario usuarioB;
    private Usuario usuarioAdmin;

    private String tokenA;
    private String tokenB;
    private String tokenAdmin;

    private Long listaAId;
    private Long favoritoAId;

    @BeforeEach
    void setUpDatos() {
        // 1. Crear usuarios de prueba
        usuarioA = usuarioRepository.save(new Usuario(
                null,
                "usuarioA@unvime.edu.ar",
                passwordEncoder.encode("passwordA123"),
                "USER"
        ));

        usuarioB = usuarioRepository.save(new Usuario(
                null,
                "usuarioB@unvime.edu.ar",
                passwordEncoder.encode("passwordB123"),
                "USER"
        ));

        usuarioAdmin = usuarioRepository.save(new Usuario(
                null,
                "admin@unvime.edu.ar",
                passwordEncoder.encode("adminPass123"),
                "ADMIN"
        ));

        // 2. Generar tokens JWT
        tokenA = jwtService.generarToken(usuarioA.id(), usuarioA.email(), usuarioA.rol());
        tokenB = jwtService.generarToken(usuarioB.id(), usuarioB.email(), usuarioB.rol());
        tokenAdmin = jwtService.generarToken(usuarioAdmin.id(), usuarioAdmin.email(), usuarioAdmin.rol());

        // 3. Crear lista y favorito pertenecientes a Usuario A
        listaAId = jdbcTemplate.queryForObject(
                "INSERT INTO listas (nombre, usuario_id) VALUES ('Lista de A', ?) RETURNING id",
                Long.class,
                usuarioA.id()
        );

        favoritoAId = jdbcTemplate.queryForObject(
                "INSERT INTO favoritos (producto_id, nota, fecha_alta, lista_id) VALUES (1, 'Favorito de A', ?, ?) RETURNING id",
                Long.class,
                LocalDateTime.now(),
                listaAId
        );
    }

    @Test
    @DisplayName("crear_favoritoEnListaPropia_retornaCreated")
    void crear_favoritoEnListaPropia_retornaCreated() throws Exception {
        String json = String.format("""
                {
                    "productoId": 10,
                    "nota": "Nuevo fav propio",
                    "listaId": %d
                }
                """, listaAId);

        mockMvc.perform(post("/api/favoritos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.productoId", is(10)))
                .andExpect(jsonPath("$.listaId", is(listaAId.intValue())));
    }

    @Test
    @DisplayName("crear_favoritoEnListaAjena_retornaForbidden")
    void crear_favoritoEnListaAjena_retornaForbidden() throws Exception {
        String json = String.format("""
                {
                    "productoId": 11,
                    "nota": "Intento en lista ajena",
                    "listaId": %d
                }
                """, listaAId);

        mockMvc.perform(post("/api/favoritos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("crear_favoritoEnListaAjenaConRolAdmin_retornaCreated")
    void crear_favoritoEnListaAjenaConRolAdmin_retornaCreated() throws Exception {
        String json = String.format("""
                {
                    "productoId": 12,
                    "nota": "Admin creando en lista de A",
                    "listaId": %d
                }
                """, listaAId);

        mockMvc.perform(post("/api/favoritos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.productoId", is(12)));
    }

    @Test
    @DisplayName("obtenerPorId_favoritoEnListaPropia_retornaOk")
    void obtenerPorId_favoritoEnListaPropia_retornaOk() throws Exception {
        mockMvc.perform(get("/api/favoritos/{id}", favoritoAId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(favoritoAId.intValue())))
                .andExpect(jsonPath("$.nota", is("Favorito de A")));
    }

    @Test
    @DisplayName("obtenerPorId_favoritoEnListaAjena_retornaForbidden")
    void obtenerPorId_favoritoEnListaAjena_retornaForbidden() throws Exception {
        mockMvc.perform(get("/api/favoritos/{id}", favoritoAId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("obtenerPorId_favoritoEnListaAjenaConRolAdmin_retornaOk")
    void obtenerPorId_favoritoEnListaAjenaConRolAdmin_retornaOk() throws Exception {
        mockMvc.perform(get("/api/favoritos/{id}", favoritoAId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(favoritoAId.intValue())))
                .andExpect(jsonPath("$.nota", is("Favorito de A")));
    }

    @Test
    @DisplayName("listar_usuarioComun_retornaSoloFavoritosDeSusListas")
    void listar_usuarioComun_retornaSoloFavoritosDeSusListas() throws Exception {
        // Usuario B no tiene listas ni favoritos creados
        mockMvc.perform(get("/api/favoritos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Usuario A tiene 1 favorito en su lista
        mockMvc.perform(get("/api/favoritos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(favoritoAId.intValue())));
    }

    @Test
    @DisplayName("listar_admin_retornaTodosLosFavoritos")
    void listar_admin_retornaTodosLosFavoritos() throws Exception {
        mockMvc.perform(get("/api/favoritos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(favoritoAId.intValue())));
    }

    @Test
    @DisplayName("eliminar_favoritoEnListaAjena_retornaForbidden")
    void eliminar_favoritoEnListaAjena_retornaForbidden() throws Exception {
        mockMvc.perform(delete("/api/favoritos/{id}", favoritoAId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("eliminar_favoritoEnListaPropia_retornaNoContent")
    void eliminar_favoritoEnListaPropia_retornaNoContent() throws Exception {
        mockMvc.perform(delete("/api/favoritos/{id}", favoritoAId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isNoContent());
    }
}
