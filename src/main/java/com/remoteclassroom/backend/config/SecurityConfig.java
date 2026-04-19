package com.remoteclassroom.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())

                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                .authorizeHttpRequests(auth -> auth
                        // ✅ Auth routes — always public
                        .requestMatchers("/api/auth/**").permitAll()

                        // ✅ Transcription — public
                        .requestMatchers("/api/transcribe/**").permitAll()

                        // ✅ Error page — public
                        .requestMatchers("/error").permitAll()

                        // ✅ Bug Fix #2: Video listing — public (no login required)
                        .requestMatchers(HttpMethod.GET, "/api/video/all").permitAll()

                        // 🔒 Batch management
                        .requestMatchers("/api/batch/create").hasRole("TEACHER")
                        .requestMatchers("/api/batch/join").hasRole("STUDENT")
                        .requestMatchers("/api/batch/my").authenticated()

                        // 🔒 Video management
                        .requestMatchers(HttpMethod.POST, "/api/video/**").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.GET, "/api/video/batch/**").authenticated()

                        // 🔒 Quiz management
                        .requestMatchers("/api/quiz/generate").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.GET, "/api/quiz/**").authenticated()
                        .requestMatchers("/api/quiz/submit").hasRole("STUDENT")

                        // 🔒 Live Class management
                        .requestMatchers("/api/live/create", "/api/live/start", "/api/live/end").hasRole("TEACHER")
                        .requestMatchers("/api/live/status/**").authenticated()
                        .requestMatchers("/api/ai/doubt").authenticated()

                        // 🔒 Other existing routes
                        .requestMatchers("/api/student/**").hasRole("STUDENT")
                        .requestMatchers("/api/teacher/**").hasRole("TEACHER")
                        .requestMatchers("/api/user/**").authenticated()
                        .requestMatchers("/api/video/download/**").authenticated()

                        // 🔒 Everything else
                        .anyRequest().authenticated())

                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
