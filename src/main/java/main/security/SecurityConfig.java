package main.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz
                       .requestMatchers("/api/users/register").permitAll()
                        .requestMatchers("/api/users/register-admin").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/index.html").permitAll()
                        .requestMatchers("/inscription/**").permitAll()
                        .requestMatchers("/connection/**").permitAll()


                        .requestMatchers("/menu/**").permitAll()
                        .requestMatchers("/data/**").permitAll()

                        .requestMatchers("/playlist/**").permitAll()



                        .requestMatchers("/textfiles/**").permitAll()
                        .requestMatchers("/photos/**").permitAll()
                        .requestMatchers("/api/users/**").permitAll()
                        .requestMatchers("/api/messages/**").permitAll()
                        .requestMatchers("/menu/**").permitAll()
                        .requestMatchers("/api/users/all").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()

               



                        // ⚠️ AJOUTE CETTE LIGNE :
                        .requestMatchers("/api/conversations/**").authenticated()

                        .requestMatchers("/api/users/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers.frameOptions(frame -> frame.disable()));

        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(
                                "https://security-learning-1.onrender.com",
                                "http://localhost:3000",
                                "http://localhost:8080",
                                "http://127.0.0.1:8080"
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }






}