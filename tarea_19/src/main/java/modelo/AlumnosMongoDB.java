package modelo;

import static com.mongodb.client.model.Filters.eq;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;

import pool.PoolConexiones;

public class AlumnosMongoDB implements AlumnosDAO {

	private final MongoClient mongoClient;
	private final MongoDatabase database;
	private final MongoCollection<Document> coleccionAlumnos;
	private final MongoCollection<Document> coleccionCounters;
	private final Scanner sc = new Scanner(System.in);

	private static final Logger loggerGeneral = LogManager.getRootLogger();
	private static final Logger loggerExcepciones = LogManager.getLogger("exceptions");

	/**
	 * Constructor: Establece la conexión con la base de datos MongoDB.
	 */
	public AlumnosMongoDB() {
		Properties properties = new Properties();

		try (FileInputStream input = new FileInputStream("src/main/resources/mongo.properties")) {
			properties.load(input);

			String uri = properties.getProperty("mongo.uri");
			String nombreBD = properties.getProperty("mongo.database");
			String nombreColeccionAlumnos = properties.getProperty("mongo.collection");
			String nombreColeccionCounters = properties.getProperty("mongo.collectionCounters");

			// Conectar a MongoDB
			mongoClient = MongoClients.create(uri);
			database = mongoClient.getDatabase(nombreBD);
			coleccionAlumnos = database.getCollection(nombreColeccionAlumnos);
			coleccionCounters = database.getCollection(nombreColeccionCounters);

			System.out.println("✅ Conectado a MongoDB correctamente.");

			// Sincronizar contador tras la conexión
			sincronizarContadorNia();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Error al cargar el archivo de configuración.");
		}
	}

	/**
	 * Método para cerrar la conexión a MongoDB.
	 */
	public void cerrarConexion() {
		if (mongoClient != null) {
			mongoClient.close();
			System.out.println("✅ Conexión cerrada correctamente.");
		}
	}

	/**
	 * Cierra la conexión al finalizar.
	 */
	@Override
	protected void finalize() throws Throwable {
		cerrarConexion();
		super.finalize();
	}

	/**
	 * Sincroniza la colección 'counters' con el NIA más alto en la colección de
	 * alumnos.
	 */
	public void sincronizarContadorNia() {
		// Obtener el NIA máximo actual en la colección de alumnos
		Document niaMaximo = coleccionAlumnos.find().sort(new Document("nia", -1)).first();

		int siguienteNia = (niaMaximo != null) ? niaMaximo.getInteger("nia") : 1000;

		// Actualizar o insertar el valor en la colección 'counters'
		Document filtro = new Document("_id", "alumno_nia");
		Document actualizacion = new Document("$set", new Document("seq", siguienteNia));
		coleccionCounters.updateOne(filtro, actualizacion, new com.mongodb.client.model.UpdateOptions().upsert(true));

		loggerGeneral.info("Contador de NIA sincronizado. El siguiente NIA será: " + (siguienteNia + 1));
	}

	/**
	 * Obtiene el siguiente valor para el NIA (autoincremental)
	 */
	public int obtenerSiguienteNia() {
		Document filtro = new Document("_id", "alumno_nia");
		Document actualizacion = new Document("$inc", new Document("seq", 1));
		Document resultado = coleccionCounters.findOneAndUpdate(filtro, actualizacion,
				new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER));

