package com.url.url_shortner.secuirity.jwt;

import com.url.url_shortner.service.UserDetailsImpl;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
@Component
public class JwtUtils {
    @Value("${jwt.secret}")
    private String jwtSecret;
    @Value("${jwt.expiration}")
    private int jwtExpirationMs;
    //Authorization -> Bearer <token>
    public String getJwtFromHeader(HttpServletRequest request){
        String bearerToken= request.getHeader("Authorization");
        if(bearerToken != null && bearerToken.startsWith("Bearer ")){
            return bearerToken.substring(7);
        }
        return null;
    }
    //Token generation based on username and role
    public String generateToken(UserDetailsImpl userDetails){
        String username=userDetails.getUsername();
        String roles=userDetails.getAuthorities().stream().map(auth -> auth.getAuthority()).reduce((a,b)->a+","+b).orElse("");
        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date((new Date().getTime() + jwtExpirationMs)))
                .signWith(key())
                .compact();
    }
    public String getUsernameFromJWTToken(String token){
        return Jwts.parser().verifyWith((SecretKey) key()).build().parseSignedClaims(token).getPayload().getSubject();
    }
    private Key key(){
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }
    public boolean validateToken(String authToken){
        try {
            Jwts.parser().verifyWith((SecretKey) key()).build().parseSignedClaims(authToken);
            return true;
        } catch (JwtException e) {
            throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
