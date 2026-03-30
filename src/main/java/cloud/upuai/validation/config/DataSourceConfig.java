package cloud.upuai.validation.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Bean
    public DataSource dataSource() {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            throw new IllegalStateException("DATABASE_URL environment variable is required");
        }

        // Parse postgresql://user:pass@host:port/db
        String httpUrl = databaseUrl.replace("postgresql://", "http://").replace("postgres://", "http://");
        URI uri = URI.create(httpUrl);

        String host = uri.getHost();
        int port = uri.getPort() > 0 ? uri.getPort() : 5432;
        String dbName = uri.getPath().substring(1);
        String userInfo = uri.getUserInfo();
        String user = userInfo.split(":")[0];
        String pass = userInfo.contains(":") ? userInfo.substring(userInfo.indexOf(':') + 1) : "";

        String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s?sslmode=require", host, port, dbName);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(user);
        config.setPassword(pass);
        config.setMaximumPoolSize(5);
        config.setConnectionTimeout(10_000);

        log.info("Connecting to PostgreSQL at {}:{}/{}", host, port, dbName);
        return new HikariDataSource(config);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
