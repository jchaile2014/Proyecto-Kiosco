package org.kiosco.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * La app corre solo en la PC (escucha en 127.0.0.1), así que no pide usuario.
 * Spring Security queda activo igual por la protección CSRF: evita que una página
 * web cualquiera abierta en el navegador pueda cargar o borrar movimientos.
 */
@Configuration
public class SeguridadConfig {

    @Bean
    SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                // El token CSRF va en una cookie y no en la sesión: la pantalla queda abierta
                // todo el día y tiene que seguir funcionando aunque la app se reinicie.
                .csrf(csrf -> csrf.csrfTokenRepository(new CookieCsrfTokenRepository()));
        return http.build();
    }

    /** Sin usuarios: evita que Spring genere uno con contraseña aleatoria al arrancar. */
    @Bean
    UserDetailsService sinUsuarios() {
        return new InMemoryUserDetailsManager();
    }
}
