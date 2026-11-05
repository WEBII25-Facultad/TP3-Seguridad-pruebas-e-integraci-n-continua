package ar.edu.unvime.tp1favoritos.usuarios;

import ar.edu.unvime.tp1favoritos.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIT extends BaseIntegrationTest {

    @Test
    @DisplayName("registro_datosValidos_retornaCreated")
    void registro_datosValidos_retornaCreated() throws Exception {
        String json = """
                {
                    "email": "nuevo.usuario@unvime.edu.ar",
                    "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.email", is("nuevo.usuario@unvime.edu.ar")))
                .andExpect(jsonPath("$.rol", is("USER")));
    }

    @Test
    @DisplayName("registro_emailRepetido_retornaConflict")
    void registro_emailRepetido_retornaConflict() throws Exception {
        String json = """
                {
                    "email": "repetido@unvime.edu.ar",
                    "password": "password123"
                }
                """;

        // Primer registro exitoso
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        // Segundo registro con el mismo email -> 409
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.title", is("Conflicto de registro")));
    }

    @Test
    @DisplayName("registro_emailInvalido_retornaBadRequest")
    void registro_emailInvalido_retornaBadRequest() throws Exception {
        String json = """
                {
                    "email": "no-es-un-email-valido",
                    "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.title", is("Error de validación")));
    }

    @Test
    @DisplayName("registro_passwordInvalido_retornaBadRequest")
    void registro_passwordInvalido_retornaBadRequest() throws Exception {
        String json = """
                {
                    "email": "valido@unvime.edu.ar",
                    "password": "123"
                }
                """;

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.title", is("Error de validación")));
    }
}
