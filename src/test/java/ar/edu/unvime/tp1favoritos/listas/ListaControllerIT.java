package ar.edu.unvime.tp1favoritos.listas;

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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ListaControllerIT extends BaseIntegrationTest {

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
    private String tokenAdmin;

    private Long listaAId;

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
        tokenAdmin = jwtService.generarToken(usuarioAdmin.id(), usuarioAdmin.email(), usuarioAdmin.rol());

        // 3. Insertar lista perteneciente a Usuario A directamente
        listaAId = jdbcTemplate.queryForObject(
                "INSERT INTO listas (nombre, usuario_id) VALUES ('Lista Privada de A', ?) RETURNING id",
                Long.class,
                usuarioA.id()
        );
    }

    @Test
    @DisplayName("login_credencialesValidas_retornaOkConToken")
    void login_credencialesValidas_retornaOkConToken() throws Exception {
        String loginJson = """
                {
                    "email": "usuarioA@unvime.edu.ar",
                    "password": "passwordA123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.usuario.email", is("usuarioA@unvime.edu.ar")));
    }

    @Test
    @DisplayName("login_credencialesInvalidas_retornaUnauthorized")
    void login_credencialesInvalidas_retornaUnauthorized() throws Exception {
        String loginJson = """
                {
                    "email": "usuarioA@unvime.edu.ar",
                    "password": "passwordIncorrecto"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("obtenerLista_listaPropia_retornaOk")
    void obtenerLista_listaPropia_retornaOk() throws Exception {
        mockMvc.perform(get("/api/listas/{id}", listaAId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(listaAId.intValue())))
                .andExpect(jsonPath("$.nombre", is("Lista Privada de A")))
                .andExpect(jsonPath("$.usuarioId", is(usuarioA.id().intValue())));
    }

    @Test
    @DisplayName("obtenerLista_listaAjena_retornaForbidden")
    void obtenerLista_listaAjena_retornaForbidden() throws Exception {
        // Token del Usuario B intentando acceder a la lista de Usuario A
        String tokenB = jwtService.generarToken(usuarioB.id(), usuarioB.email(), usuarioB.rol());

        mockMvc.perform(get("/api/listas/{id}", listaAId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("listar_sinToken_retornaUnauthorized")
    void listar_sinToken_retornaUnauthorized() throws Exception {
        mockMvc.perform(get("/api/listas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("obtenerLista_listaAjenaConRolAdmin_retornaOk")
    void obtenerLista_listaAjenaConRolAdmin_retornaOk() throws Exception {
        mockMvc.perform(get("/api/listas/{id}", listaAId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(listaAId.intValue())))
                .andExpect(jsonPath("$.nombre", is("Lista Privada de A")));
    }
}
