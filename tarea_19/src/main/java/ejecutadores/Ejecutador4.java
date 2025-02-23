package ejecutadores;

import controlador.Controlador;
import modelo.AlumnosDAO;
import vista.IVista;
import vista.VistaConsola;

public class Ejecutador4 {

	public static void main(String[] args) {

		IVista vista = new VistaConsola();
		AlumnosDAO modelo = vista.elegirModelo();
		new Controlador().ejecutar(modelo, vista);
	}
}