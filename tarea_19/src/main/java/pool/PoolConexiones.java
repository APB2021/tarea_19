package pool;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.util.Properties;

public class PoolConexiones {

	private static HikariDataSource dataSource;

	static {
		try {
			// Carga las propiedades desde el archivo
			Properties properties = new Properties();

			try (FileInputStream fis = new FileInputStream("src\\main\\resources\\db.properties")) {
				properties.load(fis);
			}

			// Configura HikariCP con las propiedades
			HikariConfig config = new HikariConfig();
			config.setJdbcUrl(properties.getProperty("db.url"));
			config.setUsername(properties.getProperty("db.user"));
			config.setPassword(properties.getProperty("db.password"));
			config.setMaximumPoolSize(Integer.parseInt(properties.getProperty("db.maximumPoolSize")));
			config.setMinimumIdle(Integer.parseInt(properties.getProperty("db.minimumIdle")));
			config.setConnectionTimeout(Long.parseLong(properties.getProperty("db.connectionTimeout")));
			config.setIdleTimeout(Long.parseLong(properties.getProperty("db.idleTimeout")));
			config.setMaxLifetime(Long.parseLong(properties.getProperty("db.maxLifetime")));

			// Inicializa el pool de conexiones
			dataSource = new HikariDataSource(config);

		} catch (IOException | NumberFormatException e) {
			e.printStackTrace();
			throw new RuntimeException("Error al configurar el pool de conexiones", e);
		}
	}

	/**
	 * Obtiene una conexión desde el pool.
	 *
	 * @return Connection
	 * @throws SQLException si ocurre un error al obtener la conexión.
	 */
	public static Connection getConnection() throws java.sql.SQLException {
		return dataSource.getConnection();
	}

	/**
	 * Cierra el pool de conexiones.
	 */
	public static void cerrarPool() {
		if (dataSource != null && !dataSource.isClosed()) {
			dataSource.close();
		}
	}
}
