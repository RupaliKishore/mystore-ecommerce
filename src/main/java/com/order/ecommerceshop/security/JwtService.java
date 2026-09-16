package com.order.ecommerceshop.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService
{
    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration}")
    private Long expiration;

    private SecretKey getSigningKey()
    {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }


    // create token
    public String generateToken(String email, String role)
    {
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey()) // jjwt auto-selects HS256/384/512
                .compact();
    }


    // get email in token
    public String extractEmail(String token)
    {
        return extractClaim(token, Claims::getSubject);
    }


    public String extractRole(String token)
    {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }


    // check token is valid
    public boolean isTokenValid(String token , String email)
    {
        try {
               String tokenEmail = extractEmail(token);
                return tokenEmail.equals(email) && !isTokenExpired(token);
            }
            catch(Exception ex)
            {
                return false;
            }

    }

    private boolean isTokenExpired(String token)
    {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }


    private <T> T extractClaim(String token, Function<Claims, T> resolver)
    {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return resolver.apply(claims);
    }




}
