package ar.edu.unvime.tp1favoritos.usuarios.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

    private final Key signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generarToken(Long usuarioId, String email, String rol) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(email)
                .claim("userId", usuarioId)
                .claim("rol", rol)
                .setIssuedAt(ahora)
                .setExpiration(expiracion)
                .signWith(signingKey)
                .compact();
    }

    public Claims extraerClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extraerEmail(String token) {
        return extraerClaims(token).getSubject();
    }

    public Long extraerUsuarioId(String token) {
        Object userIdClaim = extraerClaims(token).get("userId");
        if (userIdClaim instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(userIdClaim.toString());
    }

    public String extraerRol(String token) {
        return (String) extraerClaims(token).get("rol");
    }

    public boolean esTokenValido(String token) {
        try {
            Claims claims = extraerClaims(token);
            return claims.getExpiration() == null || claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
