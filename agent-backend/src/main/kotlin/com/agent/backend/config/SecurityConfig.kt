package com.agent.backend.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class SecurityConfig {

    /**
     * Maps Keycloak realm roles (realm_access.roles) to Spring Security GrantedAuthority
     * so that hasRole("ADMIN") and similar expressions work correctly.
     */
    @Bean
    fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        val converter = JwtAuthenticationConverter()
        converter.setJwtGrantedAuthoritiesConverter { jwt ->
            @Suppress("UNCHECKED_CAST")
            val realmRoles = (jwt.claims["realm_access"] as? Map<String, Any>)
                ?.get("roles") as? List<String>
                ?: emptyList()
            realmRoles.map { role -> SimpleGrantedAuthority("ROLE_$role") }
        }
        return converter
    }

    @Bean
    fun filterChain(http: HttpSecurity, jwtAuthenticationConverter: JwtAuthenticationConverter): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { }
            .authorizeHttpRequests { auth ->
                auth
                    // Allow CORS preflight requests
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    // Public endpoints: login, logout and refresh
                    .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                    .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
                    .requestMatchers(HttpMethod.POST, "/auth/logout").permitAll()
                    .requestMatchers(HttpMethod.POST, "/auth/refresh").permitAll()
                    .requestMatchers(HttpMethod.POST, "/auth/verify-email").permitAll()
                    .requestMatchers(HttpMethod.POST, "/auth/resend-verification").permitAll()
                    .requestMatchers(HttpMethod.POST, "/newsletter/subscribe").permitAll()
                    .requestMatchers(HttpMethod.GET, "/newsletter/unsubscribe/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/newsletter/confirm/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/newsletter/resend-verification").permitAll()
                    .requestMatchers("/actuator/**").permitAll()
                    // WebSocket endpoints (authentication handled at STOMP level)
                    .requestMatchers("/ws/**").permitAll()
                    // Admin-only endpoints: require ADMIN role (enforced via Keycloak realm roles)
                    .requestMatchers("/newsletter/admin/**").hasRole("ADMIN")
                    // Everything else requires a valid JWT
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)
                }
            }
        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            addAllowedOriginPattern("*")
            addAllowedHeader("*")
            addAllowedMethod("*")
            allowCredentials = true
        }
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
