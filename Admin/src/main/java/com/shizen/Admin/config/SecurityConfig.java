package com.shizen.Admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration 
public class SecurityConfig { 

    // Define BCrypt como el algoritmo de contraseñas.
    // Spring Security automáticamente lo usa para verificar contra tu UserDetailsServiceImpl.
    @Bean 
    public PasswordEncoder passwordEncoder() { 
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Rutas públicas accesibles sin iniciar sesión
                .requestMatchers("/", "/login", "/*.css", "/assets/**", "/static/**", "/webjars/**", "/ws/**", "/js/**").permitAll()
                // Cualquier otra pantalla del panel requiere inicio de sesión
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/")                        // La URL donde está el formulario
                .loginProcessingUrl("/login")          // El endpoint POST donde se envían los datos
                .usernameParameter("username")         // Campo name="username" del formulario
                .passwordParameter("password")         // Campo name="password" del formulario
                .defaultSuccessUrl("/dashboard", true) // A dónde va si el login es correcto
                .failureUrl("/?error=true")            // A dónde vuelve si falla
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/?logout=true")
                .permitAll()
            );        

        return http.build();
    }
}