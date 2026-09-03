package app.tritonwatch.user_service.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String READ_USER_PROFILE = "SCOPE_read:user-profile";
    private static final String UPDATE_USER_PROFILE = "SCOPE_update:user-profile";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/me").hasAuthority(READ_USER_PROFILE)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/me", "/api/v1/me/notification-preferences")
                        .hasAuthority(UPDATE_USER_PROFILE)
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/me/email/verification-requests",
                                "/api/v1/me/email/verifications",
                                "/api/v1/me/phone/verification-requests",
                                "/api/v1/me/phone/verifications"
                        ).hasAuthority(UPDATE_USER_PROFILE)
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/me").hasAuthority(UPDATE_USER_PROFILE)
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()))
                .build();
    }

    @Bean
    NimbusJwtDecoder jwtDecoder(
            @Value("${auth0.issuer}") String configuredIssuer,
            @Value("${auth0.audience}") String audience
    ) {
        String issuer = configuredIssuer.endsWith("/") ? configuredIssuer : configuredIssuer + "/";
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri(issuer + ".well-known/jwks.json")
                .build();

        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>(
                "aud",
                audiences -> audiences != null && audiences.contains(audience)
        );

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));
        return decoder;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${auth0.allowed-origins}") String configuredOrigins
    ) {
        List<String> origins = Arrays.stream(configuredOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "PUT", "POST", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
