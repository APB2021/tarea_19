package logs;

import java.io.InputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

public class InicializarLogs {

	private static final Logger logger = LogManager.getLogger(InicializarLogs.class);

	public static void inicializarLogs() {

		InputStream configStream = InicializarLogs.class.getClassLoader().getResourceAsStream("log4j2.xml");
		if (configStream == null) {
			System.out.println("No se pudo encontrar el archivo log4j2.xml en la ubicación correcta");
		} else {
			System.out.println("Archivo log4j2.xml encontrado, inicializando logs...");
			Configurator.initialize(null);
		}

		// Configuración de Log4j2
		Configurator.initialize(null, "log4j2.xml");
		logger.info("Logs inicializados correctamente");
	}
}