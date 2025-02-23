package vista;

import java.io.File;
import java.util.InputMismatchException;
import java.util.Scanner;

import modelo.Alumno;
import modelo.AlumnosDAO;
import modelo.AlumnosHibernate;
import modelo.BaseDatos;
import modelo.Grupo;

public class VistaConsola implements IVista {

	private final Scanner sc = new Scanner(System.in);

	private AlumnosDAO modelo;

	public AlumnosDAO elegirModelo() {
		seleccionarBaseDatos();
		return modelo;
	}

	private void seleccionarBaseDatos() {
		System.out.println("""
				---- Selección de Base de Datos ----
				1. MySQL (Hibernate)
				2. Oracle (Hibernate)
				-----------------------------------
				""");

		int opcion;
		do {
			System.out.print("Selecciona la base de datos con la que deseas trabajar: ");
			try {
				opcion = sc.nextInt();
				sc.nextLine(); // Limpiar buffer

				switch (opcion) {
				case 1 -> {
					System.out.println("✅ Conectando a MySQL...");
					AlumnosHibernate.setBaseDatos(BaseDatos.MYSQL);
					modelo = new AlumnosHibernate();
				}
				case 2 -> {
					System.out.println("✅ Conectando a Oracle...");
					AlumnosHibernate.setBaseDatos(BaseDatos.ORACLE);
					modelo = new AlumnosHibernate();
				}
				default -> System.out.println("❌ Opción no válida. Intenta de nuevo.");
				}
			} catch (InputMismatchException e) {
				System.out.println("❌ Entrada no válida. Introduce un número.");
				sc.nextLine();
				opcion = -1;
			}
		} while (modelo == null);
	}

	public void mostrarMenu(AlumnosDAO modelo) {
		int opcion;
		do {
			imprimirMenu();
			System.out.print("Selecciona una opción: ");
			try {
				opcion = sc.nextInt();
				sc.nextLine(); // Limpiar buffer
				gestionarOpcion(opcion, modelo);
			} catch (InputMismatchException e) {
				System.out.println("Entrada no válida. Por favor, introduce un número.");
				sc.nextLine(); // Limpiar buffer en caso de error
				opcion = -1; // Reiniciar opción para evitar salir del bucle
			}
		} while (opcion != 0);
	}

	private void imprimirMenu() {

		String textoMenu = """
				---- Menú Principal -------------------------------------------
				1. Insertar nuevo alumno.
				2. Insertar nuevo grupo.
				3. Mostrar todos los alumnos.
				4. Guardar todos los alumnos en un fichero de texto.
				5. Leer alumnos de un fichero de texto y guardarlos en la BD.
				6. Modificar el nombre de un alumno por su NIA.
				7. Eliminar un alumno a partir de su NIA.
				8. Eliminar los alumnos del grupo indicado.
				9. Guardar grupos y alumnos en un archivo XML.
				10. Leer un archivo XML de grupos y guardar los datos en la BD.
				11. Mostrar todos los alumnos del grupo elegido.
				12. Mostrar todos los datos de un alumno por su NIA.
				13. Cambiar de grupo al alumno que elija el usuario.
				14. Guardar el grupo que elija el usuario en un fichero XML.
				0. Salir.
				---------------------------------------------------------------
				""";

		System.out.println(textoMenu);
	}

