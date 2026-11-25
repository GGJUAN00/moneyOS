package com.moneyos.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.net.URI;

/**
 * En internet (Render, Neon…) la base de datos llega como una sola variable:
 *   DATABASE_URL = postgres://usuario:clave@servidor:5432/basedatos
 * Esta clase la convierte al formato que entiende Java (JDBC).
 * En el computador no existe esa variable y se usa la base H2 en archivo.
 */
@Configuration
@ConditionalOnExpression("!'${DATABASE_URL:}'.isEmpty()")
public class BaseDeDatosConfig {

    @Bean
    public DataSource dataSource() {
        String url = System.getenv("DATABASE_URL");
        HikariConfig config = new HikariConfig();
        config.setMaximumPoolSize(5);

        if (url.startsWith("jdbc:")) {
            config.setJdbcUrl(url);
        } else {
            URI uri = URI.create(url.replaceFirst("^postgres(ql)?://", "postgresql://"));
            String[] credenciales = uri.getUserInfo() == null ? new String[]{"", ""} : uri.getUserInfo().split(":", 2);
            int puerto = uri.getPort() > 0 ? uri.getPort() : 5432;
            String jdbc = "jdbc:postgresql://" + uri.getHost() + ":" + puerto + uri.getPath()
                + (uri.getQuery() == null ? "" : "?" + uri.getQuery());
            config.setJdbcUrl(jdbc);
            config.setUsername(java.net.URLDecoder.decode(credenciales[0], java.nio.charset.StandardCharsets.UTF_8));
            config.setPassword(credenciales.length > 1
                ? java.net.URLDecoder.decode(credenciales[1], java.nio.charset.StandardCharsets.UTF_8) : "");
        }
        return new HikariDataSource(config);
    }
}
