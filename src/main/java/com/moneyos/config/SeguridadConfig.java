package com.moneyos.config;

import com.moneyos.seguridad.FiltroBloqueoLogin;
import com.moneyos.seguridad.ManejadorLoginExitoso;
import com.moneyos.seguridad.ManejadorLoginFallido;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Reglas de acceso por rol (qué "puertas" abre cada gafete).
 */
@Configuration
public class SeguridadConfig {

    @Bean
    public PasswordEncoder cifrador() {
        // Las contraseñas se guardan cifradas con BCrypt, nunca como texto.
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain reglas(HttpSecurity http, ManejadorLoginExitoso exito, ManejadorLoginFallido fallo,
                                      FiltroBloqueoLogin bloqueo) throws Exception {
        http
            .authorizeHttpRequests(acceso -> acceso
                // Pantallas públicas
                .requestMatchers("/login", "/registro", "/salud", "/error",
                    "/css/**", "/js/**", "/img/**", "/favicon.ico", "/manifest.webmanifest").permitAll()
                // Gestión de Usuarios: SOLO ADMIN
                .requestMatchers("/panel/usuarios/**").hasRole("ADMIN")
                // Registrar o borrar movimientos y cambiar el presupuesto: ADMIN y OPERADOR
                .requestMatchers(HttpMethod.POST, "/panel/movimientos/**", "/panel/presupuesto").hasAnyRole("ADMIN", "OPERADOR")
                // Lo demás exige haber iniciado sesión
                .anyRequest().authenticated())
            .formLogin(login -> login
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(exito)
                .failureHandler(fallo)
                .permitAll())
            .logout(salir -> salir
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?salir")
                .permitAll())
            .exceptionHandling(e -> e.accessDeniedHandler((request, response, ex) ->
                response.sendRedirect(request.getContextPath() + "/panel?denegado")))
            .addFilterBefore(bloqueo, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** El filtro de bloqueo solo debe correr dentro de Spring Security (no dos veces). */
    @Bean
    public FilterRegistrationBean<FiltroBloqueoLogin> noRegistrarDosVeces(FiltroBloqueoLogin filtro) {
        FilterRegistrationBean<FiltroBloqueoLogin> registro = new FilterRegistrationBean<>(filtro);
        registro.setEnabled(false);
        return registro;
    }
}
