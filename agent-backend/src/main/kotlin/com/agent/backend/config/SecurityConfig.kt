package com.agent.backend.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class SecurityConfig {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
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
                    .requestMatchers(HttpMethod.POST, "/newsletter/unsubscribe").permitAll()
                    .requestMatchers(HttpMethod.GET, "/newsletter/unsubscribe/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/newsletter/confirm/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/newsletter/resend-verification").permitAll()
                    .requestMatchers("/actuator/**").permitAll()
                    // WebSocket endpoints (authentication handled at STOMP level)
                    .requestMatchers("/ws/**").permitAll()
                    // Everything else requires a valid JWT
                    .anyRequest().authenticated()           }
            .oauth2ResourceServer { it.jwt(Customizer.withDefaults()) }
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
