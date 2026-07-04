import Controller.EscuelaController;
import Models.*;
import Exceptions.*;
import Web.WebServer;
import Repository.DataRepository;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Main {
    private static EscuelaController controller;
    private static WebServer webServer;
    private static Scanner scanner;

    public static void main(String[] args) {
        controller = new EscuelaController();
        scanner = new Scanner(System.in);

        // 1. Iniciar Servidor Web en segundo plano
        try {
            webServer = new WebServer(controller, 8080);
            webServer.start();
        } catch (IOException e) {
            System.err.println("[Error] No se pudo iniciar el servidor web: " + e.getMessage());
        }

        System.out.println("=================================================");
        System.out.println("   BIENVENIDO A LA I.E. \"MI PERÚ\" - PORTAL CLI  ");
        System.out.println("=================================================");
        System.out.println("Nota: El portal web está corriendo en http://localhost:8080");

        boolean ejecutando = true;
        while (ejecutando) {
            try {
                System.out.println("\n--- INICIO DE SESIÓN ---");
                System.out.print("Usuario: ");
                String username = scanner.nextLine().trim();
                System.out.print("Contraseña: ");
                String password = scanner.nextLine().trim();

                Usuario usuarioLogueado = controller.login(username, password);
                System.out.println("\n[Éxito] Sesión iniciada como " + usuarioLogueado.getRol() + ": " + usuarioLogueado.getNombre());

                ejecutarMenuPorRol(usuarioLogueado);

            } catch (LoginException e) {
                System.out.println("[Error de Acceso] " + e.getMessage());
            } catch (Exception e) {
                System.out.println("[Error Inesperado] Ocurrió un inconveniente: " + e.getMessage());
            }

            System.out.print("\n¿Desea salir completamente de la aplicación de consola? (si/no): ");
            String salir = scanner.nextLine().trim().toLowerCase();
            if (salir.equals("si") || salir.equals("s")) {
                ejecutando = false;
            }
        }

        // Apagar el servidor web
        if (webServer != null) {
            webServer.stop();
        }
        System.out.println("¡Gracias por utilizar el sistema de la I.E. \"Mi Perú\"!");
    }

    private static void ejecutarMenuPorRol(Usuario usuario) {
        boolean enSesion = true;
        while (enSesion) {
            try {
                if (usuario instanceof Director) {
                    enSesion = menuDirector((Director) usuario);
                } else if (usuario instanceof Docente) {
                    enSesion = menuDocente((Docente) usuario);
                } else if (usuario instanceof Alumno) {
                    enSesion = menuAlumno((Alumno) usuario);
                } else {
                    enSesion = false;
                }
            } catch (Exception e) {
                System.out.println("\n[Error de Operación] " + e.getMessage());
            }
        }
    }

    // --- MENÚ DEL DIRECTOR (RF16 - RF21) ---
    private static boolean menuDirector(Director director) throws Exception {
        System.out.println("\n====================================");
        System.out.println("        MENÚ DE DIRECTOR           ");
        System.out.println("====================================");
        System.out.println("1. Listar Estudiantes (Streams - filter)");
        System.out.println("2. Registrar Nuevo Estudiante (try-catch)");
        System.out.println("3. Registrar Nuevo Docente");
        System.out.println("4. Procesar Matrícula (Regla de negocio: valida deudas)");
        System.out.println("5. Generar Reporte Financiero (Streams - map/reduce)");
        System.out.println("6. Generar Reporte de Asistencia General");
        System.out.println("7. Cerrar Sesión");
        System.out.print("Seleccione una opción: ");

        String opcion = scanner.nextLine().trim();
        switch (opcion) {
            case "1": {
                System.out.println("\n--- LISTADO DE ESTUDIANTES ---");
                List<Alumno> alumnos = controller.getService().obtenerTodosLosAlumnos();
                // Uso de programación funcional: forEach y Lambda
                alumnos.forEach(al -> {
                    String desempeno = controller.getService().obtenerDesempenoGeneral(al.getId());
                    System.out.println("- [" + al.getId() + "] " + al.getNombre() + 
                                       " | Grado: " + (al.getGrado() == null ? "Sin Matricular" : al.getGrado() + " " + al.getSeccion()) + 
                                       " | Desempeño Académico: " + desempeno);
                });
                break;
            }
            case "2": {
                System.out.println("\n--- REGISTRAR ALUMNO ---");
                System.out.print("ID (ej. AL04): ");
                String id = scanner.nextLine().trim();
                System.out.print("Nombre Completo: ");
                String nombre = scanner.nextLine().trim();
                System.out.print("Usuario: ");
                String user = scanner.nextLine().trim();
                System.out.print("Contraseña: ");
                String pass = scanner.nextLine().trim();
                controller.registrarAlumno(id, user, pass, nombre, "", "");
                System.out.println("[Éxito] Estudiante registrado correctamente.");
                break;
            }
            case "3": {
                System.out.println("\n--- REGISTRAR DOCENTE ---");
                System.out.print("ID (ej. DOC03): ");
                String id = scanner.nextLine().trim();
                System.out.print("Nombre Completo: ");
                String nombre = scanner.nextLine().trim();
                System.out.print("Especialidad: ");
                String especialidad = scanner.nextLine().trim();
                System.out.print("Usuario: ");
                String user = scanner.nextLine().trim();
                System.out.print("Contraseña: ");
                String pass = scanner.nextLine().trim();
                controller.registrarDocente(id, user, pass, nombre, especialidad);
                System.out.println("[Éxito] Docente registrado correctamente.");
                break;
            }
            case "4": {
                System.out.println("\n--- REGISTRAR MATRÍCULA ---");
                System.out.print("ID del Alumno: ");
                String alumnoId = scanner.nextLine().trim();
                System.out.print("Grado (ej: 5to): ");
                String grado = scanner.nextLine().trim();
                System.out.print("Sección (ej: A): ");
                String seccion = scanner.nextLine().trim();
                
                // Intenta matricular. Lanza excepción si la regla de deudas es violada
                controller.registrarMatricula(alumnoId, grado, seccion);
                System.out.println("[Éxito] Matrícula aprobada. Grado y sección asignados.");
                break;
            }
            case "5": {
                System.out.println("\n--- REPORTE FINANCIERO DE PENSIONES ---");
                double total = controller.getService().calcularTotalRecaudado();
                System.out.println("Total Recaudado (Pensiones Pagadas): S/. " + total);
                
                // Streams para filtrar y contar
                long pendientes = controller.getService().obtenerPagosPendientes().size();
                System.out.println("Cantidad de Pensiones por Cobrar: " + pendientes);
                break;
            }
            case "6": {
                System.out.println("\n--- REPORTE DE ASISTENCIA GENERAL ---");
                double tasa = controller.getService().calcularTasaAsistenciaAlumnos();
                System.out.printf("Tasa de Asistencia Diaria de Alumnos: %.2f%%\n", tasa);
                break;
            }
            case "7":
                System.out.println("Cerrando sesión de director...");
                return false;
            default:
                System.out.println("Opción no válida.");
        }
        return true;
    }

    // --- MENÚ DEL DOCENTE (RF02 - RF08) ---
    private static boolean menuDocente(Docente docente) throws Exception {
        System.out.println("\n====================================");
        System.out.println("         MENÚ DE DOCENTE           ");
        System.out.println("====================================");
        System.out.println("1. Listar Cursos Asignados (Streams - filter)");
        System.out.println("2. Registrar Notas de Alumnos (Regla: conversión automática)");
        System.out.println("3. Registrar Asistencia de Alumnos");
        System.out.println("4. Crear Tarea");
        System.out.println("5. Calificar Tarea de Alumnos");
        System.out.println("6. Cerrar Sesión");
        System.out.print("Seleccione una opción: ");

        String opcion = scanner.nextLine().trim();
        switch (opcion) {
            case "1": {
                System.out.println("\n--- CURSOS ASIGNADOS ---");
                List<Curso> cursos = controller.obtenerCursosPorDocente(docente.getId());
                cursos.forEach(c -> System.out.println("- [" + c.getId() + "] " + c.getNombre() + " (Grado: " + c.getGrado() + ", Sección: " + c.getSeccion() + ")"));
                break;
            }
            case "2": {
                System.out.println("\n--- REGISTRAR NOTA ---");
                System.out.print("ID del Curso: ");
                String cursoId = scanner.nextLine().trim();
                System.out.print("ID del Alumno: ");
                String alumnoId = scanner.nextLine().trim();
                System.out.print("Nota Numérica (0-20): ");
                double notaNum = Double.parseDouble(scanner.nextLine().trim());
                System.out.print("Periodo (ej: Bimestre 1): ");
                String periodo = scanner.nextLine().trim();

                controller.registrarNota(alumnoId, cursoId, notaNum, periodo);
                System.out.println("[Éxito] Nota registrada. Se realizó la conversión automática a escala institucional de letras.");
                break;
            }
            case "3": {
                System.out.println("\n--- REGISTRAR ASISTENCIA ---");
                System.out.print("ID del Alumno: ");
                String alumnoId = scanner.nextLine().trim();
                System.out.print("Estado (ASISTENCIA / TARDANZA / FALTA): ");
                String estado = scanner.nextLine().trim().toUpperCase();
                System.out.print("Observación: ");
                String obs = scanner.nextLine().trim();
                System.out.print("Fecha (AAAA-MM-DD): ");
                String fecha = scanner.nextLine().trim();

                controller.registrarAsistencia(alumnoId, fecha, estado, obs, false);
                System.out.println("[Éxito] Asistencia del alumno registrada.");
                break;
            }
            case "4": {
                System.out.println("\n--- CREAR TAREA ---");
                System.out.print("ID del Curso: ");
                String cursoId = scanner.nextLine().trim();
                System.out.print("Título: ");
                String titulo = scanner.nextLine().trim();
                System.out.print("Descripción: ");
                String desc = scanner.nextLine().trim();
                System.out.print("Fecha Entrega (AAAA-MM-DD): ");
                String fecha = scanner.nextLine().trim();
                System.out.print("Archivo Adjunto: ");
                String archivo = scanner.nextLine().trim();

                controller.crearTarea(cursoId, titulo, desc, fecha, archivo);
                System.out.println("[Éxito] Tarea asignada exitosamente.");
                break;
            }
            case "5": {
                System.out.println("\n--- CALIFICAR TAREA ---");
                System.out.print("ID de la Tarea: ");
                String tareaId = scanner.nextLine().trim();
                System.out.print("ID del Alumno: ");
                String alumnoId = scanner.nextLine().trim();
                System.out.print("Calificación Letra (AD, A, B, C): ");
                String letra = scanner.nextLine().trim().toUpperCase();
                System.out.print("Retroalimentación: ");
                String retro = scanner.nextLine().trim();

                controller.calificarTarea(tareaId, alumnoId, letra, retro);
                System.out.println("[Éxito] Calificación de la tarea registrada.");
                break;
            }
            case "6":
                System.out.println("Cerrando sesión de docente...");
                return false;
            default:
                System.out.println("Opción no válida.");
        }
        return true;
    }

    // --- MENÚ DEL ALUMNO (RF09 - RF15) ---
    private static boolean menuAlumno(Alumno alumno) throws Exception {
        System.out.println("\n====================================");
        System.out.println("         MENÚ DE ALUMNO            ");
        System.out.println("====================================");
        System.out.println("1. Ver Cursos y Notas (Escala AD, A, B, C)");
        System.out.println("2. Consultar Nivel de Desempeño Académico General (Regla de negocio)");
        System.out.println("3. Enviar / Subir Tarea");
        System.out.println("4. Consultar Pensiones / Pagos");
        System.out.println("5. Cerrar Sesión");
        System.out.print("Seleccione una opción: ");

        String opcion = scanner.nextLine().trim();
        switch (opcion) {
            case "1": {
                System.out.println("\n--- MIS CURSOS Y NOTAS CALIFICADAS ---");
                List<Curso> misCursos = controller.obtenerCursosPorAlumno(alumno.getId());
                if (misCursos.isEmpty()) {
                    System.out.println("No se encuentra matriculado en ningún curso.");
                } else {
                    for (Curso c : misCursos) {
                        System.out.println("- Curso: " + c.getNombre() + " (" + c.getId() + ")");
                        List<Nota> notasCurso = controller.getService().obtenerNotasPorAlumno(alumno.getId()).stream()
                            .filter(n -> n.getIdCurso().equals(c.getId()))
                            .collect(Collectors.toList());
                        if (notasCurso.isEmpty()) {
                            System.out.println("  Sin notas calificadas.");
                        } else {
                            notasCurso.forEach(n -> System.out.println("  * " + n.getPeriodo() + ": " + n.getValorLetra() + " (" + n.getValorNumerico() + ")"));
                        }
                    }
                }
                break;
            }
            case "2": {
                System.out.println("\n--- EVALUACIÓN ACADÉMICA AUTOMÁTICA ---");
                String eval = controller.getService().obtenerDesempenoGeneral(alumno.getId());
                System.out.println("Su desempeño general es: " + eval);
                break;
            }
            case "3": {
                System.out.println("\n--- SUBIR TAREA ---");
                System.out.print("ID de la Tarea: ");
                String tareaId = scanner.nextLine().trim();
                System.out.print("Nombre del archivo a enviar (ej: mi_tarea.pdf): ");
                String archivo = scanner.nextLine().trim();
                System.out.print("Fecha de Envío (AAAA-MM-DD): ");
                String fecha = scanner.nextLine().trim();

                controller.enviarTarea(tareaId, alumno.getId(), archivo, fecha);
                System.out.println("[Éxito] Archivo subido y enviado correctamente.");
                break;
            }
            case "4": {
                System.out.println("\n--- MIS PENSIONES Y PAGOS ---");
                List<Pago> pagos = DataRepository.getInstance().getPagos().stream()
                    .filter(p -> p.getIdAlumno().equals(alumno.getId()))
                    .collect(Collectors.toList());

                pagos.forEach(p -> System.out.println("- Concepto: " + p.getConcepto() + " | Monto: S/. " + p.getMonto() + 
                                                   " | Estado: " + (p.isPagado() ? "PAGADO (El " + p.getFechaPago() + ")" : "PENDIENTE (Vence: " + p.getFechaVencimiento() + ")")));
                break;
            }
            case "5":
                System.out.println("Cerrando sesión de alumno...");
                return false;
            default:
                System.out.println("Opción no válida.");
        }
        return true;
    }
}
