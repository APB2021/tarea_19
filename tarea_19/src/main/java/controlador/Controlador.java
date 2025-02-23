package controlador;

import logs.InicializarLogs;
import modelo.AlumnosDAO;
import vista.IVista;

public class Controlador {

	public void ejecutar(AlumnosDAO modelo, IVista vista) {

		// Inicializar los logs
		InicializarLogs.inicializarLogs();

		// Llamar a la vista que contiene el menú principal en consola
		vista.mostrarMenu(modelo);
	}
}