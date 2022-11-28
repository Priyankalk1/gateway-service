package com.maveric.gatewayservice.util;

import com.maveric.gatewayservice.dto.GateWayResponseDto;
import com.maveric.gatewayservice.filter.JwtAuthFilter;
import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtAuthUtil {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(JwtAuthFilter.class);
    @Value("${jwt.secret}")
    private String secretKey;

    private Claims extractAllClaims(String token) {
        return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody();
    }

    public GateWayResponseDto validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
            return new GateWayResponseDto(true,extractAllClaims(token));
        } catch (SignatureException | MalformedJwtException | ExpiredJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            log.error("ValidateToken Exception ->{}",e.getMessage());
        }
        return new GateWayResponseDto(false,null);
    }
}