		if (resultado != null && resultado.containsKey("seq")) {
			return resultado.getInteger("seq");
		} else {
			loggerGeneral.error("No se pudo obtener el valor del NIA.");
			return 1000; // Valor inicial por defecto si hay un fallo
		}
	}

	/**
	 * Solicita al usuario los datos necesarios para crear un objeto Alumno.
	 * 
	 * @return Un objeto Alumno con los datos ingresados por el usuario.
	 */
	@Override
	public Alumno solicitarDatosAlumno() {
		try {
			System.out.println("Introduce el nombre del alumno:");
			String nombre = sc.nextLine().trim().toUpperCase();

			System.out.println("Introduce los apellidos del alumno:");
			String apellidos = sc.nextLine().trim().toUpperCase();

			// Validar género
			char respuestaGenero = solicitarGenero();

			// Validar fecha de nacimiento
			Date fechaNacimiento = solicitarFechaNacimiento();

			System.out.println("Introduce el ciclo del alumno:");
			String ciclo = sc.nextLine().trim().toUpperCase();

			System.out.println("Introduce el curso del alumno:");
			String curso = sc.nextLine().trim().toUpperCase();

			// Validar nombre del grupo
			String nombreGrupo = solicitarNombreGrupo();

			// Crear el objeto Grupo
			Grupo grupo = new Grupo(nombreGrupo);

			// Crear y devolver el objeto Alumno
			Alumno alumno = new Alumno(nombre, apellidos, respuestaGenero, fechaNacimiento, ciclo, curso, grupo);
			loggerGeneral.info("Datos del alumno solicitados correctamente: {}", alumno);
			return alumno;

		} catch (Exception e) {
			loggerExcepciones.error("Error inesperado al solicitar datos del alumno: {}", e.getMessage(), e);
			return null;
		}
	}

	private char solicitarGenero() {
		char genero;
		do {
			System.out.println("Introduce el género del alumno (M/F):");
			String input = sc.nextLine().trim().toUpperCase();
			if (input.length() == 1 && (input.charAt(0) == 'M' || input.charAt(0) == 'F')) {
				genero = input.charAt(0);
				break;
			} else {
				loggerGeneral.info("Entrada no válida para género: {}", input);
				System.out.println("Respuesta no válida. Introduce 'M' o 'F'.");
			}
		} while (true);
		return genero;
	}

	private Date solicitarFechaNacimiento() {
		Date fecha = null;
		SimpleDateFormat formatoFecha = new SimpleDateFormat("dd-MM-yyyy");
		formatoFecha.setLenient(false); // Validación estricta

		do {
			System.out.println("Introduce la fecha de nacimiento (dd-MM-aaaa):");
			String fechaInput = sc.nextLine().trim();
			try {
				fecha = formatoFecha.parse(fechaInput);
			} catch (ParseException e) {
				loggerExcepciones.error("Formato de fecha inválido ingresado: {}", fechaInput);
				System.out.println("Formato de fecha inválido. Intenta de nuevo.");
			}
		} while (fecha == null);

		return fecha;
	}

	private String solicitarNombreGrupo() {
		String nombreGrupo;
		do {
			System.out.println("Introduce el nombre del grupo del alumno:");
			nombreGrupo = sc.nextLine().trim().toUpperCase();
			if (!validarNombreGrupo(nombreGrupo)) {
				loggerGeneral.info("Entrada no válida para grupo: {}", nombreGrupo);
				System.out.println("El nombre del grupo no es válido. Intenta de nuevo.");
			}
		} while (!validarNombreGrupo(nombreGrupo));
		return nombreGrupo;
	}

	/**
	 * Valida si un nombre de grupo existe en la base de datos.
	 * 
	 * @param nombreGrupo El nombre del grupo a validar.
	 * @return true si el grupo existe, false en caso contrario.
	 */
	public boolean validarNombreGrupo(String nombreGrupo) {
		String sql = "SELECT 1 FROM grupos WHERE nombreGrupo = ?";

		try (Connection conexion = PoolConexiones.getConnection();
				PreparedStatement sentencia = conexion.prepareStatement(sql)) {

			sentencia.setString(1, nombreGrupo);

			try (ResultSet resultado = sentencia.executeQuery()) {
				return resultado.next(); // Retorna true si el grupo existe, false si no
			}
		} catch (SQLException e) {
			loggerExcepciones.error("Error al validar el grupo '{}': {}", nombreGrupo, e.getMessage());
			return false;
		}
	}

	// 1. Insertar nuevo alumno.

	/**
	 * Inserta un nuevo alumno en la colección de MongoDB. El campo 'nia' se
	 * autoincrementa utilizando la colección 'counters'.
	 * 
	 * @param alumno El objeto Alumno que se desea insertar.
	 * @return true si la inserción fue exitosa, false en caso contrario.
	 */
	@Override
	public boolean insertarAlumno(Alumno alumno) {
		try {
			int nuevoNia = obtenerSiguienteNia();
			alumno.setNia(nuevoNia);

			// Formatear la fecha en el formato deseado (dd-MM-aaaa)
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
			String fechaFormateada = sdf.format(alumno.getFechaNacimiento());

			// Convertir el objeto Alumno a un documento BSON para la inserción
			Document alumnoDoc = new Document().append("nia", alumno.getNia()).append("nombre", alumno.getNombre())
					.append("apellidos", alumno.getApellidos()).append("genero", String.valueOf(alumno.getGenero()))
					.append("fechaNacimiento", fechaFormateada).append("ciclo", alumno.getCiclo())
					.append("curso", alumno.getCurso()).append("grupo", alumno.getGrupo().getNombreGrupo());

			coleccionAlumnos.insertOne(alumnoDoc);
			loggerGeneral.info("Alumno insertado correctamente con NIA: " + nuevoNia);
			return true;

		} catch (Exception e) {
			loggerExcepciones.error("Error al insertar el alumno: " + e.getMessage(), e);
			return false;
		}
	}

	// 2. Insertar nuevo grupo.

	@Override
	public boolean insertarGrupo(Grupo grupo) {
		// TODO Auto-generated method stub
		return false;
	}

	// 3. Mostrar todos los alumnos.

	/**
	 * Muestra todos los alumnos de la colección.
	 *
	 * @param mostrarTodaLaInformacion Indica si se debe mostrar toda la información
	 *                                 (true) o solo NIA y nombre (false).
	 * @return true si se muestran los alumnos correctamente, false en caso
	 *         contrario.
	 */
	@Override
	public boolean mostrarTodosLosAlumnos(boolean mostrarTodaLaInformacion) {
		try {
			FindIterable<Document> documentos = coleccionAlumnos.find();

			if (documentos.first() == null) {
				System.out.println("No hay alumnos registrados.");
				return false;
			}

			List<Integer> listaNias = new ArrayList<>();

			if (mostrarTodaLaInformacion) {
				System.out.println("Lista completa de alumnos registrados:");
			} else {
				System.out.println("Lista de alumnos (NIA y Nombre):");
			}

			for (Document doc : documentos) {
				int nia = doc.getInteger("nia");
				String nombre = doc.getString("nombre");

				if (mostrarTodaLaInformacion) {
					String apellidos = doc.getString("apellidos");
					String genero = doc.getString("genero");
					String fechaNacimiento = doc.getString("fechaNacimiento");
					String ciclo = doc.getString("ciclo");
					String curso = doc.getString("curso");
					String grupo = doc.getString("grupo");

					System.out.printf("""
							NIA: %d
							Nombre: %s
							Apellidos: %s
							Género: %s
							Fecha de nacimiento: %s
							Ciclo: %s
							Curso: %s
							Grupo: %s
							-------------------------
							""", nia, nombre, apellidos, genero, fechaNacimiento, ciclo, curso,
							(grupo != null ? grupo : "Sin grupo"));
				} else {
					// Mostrar solo NIA y nombre
					System.out.printf("NIA: %d, Nombre: %s%n", nia, nombre);
					listaNias.add(nia);
				}
			}

			// Si solo se mostró NIA y nombre, permitir al usuario seleccionar un NIA
			if (!mostrarTodaLaInformacion) {
				System.out.println("\nIntroduce el NIA del alumno que deseas visualizar (o 0 para salir):");
				while (true) {
					try {
						int niaSeleccionado = Integer.parseInt(sc.nextLine().trim());

						if (niaSeleccionado == 0) {
							System.out.println("Saliendo sin seleccionar un alumno.");
							return true;
						}

						if (listaNias.contains(niaSeleccionado)) {
							return mostrarAlumnoPorNIA(niaSeleccionado);
						} else {
							System.out.println("El NIA seleccionado no está en la lista. Inténtalo de nuevo.");
						}
					} catch (NumberFormatException e) {
						System.out.println("El NIA debe ser un número válido. Inténtalo de nuevo:");
					}
				}
			}

			return true;
		} catch (Exception e) {
			loggerExcepciones.error("❌ Error al mostrar los alumnos: " + e.getMessage());
			return false;
		}
	}

	// 4. Guardar todos los alumnos en un fichero de texto.

	/**
	 * Guarda todos los alumnos en un fichero de texto. La información incluye sus
	 * datos y el grupo al que pertenecen. Los alumnos se ordenan de forma
	 * ascendente por su NIA.
	 */
	@Override
	public void guardarAlumnosEnFicheroTexto() {
		String nombreFichero = "alumnos.txt";
		File fichero = new File(nombreFichero);

		// Verificar si el archivo existe y pedir confirmación para sobreescribirlo
		if (fichero.exists()) {
			System.out.print("El fichero ya existe. ¿Desea sobreescribirlo? (S/N): ");
			char respuesta = sc.nextLine().toUpperCase().charAt(0);
			if (respuesta != 'S') {
				System.out.println("Operación cancelada. El fichero no se sobrescribirá.");
				loggerGeneral.info("El usuario decidió no sobrescribir el fichero '{}'.", nombreFichero);
				return;
			}
		}

		// Conectar con la base de datos MongoDB y recuperar los datos de los alumnos
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(fichero))) {
			MongoCollection<Document> coleccion = database.getCollection("alumnos");

			// Buscar todos los alumnos y ordenar por NIA
			FindIterable<Document> alumnos = coleccion.find().sort(Sorts.ascending("nia"));

			// Escribir encabezados en el fichero
			writer.write("NIA,Nombre,Apellidos,Género,Fecha Nacimiento,Ciclo,Curso,Nombre del Grupo");
			writer.newLine();

			// Verificar si hay alumnos para guardar
			if (!alumnos.iterator().hasNext()) {
				System.out.println("No hay alumnos registrados para guardar en el fichero.");
				loggerGeneral.info("No se encontraron alumnos en la base de datos para guardar en el fichero.");
				return;
			}

			// Escribir los datos de los alumnos en el fichero
			for (Document doc : alumnos) {
			    int nia = doc.getInteger("nia", 0);
			    String nombre = doc.getString("nombre");
			    String apellidos = doc.getString("apellidos");
			    String genero = doc.getString("genero");
			    String fechaNacimiento = doc.getString("fechaNacimiento");
			    String ciclo = doc.getString("ciclo");
			    String curso = doc.getString("curso");

			    Object grupoObj = doc.get("grupo");
			    String nombreGrupo = "Sin grupo";

			    if (grupoObj instanceof Document) {
			        nombreGrupo = ((Document) grupoObj).getString("nombreGrupo");
			    } else if (grupoObj instanceof String) {
			        nombreGrupo = (String) grupoObj;
			    }

			    writer.write(String.format("%d,%s,%s,%s,%s,%s,%s,%s",
			            nia, nombre, apellidos, genero, fechaNacimiento, ciclo, curso, nombreGrupo));
			    writer.newLine();
			}


			System.out.println("Datos de los alumnos guardados correctamente en el fichero 'alumnos.txt'.");
			loggerGeneral.info("Los datos de los alumnos se guardaron correctamente en el fichero '{}'.",
					nombreFichero);

		} catch (IOException e) {
			loggerExcepciones.error("Error al escribir en el fichero '{}': {}", nombreFichero, e.getMessage(), e);
			System.out.println("Se produjo un error al escribir en el fichero. Revisa los logs para más detalles.");
		} catch (MongoException e) {
			loggerExcepciones.error("Error al consultar la base de datos MongoDB: {}", e.getMessage(), e);
			System.out.println(
					"Se produjo un error al recuperar los datos de MongoDB. Revisa los logs para más detalles.");
		}
	}

	// 5. Leer alumnos de un fichero de texto y guardarlos en la BD.

	@Override
	public boolean leerAlumnosDeFicheroTexto() {
		// TODO Auto-generated method stub
		return false;
	}

	// 6. Modificar el nombre de un alumno por su NIA.

	@Override
	public boolean modificarNombreAlumnoPorNIA(int nia, String nuevoNombre) {
		// TODO Auto-generated method stub
		return false;
	}

	// 7. Eliminar un alumno a partir de su NIA.

	/**
	 * Elimina un alumno por su NIA.
	 */
	@Override
	public boolean eliminarAlumnoPorNIA(int nia) {
		try {
			Bson filtro = eq("nia", nia);
			coleccionAlumnos.deleteOne(filtro);
			System.out.println("✅ Alumno con NIA " + nia + " eliminado correctamente.");
			return true;
		} catch (Exception e) {
			loggerExcepciones.error("❌ Error al eliminar el alumno: " + e.getMessage());
			return false;
		}
	}

	@Override
	public boolean mostrarTodosLosGrupos() {
		// TODO Auto-generated method stub
		return false;
	}

	// 8. Eliminar los alumnos del grupo indicado.

	@Override
	public boolean eliminarAlumnosPorGrupo(String grupo) {
		// TODO Auto-generated method stub
		return false;
	}

	// 9. Guardar grupos y alumnos en un archivo XML.

	@Override
	public boolean guardarGruposEnXML() {
		// TODO Auto-generated method stub
		return false;
	}

	// 10. Leer un archivo XML de grupos y guardar los datos en la BD.

	@Override
	public boolean leerYGuardarGruposXML(String rutaArchivo) {
		// TODO Auto-generated method stub
		return false;
	}

	// 11. Mostrar todos los alumnos del grupo elegido.

	@Override
	public void mostrarAlumnosPorGrupo() {
		// TODO Auto-generated method stub

	}

	// 12. Mostrar todos los datos de un alumno por su NIA.

	@Override
	public boolean mostrarAlumnoPorNIA(int nia) {
		// TODO Auto-generated method stub
		return false;
	}

	// 13. Cambiar de grupo al alumno que elija el usuario.

	@Override
	public boolean cambiarGrupoAlumno() {
		// TODO Auto-generated method stub
		return false;
	}

	// 14. Guardar el grupo que elija el usuario en un fichero XML.

	@Override
	public boolean guardarGrupoEspecificoEnXML() {
		// TODO Auto-generated method stub
		return false;
	}
}
