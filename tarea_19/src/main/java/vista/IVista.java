package vista;

import modelo.AlumnosDAO;

public interface IVista {

	public void mostrarMenu(AlumnosDAO modelo);

	// case 1:
	public void insertarNuevoAlumno(AlumnosDAO modelo);

	// case 2:
	public void insertarNuevoGrupo(AlumnosDAO modelo);

	// cases 3 y 12:
	public void mostrarTodosLosAlumnos(AlumnosDAO modelo, boolean mostrarTodaLaInformacion);

	// case 4:
	public void guardarAlumnosEnFicheroTexto(AlumnosDAO modelo);

	// case 5:
	public void leerAlumnosDesdeFichero(AlumnosDAO modelo);

	// case 6:
	public void modificarNombreAlumnoPorNia(AlumnosDAO modelo);

	// case 7:
	public void eliminarAlumnoPorNIA(AlumnosDAO modelo);

	// case 8:
	public void eliminarAlumnosPorGrupo(AlumnosDAO modelo);

	// case 9:
	public void guardarGruposEnXML(AlumnosDAO modelo);

	// case 10:
	public void leerYGuardarGruposXML(AlumnosDAO modelo);

	// case 11:
	public void mostrarAlumnosPorGrupo(AlumnosDAO modelo);

	// case 13:
	public void cambiarGrupoAlumno(AlumnosDAO modelo);

	// case 14:
	public void guardarGrupoEspecificoEnXML(AlumnosDAO modelo);

	// Para elegir la BD antes de mostrar eel menu
	public AlumnosDAO elegirModelo();
}