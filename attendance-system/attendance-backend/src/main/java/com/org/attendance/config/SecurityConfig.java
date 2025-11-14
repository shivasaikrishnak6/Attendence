package com.org.attendance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(request -> {
                var c = new org.springframework.web.cors.CorsConfiguration();
                // Use patterns when allowCredentials(true)
                c.setAllowedOriginPatterns(java.util.List.of("*"));
                c.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                c.setAllowedHeaders(java.util.List.of("Authorization", "Content-Type"));
                c.setAllowCredentials(true);
                return c;
            }))
            .authorizeHttpRequests(auth -> auth
                // static pages
                .requestMatchers("/", "/index.html", "/user.html", "/admin.html", "/css/**", "/js/**").permitAll()
                // public auth endpoints (index.html uses this for role routing, but requires credentials)
                .requestMatchers(HttpMethod.GET, "/auth/check").authenticated()
                // admin-only APIs
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // your other APIs require login
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        var admin = User.withUsername("admin").password(encoder.encode("admin123")).roles("ADMIN").build();
        var user  = User.withUsername("user").password(encoder.encode("user123")).roles("USER").build();
        return new InMemoryUserDetailsManager(admin, user);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

