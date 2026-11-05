package ar.edu.unvime.tp1favoritos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.flyway.enabled=false",
        "jwt.secret=clave-secreta-de-prueba-para-contextLoads-minimo-256-bits-ok",
        "jwt.expiration=86400000"
})
class Tp1FavoritosApplicationTests {

    @MockitoBean
    private DataSource dataSource;

    @Test
    void contextLoads() {
    }

}
