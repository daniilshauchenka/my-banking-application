package com.example.accountservice;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.security", name = "enabled", havingValue = "true", matchIfMissing = true)
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService
    ) {
        OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();
        AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                        clientRegistrationRepository,
                        authorizedClientService
                );
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
        return authorizedClientManager;
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.security", name = "enabled", havingValue = "true", matchIfMissing = true)
    @Qualifier("notificationServiceRestClient")
    public RestClient notificationServiceRestClient(
            OAuth2AuthorizedClientManager authorizedClientManager,
            @Value("${notification-service.url}") String notificationServiceUrl
    ) {
        OAuth2ClientHttpRequestInterceptor interceptor = new OAuth2ClientHttpRequestInterceptor(authorizedClientManager);
        interceptor.setClientRegistrationIdResolver(request -> "account-service");
        interceptor.setPrincipalResolver(request -> new UsernamePasswordAuthenticationToken(
                "account-service",
                "N/A",
                AuthorityUtils.createAuthorityList("ROLE_SERVICE")
        ));

        return RestClient.builder()
                .baseUrl(notificationServiceUrl)
                .requestInterceptor(interceptor)
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.security", name = "enabled", havingValue = "false")
    @Qualifier("notificationServiceRestClient")
    public RestClient insecureNotificationServiceRestClient(@Value("${notification-service.url}") String notificationServiceUrl) {
        return RestClient.builder()
                .baseUrl(notificationServiceUrl)
                .build();
    }
}