	/**
	 * Gestiona la opción seleccionada por el usuario y llama al método
	 * correspondiente.
	 * 
	 * @param opcion Opción seleccionada por el usuario.
	 * @param modelo Modelo de datos de alumnos y grupos.
	 */
	private void gestionarOpcion(int opcion, AlumnosDAO modelo) {
		switch (opcion) {

		case 1 -> insertarNuevoAlumno(modelo);
		case 2 -> insertarNuevoGrupo(modelo);
		case 3 -> mostrarTodosLosAlumnos(modelo, true); // Mostrará toda la información de todos los alumnos
		case 4 -> guardarAlumnosEnFicheroTexto(modelo);
		case 5 -> leerAlumnosDesdeFichero(modelo);
		case 6 -> modificarNombreAlumnoPorNia(modelo);
		case 7 -> eliminarAlumnoPorNIA(modelo);
		case 8 -> eliminarAlumnosPorGrupo(modelo);
		case 9 -> guardarGruposEnXML(modelo);
		case 10 -> leerYGuardarGruposXML(modelo);
		case 11 -> mostrarAlumnosPorGrupo(modelo);
		case 12 -> mostrarTodosLosAlumnos(modelo, false); // Muestra, 1º, el nia y el nombre de todos los alumnos
		case 13 -> cambiarGrupoAlumno(modelo);
		case 14 -> guardarGrupoEspecificoEnXML(modelo);
		case 0 -> System.out.println("Finalizando del programa...");
		default -> System.out.println("Opción no válida. Intenténtelo de nuevo.");
		}
	}

	/**
	 * Inserta un nuevo alumno solicitando los datos al usuario y almacenándolos en
	 * la base de datos.
	 * 
	 * @param modelo el objeto DAO para gestionar las operaciones de alumnos.
	 */

