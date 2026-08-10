package com.example.accountservice.config;

import org.springframework.http.HttpMethod;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.Set;
import java.util.function.Supplier;

@Configuration
@ConditionalOnProperty(prefix = "app.security", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/accounts/*/deposit", "/accounts/*/withdraw")
                        .access(serviceClientAccess())
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }

    private AuthorizationManager<RequestAuthorizationContext> serviceClientAccess() {
        Set<String> allowedClients = Set.of("cash-service", "transfer-service");
        return new AuthorizationManager<>() {
            @Override
            public AuthorizationDecision authorize(Supplier<? extends Authentication> authentication, RequestAuthorizationContext context) {
                Authentication current = authentication.get();
                if (current == null || !current.isAuthenticated() || !(current.getPrincipal() instanceof Jwt jwt)) {
                    return new AuthorizationDecision(false);
                }

                String clientId = jwt.getClaimAsString("azp");
                if (clientId == null) {
                    clientId = jwt.getClaimAsString("client_id");
                }
                return new AuthorizationDecision(allowedClients.contains(clientId));
            }
        };
    }
}
