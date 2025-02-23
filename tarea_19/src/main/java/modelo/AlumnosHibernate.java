package modelo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Utilizará Hibernate para acceder a los datos.
 * 
 * @author Alberto Polo
 */
public class AlumnosHibernate implements AlumnosDAO {

	private final static Scanner sc = new Scanner(System.in);

	private static SessionFactory sessionFactory;
	private static BaseDatos baseDatosSeleccionada = BaseDatos.MYSQL; // Valor por defecto

	/**
	 * Inicializa Hibernate según la base de datos seleccionada.
	 */
	public static void inicializarHibernate() {
		String configFile = switch (baseDatosSeleccionada) {
		case ORACLE -> "hibernate-oracle.cfg.xml";
		case MYSQL -> "hibernate-mysql.cfg.xml";
		default -> "hibernate.cfg.xml";
		};

		try {
			// Cargar la configuración de Hibernate
			sessionFactory = new Configuration().configure(configFile).buildSessionFactory();
			System.out.println("✅ Hibernate inicializado correctamente con " + baseDatosSeleccionada);
		} catch (Throwable ex) {
			throw new ExceptionInInitializerError("❌ Error al inicializar Hibernate: " + ex);
		}
	}

	/**
	 * Establece la base de datos y reinicia Hibernate.
	 */
	public static void setBaseDatos(BaseDatos baseDatos) {
		baseDatosSeleccionada = baseDatos;
		if (sessionFactory != null) {
			sessionFactory.close();
		}
		inicializarHibernate();
	}

	/**
	 * Obtiene una sesión de Hibernate.
	 *
	 * @return una nueva sesión
	 */
	private Session getSession() {
		return sessionFactory.openSession();
	}

	// 1. Insertar nuevo alumno. //////////////////////////////////

	@Override
	public boolean insertarAlumno(Alumno alumno) {
		Transaction tx = null;
		Session session = null; // 🔹 Inicializamos `session` en null para asegurarnos de cerrarla en `finally`
		try {
			session = getSession();
			tx = session.beginTransaction();
			session.persist(alumno);
			tx.commit();
			System.out.println("✅ Alumno insertado en Hibernate.");
			return true;
		} catch (Exception e) {
			if (tx != null)
				tx.rollback();
			e.printStackTrace();
			return false;
		} finally {
			if (session != null)
				session.close(); // 🔹 Cerramos la sesión solo si fue abierta correctamente
		}
	}

	@Override
	public Alumno solicitarDatosAlumno() {
		System.out.println("Introduce el nombre del alumno:");
		String nombre = sc.nextLine().toUpperCase().trim();

		System.out.println("Introduce los apellidos:");
		String apellidos = sc.nextLine().toUpperCase().trim();

		System.out.println("Introduce el género (M/F):");
		char genero = sc.nextLine().toUpperCase().charAt(0);

		System.out.println("Introduce la fecha de nacimiento (dd-MM-yyyy):");
		Date fechaNacimiento;
		try {
			SimpleDateFormat formato = new SimpleDateFormat("dd-MM-yyyy");
			fechaNacimiento = formato.parse(sc.nextLine());
		} catch (Exception e) {
			System.out.println("❌ Formato de fecha incorrecto.");
			return null;
		}

		System.out.println("Introduce el ciclo:");
		String ciclo = sc.nextLine().toUpperCase().trim();

		System.out.println("Introduce el curso:");
		String curso = sc.nextLine().toUpperCase().trim();

		System.out.println("Introduce el nombre del grupo:");
		String nombreGrupo = sc.nextLine().toUpperCase().trim();

		// 🔹 Obtener el grupo desde la BD
		Grupo grupo;
		try (Session session = getSession()) {
			grupo = session.createQuery("FROM Grupo WHERE nombreGrupo = :nombre", Grupo.class)
					.setParameter("nombre", nombreGrupo).uniqueResult();
		}

		if (grupo == null) {
			System.out.println("❌ El grupo no existe en la BD. Debes crearlo antes de asignarlo a un alumno.");
			return null;
		}

		return new Alumno(nombre, apellidos, genero, fechaNacimiento, ciclo, curso, grupo);
	}