	public void insertarNuevoAlumno(AlumnosDAO modelo) {
		try {
			Alumno alumno = modelo.solicitarDatosAlumno();
			if (alumno == null) {
				System.out.println("❌ Error: El objeto Alumno es null.");
				return;
			}
			if (modelo.insertarAlumno(alumno)) {
				System.out.println("✅ Alumno insertado correctamente.");
			} else {
				System.out.println("❌ Error al insertar el alumno.");
			}
		} catch (Exception e) {
			System.out.println("❌ Ocurrió un error al insertar el alumno: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Inserta un nuevo grupo solicitando los datos al usuario y almacenándolos en
	 * la base de datos.
	 *
	 * @param modelo el DAO que permite interactuar con la base de datos.
	 */

	public void insertarNuevoGrupo(AlumnosDAO modelo) {
		String nombreGrupo;

		// Solicitar al usuario el nombre del nuevo grupo con validación
		while (true) {
			System.out.println("Introduce el nombre del nuevo grupo (una letra):");
			nombreGrupo = sc.nextLine().toUpperCase().trim();

			// Validamos que el nombre sea solo una letra
			if (nombreGrupo.length() == 1 && nombreGrupo.matches("[A-Za-z]")) {
				break; // Salimos del bucle si la validación es exitosa
			} else {
				System.out.println("El nombre del grupo debe ser una sola letra.");
			}
		}

		// Crear objeto Grupo
		Grupo grupo = new Grupo(nombreGrupo);

		// Llamamos al método del DAO para insertar el grupo
		if (modelo.insertarGrupo(grupo)) {
			System.out.println("Grupo insertado correctamente.");
		} else {
			System.out.println("Error al insertar el grupo.");
		}
	}

	/**
	 * Muestra todos los alumnos en la BD
	 * 
	 * @param modelo                    el DAO que permite interactuar con la base
	 *                                  de datos.
	 * @param mostrarTodaLaInformación.
	 */

	public void mostrarTodosLosAlumnos(AlumnosDAO modelo, boolean mostrarTodaLaInformación) {
		try {
			if (modelo.mostrarTodosLosAlumnos(mostrarTodaLaInformación)) {
				System.out.println("✅ Los alumnos se han mostrado correctamente.");
			} else {
				System.out.println("❌ No se pudieron mostrar los alumnos.");
			}
		} catch (Exception e) {
			System.out.println("❌ Ocurrió un error al mostrar los alumnos: " + e.getMessage());
		}
	}

	/**
	 * Permite guardar todos los alumnos en un archivo de texto. Recupera la
	 * información de los alumnos de la base de datos y la guarda en un archivo
	 * llamado "alumnos.txt". La información incluye: nombre, apellidos, género,
	 * fecha de nacimiento, ciclo, curso y nombre del grupo.
	 */

	public void guardarAlumnosEnFicheroTexto(AlumnosDAO modelo) {
		try {
			modelo.guardarAlumnosEnFicheroTexto();
			System.out.println("✅ Alumnos guardados correctamente en el archivo de texto.");
		} catch (Exception e) {
			System.out.println("❌ Ocurrió un error al guardar los alumnos en el archivo de texto: " + e.getMessage());
		}
	}

	/**
	 * Permite leer alumnos desde el fichero fijo "alumnos.txt" y guardarlos en la
	 * base de datos.
	 */
	public void leerAlumnosDesdeFichero(AlumnosDAO modelo) {
		try {
			if (modelo.leerAlumnosDeFicheroTexto()) {
				System.out.println("✅ Alumnos leídos e insertados correctamente desde el fichero 'alumnos.txt'.");
			} else {
				System.out.println("❌ Ocurrió un error al procesar el fichero.");
			}
		} catch (Exception e) {
			System.out.println("❌ Error al leer los alumnos desde el fichero: " + e.getMessage());
		}
	}

	/**
	 * Permite eliminar un alumno de la base de datos a partir de su NIA (PK).
	 */
	public void eliminarAlumnoPorNIA(AlumnosDAO modelo) {
		try {
			// Solicitar NIA al usuario
			System.out.println("Introduce el NIA del alumno a eliminar:");
			int nia = sc.nextInt();
			sc.nextLine(); // Limpiar buffer

			// Llamar directamente al DAO sin abrir la conexión
			if (modelo.eliminarAlumnoPorNIA(nia)) {
				System.out.println("✅ Alumno eliminado correctamente.");
			} else {
				System.out.println("❌ No se encontró un alumno con el NIA proporcionado.");
			}
		} catch (Exception e) {
			System.out.println("❌ Ocurrió un error al intentar eliminar el alumno: " + e.getMessage());
		}
	}

	/**
	 * Permite modificar el nombre de un alumno solicitando su NIA y el nuevo
	 * nombre.
	 */
	public void modificarNombreAlumnoPorNia(AlumnosDAO modelo) {
		try {
			// Solicitar al usuario el NIA del alumno
			System.out.print("Introduce el NIA del alumno cuyo nombre quieres modificar: ");
			int nia = sc.nextInt();
			sc.nextLine(); // Limpiar buffer

			// Solicitar el nuevo nombre del alumno
			System.out.print("Introduce el nuevo nombre para el alumno: ");
			String nuevoNombre = sc.nextLine().trim().toUpperCase();

			// Validar que el nombre no esté vacío
			if (nuevoNombre.isEmpty()) {
				System.out.println("❌ El nombre no puede estar vacío.");
				return;
			}

			// Llamar directamente al modelo sin gestionar la conexión aquí
			if (modelo.modificarNombreAlumnoPorNIA(nia, nuevoNombre)) {
				System.out.println("✅ Nombre del alumno modificado correctamente.");
			} else {
				System.out.println("⚠ No se pudo modificar el nombre del alumno. Verifica el NIA.");
			}
		} catch (Exception e) {
			System.out.println("❌ Ocurrió un error al modificar el nombre del alumno: " + e.getMessage());
		}
	}

	/**
	 * Elimina los alumnos del grupo indicado por el usuario. Muestra previamente
	 * los grupos existentes y permite al usuario seleccionar uno. Luego elimina a
	 * todos los alumnos que pertenezcan al grupo seleccionado.
	 */
	public void eliminarAlumnosPorGrupo(AlumnosDAO modelo) {
		try {
			// Mostramos los grupos disponibles
			System.out.println("Grupos disponibles:");
			if (!modelo.mostrarTodosLosGrupos()) { // ✅ Usa el modelo en lugar de `AlumnosBD`
				System.out.println("No hay grupos registrados.");
				return;
			}

			// Pedimos al usuario el nombre del grupo a eliminar
			System.out.println("Introduce el nombre del grupo cuyos alumnos deseas eliminar:");
			String nombreGrupo = sc.nextLine().toUpperCase().trim();

			// Confirmamos la operación con el usuario
			System.out.println(
					"¿Estás seguro de que deseas eliminar todos los alumnos del grupo " + nombreGrupo + "? (S/N)");
			String confirmacion = sc.nextLine().toUpperCase().trim();

			if (!confirmacion.equals("S")) {
				System.out.println("Operación cancelada por el usuario.");
				return;
			}

			// Llamamos al método del modelo para eliminar los alumnos
			if (modelo.eliminarAlumnosPorGrupo(nombreGrupo)) {
				System.out.println("✅ Alumnos del grupo " + nombreGrupo + " eliminados correctamente.");
			} else {
				System.out.println("❌ No se pudieron eliminar los alumnos. Verifica el nombre del grupo.");
			}
		} catch (Exception e) {
			System.out.println("❌ Ocurrió un error al eliminar alumnos por grupo: " + e.getMessage());
		}
	}

	/**
	 * Método que se encarga de guardar los grupos y sus alumnos en un archivo XML.
	 */
	public void guardarGruposEnXML(AlumnosDAO modelo) {
		try {
			if (modelo.guardarGruposEnXML()) {
				System.out.println("✅ Archivo XML guardado correctamente.");
			} else {
				System.out.println("❌ Error al guardar el archivo XML.");
			}
		} catch (Exception e) {
			System.out.println("❌ Ocurrió un error al guardar los grupos en XML: " + e.getMessage());
		}
	}

	/**
	 * Lee el archivo XML de grupos (grupos.xml) y guarda los datos en la base de
	 * datos MySQL. Si ocurre un error durante el proceso, se captura la excepción y
	 * se muestra un mensaje de error.
	 */
	public void leerYGuardarGruposXML(AlumnosDAO modelo) {
		// Ruta fija del archivo XML de grupos
		String rutaArchivo = "grupos.xml";

		// Verificamos si el archivo existe
		File archivoXML = new File(rutaArchivo);
		if (!archivoXML.exists()) {
			System.out.println("❌ El archivo XML no existe en la ruta especificada: " + rutaArchivo);
			return; // Salimos del método si el archivo no existe
		}

		// Llamamos directamente al método del modelo sin manejar conexiones
		if (modelo.leerYGuardarGruposXML(rutaArchivo)) {
			System.out.println("✅ Archivo XML leído correctamente y datos guardados en la base de datos.");
		} else {
			System.out.println("❌ Error al procesar el archivo XML.");
		}
	}

	/**
	 * Muestra los alumnos del grupo seleccionado por el usuario.
	 *
	 * @param modelo Objeto que implementa la interfaz AlumnosDAO para realizar
	 *               operaciones con la base de datos.
	 */
	public void mostrarAlumnosPorGrupo(AlumnosDAO modelo) {
		modelo.mostrarAlumnosPorGrupo();
	}

	/**
	 * Cambia de grupo al alumno seleccionado por el usuario.
	 * 
	 * @param modelo Objeto DAO para la gestión de alumnos y grupos.
	 */
	public void cambiarGrupoAlumno(AlumnosDAO modelo) {
		try {
			modelo.cambiarGrupoAlumno(); // ✅ Solo ejecuta la acción sin mostrar mensajes adicionales.
		} catch (Exception e) {
			System.out.println("❌ Se produjo un error al intentar cambiar al alumno de grupo. Revisa los logs.");
		}
	}

	/**
	 * Guarda un grupo específico con toda su información (incluyendo los alumnos)
	 * en un archivo XML.
	 * 
	 * @param modelo Objeto DAO para la gestión de alumnos y grupos.
	 */

	public void guardarGrupoEspecificoEnXML(AlumnosDAO modelo) {
		try {
			if (modelo.guardarGrupoEspecificoEnXML()) {
				System.out.println("✅ El grupo se ha guardado correctamente en un archivo XML.");
			} else {
				System.out.println("❌ No se pudo guardar el grupo en XML.");
			}
		} catch (Exception e) {
			System.out.println("❌ Se produjo un error al guardar el grupo en XML. Revisa los logs.");
			e.printStackTrace();
		}
	}

}