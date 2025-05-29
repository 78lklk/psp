package server.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;


public class DatabaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    private static final String PROPS_FILE = "config.properties";

    private static HikariDataSource dataSource;

    static {
        initDataSource();
    }

    private static void initDataSource() {
        Properties props = new Properties();

        // 1. Загружаем файл db.properties из classpath
        try (InputStream in = DatabaseConfig.class.getClassLoader().getResourceAsStream(PROPS_FILE)) {
            if (in == null) {
                throw new IllegalStateException("Файл '" + PROPS_FILE + "' не найден в classpath");
            }
            props.load(in);
        } catch (IOException e) {
            logger.error("Ошибка при загрузке файла настроек БД: {}", PROPS_FILE, e);
            throw new RuntimeException("Не удалось загрузить настройки БД", e);
        }

        // 2. Читаем обязательные параметры
        String url      = props.getProperty("db.url");
        String user     = props.getProperty("db.user");
        String password = props.getProperty("db.password");

        if (url == null || user == null || password == null) {
            throw new IllegalStateException(
                    "В файле '" + PROPS_FILE + "' должны быть заданы свойства: db.url, db.user, db.password"
            );
        }

        // 3. Настраиваем HikariCP
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(user);
            config.setPassword(password);

            // дополнительные параметры пула
            config.setAutoCommit(true);
            config.setMinimumIdle(2);
            config.setMaximumPoolSize(10);
            config.setIdleTimeout(30_000);
            config.setPoolName("LoyaltySystemPool");

            // кеширование PreparedStatement
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(config);
            logger.info("HikariCP успешно инициализирована (URL={})", url);
        } catch (Exception e) {
            logger.error("Ошибка инициализации пула соединений HikariCP", e);
            throw new RuntimeException("Не удалось инициализировать пул соединений", e);
        }
    }


    public static DataSource getDataSource() {
        return dataSource;
    }


    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }


    public static void closeDataSource() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Пул соединений HikariCP закрыт");
        }
    }
}