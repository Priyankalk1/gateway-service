package com.maveric.gatewayservice.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maveric.gatewayservice.dto.ErrorDto;
import com.maveric.gatewayservice.dto.GateWayResponseDto;
import com.maveric.gatewayservice.util.JwtAuthUtil;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.*;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


@RefreshScope
@Component
public class JwtAuthFilter implements GatewayFilter {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(JwtAuthFilter.class);
    @Autowired
    private RouterValidator routerValidator;


    @Autowired
    private JwtAuthUtil jwtAuthUtil;

    @Autowired
    ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        log.info("API gateway passing through filter");
        if (routerValidator.isSecured.test(request)) {
            if (this.isAuthMissing(request))
                return this.onError(exchange, "Authorization header is missing in request", HttpStatus.UNAUTHORIZED);

            final String raw_token = this.getAuthHeader(request);
            String token = raw_token.substring(7);

            try {
                GateWayResponseDto gateWayResponseDto = validateToken(token);
                if (!gateWayResponseDto.isResponse())
                    return this.onError(exchange, "Authorization header is invalid", HttpStatus.UNAUTHORIZED);

                this.populateRequestWithHeaders(exchange, gateWayResponseDto.getClaims());
            }
            catch(Exception e)
            {
                log.error("Exception in validating the token");
                return this.onError(exchange, "Exception in validating the token", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return chain.filter(exchange);
    }


    /*PRIVATE*/

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        DataBufferFactory dataBufferFactory = exchange.getResponse().bufferFactory();
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        try {
            response.getHeaders().add("Content-Type", "application/json");
            ErrorDto errorDto = new ErrorDto(String.valueOf(httpStatus.value()), err);
            byte[] byteData = objectMapper.writeValueAsBytes(errorDto);
            log.error("Filter blockage ->{}",errorDto.getMessage());
            return response.writeWith(Mono.just(byteData).map(dataBufferFactory::wrap));

        } catch (Exception e) {
            log.error("Unexpected error-{}",e.getMessage());

        }
        return response.setComplete();
    }

    private String getAuthHeader(ServerHttpRequest request) {
        return request.getHeaders().getOrEmpty("Authorization").get(0);
    }

    private boolean isAuthMissing(ServerHttpRequest request) {
        return !request.getHeaders().containsKey("Authorization");
    }

    private void populateRequestWithHeaders(ServerWebExchange exchange, Claims claims) {
        exchange.getRequest().mutate()
                .header("userEmail", String.valueOf(claims.get("sub")))
                .header("userId", String.valueOf(claims.get("jti")))
                .build();
    }

    private GateWayResponseDto validateToken(String token) {
        try {
            return jwtAuthUtil.validateToken(token);
        }
        catch (Exception e)
        {
            return new GateWayResponseDto();
        }

    }


}