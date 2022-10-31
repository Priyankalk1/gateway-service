package util;

import com.maveric.gatewayservice.dto.GateWayResponseDto;
import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtAuthUtil {
    @Value("${jwt.secret}")
    private String SECRET_KEY;

    private Claims extractAllClaims(String token) {
        return Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).getBody();
    }

    public GateWayResponseDto validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token);
            GateWayResponseDto gateWayResponseDto = new GateWayResponseDto(true,extractAllClaims(token));
            return gateWayResponseDto;
        } catch (SignatureException e) {
            System.out.println("Invalid JWT signature trace: {}"+ e);
        } catch (MalformedJwtException e) {
            System.out.println("Invalid JWT token trace: {}"+ e);
        } catch (ExpiredJwtException e) {
            System.out.println("Expired JWT token trace: {}"+ e);
        } catch (UnsupportedJwtException e) {
            System.out.println("Unsupported JWT token trace: {}"+ e);
        } catch (IllegalArgumentException e) {
            System.out.println("JWT token compact of handler are invalid trace: {}"+e);
        }
        return new GateWayResponseDto(false,null);
    }
}
