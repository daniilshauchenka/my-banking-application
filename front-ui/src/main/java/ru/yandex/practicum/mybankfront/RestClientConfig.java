package ru.yandex.practicum.mybankfront;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.security", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RestClient.Builder restClientBuilder(OAuth2AuthorizedClientService authorizedClientService) {
        return RestClient.builder()
                .requestInterceptor((request, body, execution) -> {
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
                        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                                oauth2Token.getAuthorizedClientRegistrationId(),
                                oauth2Token.getName()
                        );
                        if (client != null && client.getAccessToken() != null) {
                            request.getHeaders().set(HttpHeaders.AUTHORIZATION,
                                    "Bearer " + client.getAccessToken().getTokenValue());
                        }
                    }

                    return execution.execute(request, body);
                });
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.security", name = "enabled", havingValue = "false")
    public RestClient.Builder insecureRestClientBuilder() {
        return RestClient.builder();
    }
}
