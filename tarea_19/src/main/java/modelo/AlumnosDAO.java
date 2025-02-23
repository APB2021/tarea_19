package modelo;

public interface AlumnosDAO {

	// ALUMNOS:

	public boolean insertarAlumno(Alumno alumno);

	public Alumno solicitarDatosAlumno();

	public boolean mostrarTodosLosAlumnos(boolean mostrarTodaLaInformacion);

	public boolean modificarNombreAlumnoPorNIA(int nia, String nuevoNombre);

	public boolean eliminarAlumnoPorNIA(int nia);

	boolean mostrarAlumnoPorNIA(int nia);

	// FICHEROS:

	public void guardarAlumnosEnFicheroTexto();

	public boolean leerAlumnosDeFicheroTexto();

	// GRUPOS:

	public boolean insertarGrupo(Grupo grupo);

	public boolean eliminarAlumnosPorGrupo(String grupo);

	boolean mostrarTodosLosGrupos();

	boolean guardarGruposEnXML();

	boolean leerYGuardarGruposXML(String rutaArchivo);

	void mostrarAlumnosPorGrupo();

	boolean cambiarGrupoAlumno();

	boolean guardarGrupoEspecificoEnXML();
}