	// 2. Insertar nuevo grupo. //////////////////////////////////

	@Override
	public boolean insertarGrupo(Grupo grupo) {
		Transaction tx = null;
		try (Session session = getSession()) {
			tx = session.beginTransaction();
			session.persist(grupo); // Guardar el grupo en la base de datos
			tx.commit();
			System.out.println("✅ Grupo insertado correctamente en Hibernate.");
			return true;
		} catch (Exception e) {
			if (tx != null)
				tx.rollback();
			System.out.println("❌ Error al insertar el grupo: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	// 3. y 12. Mostrar todos los alumnos. //////////////////////////////////

	@Override
	public boolean mostrarTodosLosAlumnos(boolean mostrarTodaLaInformacion) {
		try (Session session = getSession()) {

			List<Alumno> alumnos = session.createQuery("FROM Alumno a ORDER BY a.nia", Alumno.class).getResultList();

			if (alumnos.isEmpty()) {
				System.out.println("No hay alumnos registrados.");
				return false;
			}

			List<Integer> listaNias = new ArrayList<>();

			if (mostrarTodaLaInformacion) {
				System.out.println("Lista completa de alumnos registrados:");
			} else {
				System.out.println("Lista de alumnos (NIA y Nombre):");
			}

			for (Alumno alumno : alumnos) {
				if (mostrarTodaLaInformacion) {
					System.out.printf("""
							-------------------------
							NIA: %d
							Nombre: %s
							Apellidos: %s
							Género: %s
							Fecha de nacimiento: %s
							Ciclo: %s
							Curso: %s
							Grupo: %s
							""", alumno.getNia(), alumno.getNombre(), alumno.getApellidos(), alumno.getGenero(),
							new SimpleDateFormat("dd-MM-yyyy").format(alumno.getFechaNacimiento()), alumno.getCiclo(),
							alumno.getCurso(),
							(alumno.getGrupo() != null ? alumno.getGrupo().getNombreGrupo() : "Sin grupo"));
				} else {
					System.out.printf("NIA: %d, Nombre: %s%n", alumno.getNia(), alumno.getNombre());
					listaNias.add(alumno.getNia());
				}
			}

			// Si estamos en modo "NIA y nombre", permitir al usuario seleccionar un NIA
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
			e.printStackTrace();
			System.out.println("Se produjo un error al recuperar los alumnos. Revisa los logs para más detalles.");
			return false;
		}
	}

	// 4. Guardar todos los alumnos en un fichero de texto. /////////////////////

	@Override
	public void guardarAlumnosEnFicheroTexto() {
		String nombreArchivo = "alumnos.txt";

		try (Session session = getSession();
				BufferedWriter writer = new BufferedWriter(new FileWriter(nombreArchivo))) {

			// Obtener todos los alumnos
			List<Alumno> alumnos = session.createQuery("FROM Alumno a ORDER BY a.nia", Alumno.class).getResultList();

			if (alumnos.isEmpty()) {
				System.out.println("No hay alumnos para guardar en el archivo.");
				return;
			}

			// Escribir la cabecera del archivo
			writer.write("NIA,Nombre,Apellidos,Género,Fecha Nacimiento,Ciclo,Curso,Nombre del Grupo");
			writer.newLine();

			// Escribir cada alumno en el archivo
			SimpleDateFormat formatoFecha = new SimpleDateFormat("dd-MM-yyyy");
			for (Alumno alumno : alumnos) {
				String linea = String.format("%d,%s,%s,%s,%s,%s,%s,%s", alumno.getNia(), alumno.getNombre(),
						alumno.getApellidos(), alumno.getGenero(), formatoFecha.format(alumno.getFechaNacimiento()),
						alumno.getCiclo(), alumno.getCurso(),
						(alumno.getGrupo() != null ? alumno.getGrupo().getNombreGrupo() : "Sin grupo"));

				writer.write(linea);
				writer.newLine();
			}

			System.out.println("✅ Alumnos guardados correctamente en " + nombreArchivo);

		} catch (IOException e) {
			System.out.println("❌ Error al guardar los alumnos en el archivo: " + e.getMessage());
		} catch (Exception e) {
			System.out.println("❌ Error inesperado: " + e.getMessage());
		}
	}

	// 5. Leer alumnos de un fichero de texto y guardarlos en la BD Hibernate.

	@Override
	public boolean leerAlumnosDeFicheroTexto() {
		String fichero = "alumnos.txt";
		int lineasInsertadas = 0;

		try (BufferedReader br = new BufferedReader(new FileReader(fichero)); Session session = getSession()) {

			Transaction tx = session.beginTransaction();
			String linea;

			// Ignorar la primera línea (cabecera)
			br.readLine();

			while ((linea = br.readLine()) != null) {
				System.out.println("📖 Leyendo línea: " + linea);

				// Separar los campos por coma
				String[] datos = linea.split(",");

				// Verificar que la línea tenga 8 campos
				if (datos.length == 8) {
					try {
						String nombre = datos[1].trim().toUpperCase();
						String apellidos = datos[2].trim().toUpperCase();
						char genero = datos[3].trim().toUpperCase().charAt(0);
						String fechaNacimiento = datos[4].trim();
						String ciclo = datos[5].trim().toUpperCase();
						String curso = datos[6].trim().toUpperCase();
						String nombreGrupo = datos[7].trim().toUpperCase();

						// Convertir la fecha
						SimpleDateFormat formatoFecha = new SimpleDateFormat("dd-MM-yyyy");
						Date fechaUtil = formatoFecha.parse(fechaNacimiento);

						// Buscar si el grupo ya existe
						Grupo grupo = session
								.createQuery("FROM Grupo g WHERE g.nombreGrupo = :nombreGrupo", Grupo.class)
								.setParameter("nombreGrupo", nombreGrupo).uniqueResult();

						// Si el grupo no existe, crearlo
						if (grupo == null) {
							grupo = new Grupo(nombreGrupo);
							session.persist(grupo);
						}

						// Crear e insertar el alumno
						Alumno alumno = new Alumno(nombre, apellidos, genero, fechaUtil, ciclo, curso, grupo);
						session.persist(alumno);
						lineasInsertadas++;
						System.out.println("✅ Alumno insertado: " + nombre + " " + apellidos);
					} catch (ParseException e) {
						System.out.println("❌ Error al convertir la fecha: " + datos[4]);
					}
				} else {
					System.out.println("⚠ Línea inválida en el fichero (número de campos incorrecto): " + linea);
				}
			}

			tx.commit();

			if (lineasInsertadas > 0) {
				System.out.println("✅ Alumnos insertados correctamente desde el fichero.");
				return true;
			} else {
				System.out.println("❌ No se insertaron alumnos.");
				return false;
			}
		} catch (IOException e) {
			System.out.println("❌ Error al leer el archivo: " + e.getMessage());
			return false;
		} catch (Exception e) {
			System.out.println("❌ Error en la base de datos al insertar alumnos: " + e.getMessage());
			return false;
		}
	}

	// 6. Modificar el nombre de un alumno por su NIA. //////////////////////

	@Override
	public boolean modificarNombreAlumnoPorNIA(int nia, String nuevoNombre) {
		Transaction tx = null;
		try (Session session = getSession()) {
			tx = session.beginTransaction();
			Alumno alumno = session.get(Alumno.class, nia);
			if (alumno != null) {
				alumno.setNombre(nuevoNombre);
				session.merge(alumno);
				tx.commit();
				System.out.println("✅ Nombre actualizado en Hibernate.");
				return true;
			}
			return false;
		} catch (Exception e) {
			if (tx != null)
				tx.rollback();
			e.printStackTrace();
			return false;
		}
	}

	// 7. Eliminar un alumno a partir de su NIA. ///////////////////////

	@Override
	public boolean eliminarAlumnoPorNIA(int nia) {
		Transaction tx = null;
		try (Session session = getSession()) {
			tx = session.beginTransaction();
			Alumno alumno = session.get(Alumno.class, nia);
			if (alumno != null) {
				session.remove(alumno);
				tx.commit();
				System.out.println("✅ Alumno eliminado en Hibernate.");
				return true;
			}
			return false;
		} catch (Exception e) {
			if (tx != null)
				tx.rollback();
			e.printStackTrace();
			return false;
		}
	}

	// 8. Eliminar los alumnos del grupo indicado.

	@Override
	public boolean eliminarAlumnosPorGrupo(String nombreGrupo) {
		Transaction tx = null;
		try (Session session = getSession()) {
			tx = session.beginTransaction();

			// Obtener el grupo por su nombre
			Grupo grupo = session.createQuery("FROM Grupo WHERE nombreGrupo = :nombreGrupo", Grupo.class)
					.setParameter("nombreGrupo", nombreGrupo).uniqueResult();

			if (grupo == null) {
				System.out.println("❌ El grupo '" + nombreGrupo + "' no existe.");
				return false;
			}

			// Eliminar los alumnos del grupo
			int eliminados = session.createMutationQuery("DELETE FROM Alumno WHERE grupo = :grupo")
					.setParameter("grupo", grupo).executeUpdate();

			tx.commit();
			System.out.println("✅ Se han eliminado " + eliminados + " alumnos del grupo '" + nombreGrupo + "'.");
			return eliminados > 0;
		} catch (Exception e) {
			if (tx != null)
				tx.rollback();
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean mostrarTodosLosGrupos() {
		try (Session session = getSession()) {
			List<String> nombresGrupos = session.createQuery("SELECT g.nombreGrupo FROM Grupo g", String.class)
					.getResultList();

			if (nombresGrupos.isEmpty()) {
				System.out.println("❌ No se encontraron grupos en la base de datos.");
				return false;
			}

			System.out.println("📌 Grupos disponibles:");
			for (String nombre : nombresGrupos) {
				System.out.println("- " + nombre);
			}

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("❌ Error al mostrar los grupos: " + e.getMessage());
			return false;
		}
	}

	// 9. Guardar grupos y alumnos en un archivo XML.

	@Override
	public boolean guardarGruposEnXML() {
		String nombreArchivo = "grupos.xml";

		try (Session session = getSession()) {
			// Obtener todos los grupos con sus alumnos (Lazy Loading -> Fetch JOIN)
			List<Grupo> grupos = session.createQuery("SELECT g FROM Grupo g LEFT JOIN FETCH g.alumnos", Grupo.class)
					.getResultList();

			if (grupos.isEmpty()) {
				System.out.println("⚠ No hay grupos registrados para guardar.");
				return false;
			}

			// Crear documento XML
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.newDocument();

			// Nodo raíz <grupos>
			Element rootElement = doc.createElement("grupos");
			doc.appendChild(rootElement);

			for (Grupo grupo : grupos) {
				Element grupoElement = doc.createElement("grupo");
				grupoElement.setAttribute("numeroGrupo", String.valueOf(grupo.getNumeroGrupo()));
				grupoElement.setAttribute("nombreGrupo", grupo.getNombreGrupo());
				rootElement.appendChild(grupoElement);

				for (Alumno alumno : grupo.getAlumnos()) {
					Element alumnoElement = doc.createElement("alumno");
					alumnoElement.setAttribute("nia", String.valueOf(alumno.getNia()));
					alumnoElement.setAttribute("nombre", alumno.getNombre());
					alumnoElement.setAttribute("apellidos", alumno.getApellidos());
					alumnoElement.setAttribute("genero", String.valueOf(alumno.getGenero()));
					alumnoElement.setAttribute("fechaNacimiento", alumno.getFechaNacimiento().toString());
					alumnoElement.setAttribute("ciclo", alumno.getCiclo());
					alumnoElement.setAttribute("curso", alumno.getCurso());

					grupoElement.appendChild(alumnoElement);
				}
			}

			// Guardar el documento XML
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");

			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(nombreArchivo));
			transformer.transform(source, result);

			System.out.println("✅ Archivo XML guardado correctamente en " + nombreArchivo);
			return true;
		} catch (Exception e) {
			System.out.println("❌ Error al generar el archivo XML: " + e.getMessage());
			return false;
		}
	}

	// 10. Leer un archivo XML de grupos y guardar los datos en la BD.

	@Override
	public boolean leerYGuardarGruposXML(String rutaArchivo) {
		try {
			File archivoXML = new File("grupos.xml"); // Usamos la ruta fija
			if (!archivoXML.exists()) {
				System.out.println("❌ El archivo XML no existe en la ruta especificada.");
				return false;
			}

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document documento = builder.parse(archivoXML);
			documento.getDocumentElement().normalize();

			NodeList listaGrupos = documento.getElementsByTagName("grupo");

			try (Session session = getSession()) {
				Transaction tx = session.beginTransaction();

				for (int i = 0; i < listaGrupos.getLength(); i++) {
					Node nodoGrupo = listaGrupos.item(i);
					if (nodoGrupo.getNodeType() == Node.ELEMENT_NODE) {
						Element elementoGrupo = (Element) nodoGrupo;

						String nombreGrupo = elementoGrupo.getAttribute("nombreGrupo");

						// Verificar si el grupo ya existe antes de insertarlo
						Grupo grupoExistente = session
								.createQuery("FROM Grupo WHERE nombreGrupo = :nombreGrupo", Grupo.class)
								.setParameter("nombreGrupo", nombreGrupo).uniqueResult();

						Grupo grupo;
						if (grupoExistente != null) {
							grupo = grupoExistente;
						} else {
							grupo = new Grupo(nombreGrupo);
							session.persist(grupo);
							session.flush(); // Forzar escritura para obtener el ID
						}

						NodeList listaAlumnos = elementoGrupo.getElementsByTagName("alumno");
						for (int j = 0; j < listaAlumnos.getLength(); j++) {
							Node nodoAlumno = listaAlumnos.item(j);
							if (nodoAlumno.getNodeType() == Node.ELEMENT_NODE) {
								Element elementoAlumno = (Element) nodoAlumno;

								String nombre = elementoAlumno.getAttribute("nombre");
								String apellidos = elementoAlumno.getAttribute("apellidos");
								char genero = elementoAlumno.getAttribute("genero").charAt(0);

								SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd"); // Ajusta el formato
																								// según el XML
								java.util.Date fechaUtil = formato
										.parse(elementoAlumno.getAttribute("fechaNacimiento"));
								java.sql.Date fechaNacimiento = new java.sql.Date(fechaUtil.getTime());

								// Date fechaNacimiento =
								// Date.valueOf(elementoAlumno.getAttribute("fechaNacimiento"));
								String ciclo = elementoAlumno.getAttribute("ciclo");
								String curso = elementoAlumno.getAttribute("curso");

								Alumno alumno = new Alumno(nombre, apellidos, genero, fechaNacimiento, ciclo, curso,
										grupo);
								session.persist(alumno);
							}
						}
					}
				}

				tx.commit();
				System.out.println("✅ Archivo XML procesado correctamente. Datos guardados en la BD.");
				return true;
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("❌ Error al procesar el archivo XML: " + e.getMessage());
			return false;
		}
	}

	// 11. Mostrar todos los alumnos del grupo elegido.

	@Override
	public void mostrarAlumnosPorGrupo() {
		// Mostrar todos los grupos antes de pedir el nombre
		if (!mostrarTodosLosGrupos()) {
			System.out.println("No hay grupos disponibles para mostrar.");
			return;
		}

		System.out.println("Introduce el nombre del grupo del que quieres ver los alumnos:");
		String nombreGrupo = sc.nextLine().trim().toUpperCase();

		try (Session session = getSession()) {
			// Obtener el grupo
			Grupo grupo = session.createQuery("FROM Grupo WHERE nombreGrupo = :nombreGrupo", Grupo.class)
					.setParameter("nombreGrupo", nombreGrupo).uniqueResult();

			if (grupo == null) {
				System.out.println("❌ El grupo especificado no existe. Inténtalo de nuevo.");
				return;
			}

			// Obtener alumnos del grupo
			List<Alumno> alumnos = session.createQuery("FROM Alumno WHERE grupo = :grupo", Alumno.class)
					.setParameter("grupo", grupo).getResultList();

			if (alumnos.isEmpty()) {
				System.out.println("❌ No hay alumnos registrados en este grupo.");
				return;
			}

			// Mostrar alumnos en formato estructurado
			System.out.println("Alumnos del grupo '" + nombreGrupo + "':");
			for (Alumno alumno : alumnos) {
				System.out.printf("""
						NIA: %d
						Nombre: %s
						Apellidos: %s
						Género: %s
						Fecha de nacimiento: %s
						Ciclo: %s
						Curso: %s
						Grupo: %s
						-------------------------\n
						""", alumno.getNia(), alumno.getNombre(), alumno.getApellidos(), alumno.getGenero(),
						alumno.getFechaNacimiento(), alumno.getCiclo(), alumno.getCurso(), grupo.getNombreGrupo());
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("❌ Se produjo un error al intentar mostrar los alumnos. Revisa los logs.");
		}
	}

	// 12. y 3. Mostrar todos los alumnos. //////////////////////////////////

	@Override
	public boolean mostrarAlumnoPorNIA(int nia) {
		try (Session session = getSession()) {
			Alumno alumno = session.get(Alumno.class, nia);

			if (alumno == null) {
				System.out.println("❌ No se encontró un alumno con el NIA: " + nia);
				return false;
			}

			// Mostrar la información detallada del alumno
			System.out.printf("""
					-------------------------
					NIA: %d
					Nombre: %s
					Apellidos: %s
					Género: %s
					Fecha de nacimiento: %s
					Ciclo: %s
					Curso: %s
					Grupo: %s
					""", alumno.getNia(), alumno.getNombre(), alumno.getApellidos(), alumno.getGenero(),
					new SimpleDateFormat("dd-MM-yyyy").format(alumno.getFechaNacimiento()), alumno.getCiclo(),
					alumno.getCurso(), (alumno.getGrupo() != null ? alumno.getGrupo().getNombreGrupo() : "Sin grupo"));

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("❌ Ocurrió un error al recuperar los datos del alumno.");
			return false;
		}
	}

	// 13. Cambiar de grupo al alumno que elija el usuario.

	/**
	 * Muestra solo NIA y nombre de los alumnos sin interacción extra.
	 * 
	 * @return true si hay alumnos, false si no hay registros.
	 */
	public boolean listarNiasYNombresAlumnos() {
		try (Session session = getSession()) {
			List<Alumno> alumnos = session.createQuery("FROM Alumno", Alumno.class).list();

			if (alumnos.isEmpty()) {
				System.out.println("❌ No hay alumnos registrados.");
				return false;
			}

			System.out.println("Lista de alumnos disponibles para cambiar de grupo:");
			for (Alumno alumno : alumnos) {
				System.out.printf("NIA: %d, Nombre: %s%n", alumno.getNia(), alumno.getNombre());
			}

			return true;
		} catch (Exception e) {
			System.out.println("❌ Error al recuperar la lista de alumnos: " + e.getMessage());
			return false;
		}
	}

	@Override
	public boolean cambiarGrupoAlumno() {
		if (!listarNiasYNombresAlumnos()) {
			System.out.println("❌ No hay alumnos disponibles.");
			return false;
		}

		System.out.println("\nIntroduce el NIA del alumno al que deseas cambiar de grupo:");
		int niaSeleccionado;
		try {
			niaSeleccionado = Integer.parseInt(sc.nextLine().trim());
		} catch (NumberFormatException e) {
			System.out.println("❌ El NIA debe ser un número válido.");
			return false;
		}

		try (Session session = getSession()) {
			Transaction tx = session.beginTransaction();

			Alumno alumno = session.get(Alumno.class, niaSeleccionado);
			if (alumno == null) {
				System.out.println("❌ No se encontró ningún alumno con el NIA proporcionado.");
				return false;
			}

			// Mostrar grupos disponibles
			List<Grupo> grupos = session.createQuery("FROM Grupo", Grupo.class).list();
			if (grupos.isEmpty()) {
				System.out.println("❌ No hay grupos disponibles.");
				return false;
			}

			System.out.println("\nGrupos disponibles:");
			for (Grupo grupo : grupos) {
				System.out.println("- " + grupo.getNombreGrupo());
			}

			System.out.println("\nIntroduce el nombre del grupo al que deseas cambiar al alumno:");
			String nuevoGrupo = sc.nextLine().trim().toUpperCase();

			Grupo grupo = session.createQuery("FROM Grupo WHERE nombreGrupo = :nombreGrupo", Grupo.class)
					.setParameter("nombreGrupo", nuevoGrupo).uniqueResult();

			if (grupo == null) {
				System.out.println("❌ El grupo especificado no existe.");
				return false;
			}

			if (alumno.getGrupo() != null && alumno.getGrupo().getNombreGrupo().equals(nuevoGrupo)) {
				System.out.println("⚠️ El alumno ya pertenece al grupo '" + nuevoGrupo + "'.");
				return false;
			}

			alumno.setGrupo(grupo);
			session.merge(alumno);

			tx.commit();
			System.out.println("✅ El grupo del alumno ha sido cambiado exitosamente.");
			return true;
		} catch (Exception e) {
			System.out.println("❌ Error al cambiar el grupo del alumno: " + e.getMessage());
			return false;
		}
	}

	@Override
	public boolean guardarGrupoEspecificoEnXML() {
		try (Session session = getSession()) {
			// Mostrar grupos disponibles
			if (!mostrarTodosLosGrupos()) {
				System.out.println("❌ No hay grupos disponibles para seleccionar.");
				return false;
			}

			// Solicitar el nombre del grupo al usuario
			System.out.print("\nIntroduce el nombre del grupo que deseas guardar en fichero XML: ");
			String nombreGrupo = sc.nextLine().trim().toUpperCase();

			// Obtener el grupo con sus alumnos usando JOIN FETCH
			Grupo grupo = session
					.createQuery("SELECT g FROM Grupo g LEFT JOIN FETCH g.alumnos WHERE g.nombreGrupo = :nombreGrupo",
							Grupo.class)
					.setParameter("nombreGrupo", nombreGrupo).uniqueResult();

			if (grupo == null) {
				System.out.println("❌ El grupo '" + nombreGrupo + "' no existe.");
				return false;
			}

			// Nombre del archivo XML
			String nombreArchivo = "grupo_" + nombreGrupo + ".xml";
			File archivoXML = new File(nombreArchivo);

			// Verificar si el archivo ya existe
			if (archivoXML.exists()) {
				System.out.print("⚠️ El archivo '" + nombreArchivo + "' ya existe. ¿Deseas sobrescribirlo? (S/N): ");
				String respuesta = sc.nextLine().trim().toUpperCase();
				if (!respuesta.equals("S")) {
					System.out.println("🚫 Operación cancelada por el usuario.");
					return false;
				}
			}

			// Crear el documento XML
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.newDocument();

			// Crear el elemento raíz <grupo>
			Element grupoElement = doc.createElement("grupo");
			grupoElement.setAttribute("numeroGrupo", String.valueOf(grupo.getNumeroGrupo()));
			grupoElement.setAttribute("nombreGrupo", grupo.getNombreGrupo());
			doc.appendChild(grupoElement);

			// Agregar alumnos al XML
			for (Alumno alumno : grupo.getAlumnos()) {
				Element alumnoElement = doc.createElement("alumno");
				alumnoElement.setAttribute("nia", String.valueOf(alumno.getNia()));
				alumnoElement.setAttribute("nombre", alumno.getNombre());
				alumnoElement.setAttribute("apellidos", alumno.getApellidos());
				alumnoElement.setAttribute("genero", String.valueOf(alumno.getGenero()));
				alumnoElement.setAttribute("fechaNacimiento",
						new SimpleDateFormat("dd-MM-yyyy").format(alumno.getFechaNacimiento()));
				alumnoElement.setAttribute("ciclo", alumno.getCiclo());
				alumnoElement.setAttribute("curso", alumno.getCurso());

				grupoElement.appendChild(alumnoElement);
			}

			// Guardar el archivo XML
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");

			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(nombreArchivo));
			transformer.transform(source, result);

			System.out.println("✅ El archivo XML del grupo '" + nombreGrupo + "' se ha guardado correctamente en '"
					+ nombreArchivo + "'.");
			return true;

		} catch (Exception e) {
			System.out.println("❌ Error al guardar el grupo en XML: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}
}