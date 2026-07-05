package Web;

import Controller.EscuelaController;
import Models.*;
import Exceptions.*;
import Repository.DataRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.*;
import java.util.stream.Collectors;

public class WebServer {
    private HttpServer server;
    private EscuelaController controller;
    private int port;
    private final Path staticRoot;

    public WebServer(EscuelaController controller, int port) {
        this.controller = controller;
        this.port = port;
        this.staticRoot = resolveStaticRoot();
    }

    private Path resolveStaticRoot() {
        Path cwd = Paths.get(System.getProperty("user.dir"));
        List<Path> candidates = List.of(
            cwd.resolve("src").resolve("Web").resolve("public"),
            cwd.resolve("MiPeru").resolve("src").resolve("Web").resolve("public"),
            cwd.resolve("public"),
            Paths.get("src", "Web", "public")
        );

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate.toAbsolutePath().normalize();
            }
        }

        return cwd.toAbsolutePath().normalize();
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new StaticFileHandler());
        server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/dashboard", new DashboardHandler());
        server.createContext("/api/action", new ActionHandler());
        server.setExecutor(null); // default executor
        server.start();
        System.out.println("[Web Server] Servidor web iniciado en http://localhost:" + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("[Web Server] Servidor web detenido.");
        }
    }

    // Helper to parse query parameters
    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return params;
        }
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                params.put(URLDecoder.decode(entry[0], StandardCharsets.UTF_8), URLDecoder.decode(entry[1], StandardCharsets.UTF_8));
            } else if (entry.length > 0) {
                params.put(URLDecoder.decode(entry[0], StandardCharsets.UTF_8), "");
            }
        }
        return params;
    }

    // Helper to send JSON responses
    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // Helper to send CORS preflight responses
    private static boolean handleOptions(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            exchange.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    // 1. Static Files Handler
    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            
            String pathStr = exchange.getRequestURI().getPath();
            if (pathStr.equals("/")) {
                pathStr = "/html/login.html";
            }

            // Path to web files
            Path filePath = staticRoot.resolve(pathStr.substring(1)).normalize();
            if ((!Files.exists(filePath) || Files.isDirectory(filePath)) && pathStr.startsWith("/html/html/")) {
                String normalizedPath = "/html/" + pathStr.substring("/html/html/".length());
                filePath = staticRoot.resolve(normalizedPath.substring(1)).normalize();
                pathStr = normalizedPath;
            }
            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                String errorMsg = "404 Not Found: " + pathStr;
                byte[] errorBytes = errorMsg.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                exchange.sendResponseHeaders(404, errorBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(errorBytes);
                }
                return;
            }

            String contentType = "text/html";
            if (pathStr.endsWith(".css")) {
                contentType = "text/css";
            } else if (pathStr.endsWith(".js")) {
                contentType = "application/javascript";
            } else if (pathStr.endsWith(".png")) {
                contentType = "image/png";
            } else if (pathStr.endsWith(".jpg") || pathStr.endsWith(".jpeg")) {
                contentType = "image/jpeg";
            } else if (pathStr.endsWith(".pdf")) {
                contentType = "application/pdf";
            }

            byte[] fileBytes = Files.readAllBytes(filePath);
            exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, fileBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(fileBytes);
            }
        }
    }

    // 2. API Handler: Login
    private class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"success\": false, \"message\": \"Method not allowed\"}");
                return;
            }

            // Read POST body
            InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            String body = br.lines().collect(Collectors.joining("\n"));
            Map<String, String> params = parseQueryParams(body);

            String username = params.get("username");
            String password = params.get("password");

            try {
                Usuario user = controller.login(username, password);
                String json = String.format(
                    "{\"success\": true, \"userId\": \"%s\", \"nombre\": \"%s\", \"rol\": \"%s\"}",
                    user.getId(), user.getNombre(), user.getRol()
                );
                sendJsonResponse(exchange, 200, json);
            } catch (LoginException e) {
                sendJsonResponse(exchange, 401, "{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    // 3. API Handler: Dashboard Data
    private class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;

            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            String userId = params.get("userId");
            String rol = params.get("rol");

            if (userId == null || rol == null) {
                sendJsonResponse(exchange, 400, "{\"success\": false, \"message\": \"Faltan parámetros userId y rol\"}");
                return;
            }

            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{\"success\": true,");

            try {
                if ("Director".equalsIgnoreCase(rol)) {
                    // Módulo del Director: RF16 Dashboard
                    long totalAlumnos = controller.getService().obtenerTodosLosAlumnos().size();
                    long totalDocentes = controller.getService().obtenerTodosLosDocentes().size();
                    long totalCursos = DataRepository.getInstance().getCursos().size();
                    
                    // Calcular matrícula
                    long matriculados = controller.getService().obtenerTodosLosAlumnos().stream()
                        .filter(a -> a.getGrado() != null && !a.getGrado().isEmpty()).count();

                    // Pagos recaudados y pendientes
                    double totalRecaudado = controller.getService().calcularTotalRecaudado();
                    long pagosPendientesCount = controller.getService().obtenerPagosPendientes().size();

                    // Asistencia docente
                    long asistenciasDocentesHoy = DataRepository.getInstance().getAsistencias().stream()
                        .filter(Asistencia::isEsDocente)
                        .filter(a -> a.getEstado().equals("ASISTENCIA"))
                        .count();

                    jsonBuilder.append("\"dashboard\": {")
                        .append("\"totalAlumnos\": ").append(totalAlumnos).append(",")
                        .append("\"totalDocentes\": ").append(totalDocentes).append(",")
                        .append("\"totalCursos\": ").append(totalCursos).append(",")
                        .append("\"matriculados\": ").append(matriculados).append(",")
                        .append("\"totalRecaudado\": ").append(totalRecaudado).append(",")
                        .append("\"pagosPendientes\": ").append(pagosPendientesCount).append(",")
                        .append("\"asistenciaDocenteHoy\": ").append(asistenciasDocentesHoy)
                        .append("},");

                    // Listado de Alumnos
                    jsonBuilder.append("\"alumnos\": [");
                    List<Alumno> alumnos = controller.getService().obtenerTodosLosAlumnos();
                    for (int i = 0; i < alumnos.size(); i++) {
                        Alumno a = alumnos.get(i);
                        String desempeño = controller.getService().obtenerDesempenoGeneral(a.getId());
                        jsonBuilder.append(String.format(
                            "{\"id\": \"%s\", \"nombre\": \"%s\", \"grado\": \"%s\", \"seccion\": \"%s\", \"desempeno\": \"%s\"}",
                            a.getId(), a.getNombre(), a.getGrado(), a.getSeccion(), desempeño
                        ));
                        if (i < alumnos.size() - 1) jsonBuilder.append(",");
                    }
                    jsonBuilder.append("],");

                    // Listado de Docentes
                    jsonBuilder.append("\"docentes\": [");
                    List<Docente> docentes = controller.getService().obtenerTodosLosDocentes();
                    for (int i = 0; i < docentes.size(); i++) {
                        Docente d = docentes.get(i);
                        jsonBuilder.append(String.format(
                            "{\"id\": \"%s\", \"nombre\": \"%s\", \"especialidad\": \"%s\"}",
                            d.getId(), d.getNombre(), d.getEspecialidad()
                        ));
                        if (i < docentes.size() - 1) jsonBuilder.append(",");
                    }
                    jsonBuilder.append("],");

                    // Listado de Pagos
                    jsonBuilder.append("\"pagos\": [");
                    List<Pago> pagos = DataRepository.getInstance().getPagos();
                    for (int i = 0; i < pagos.size(); i++) {
                        Pago p = pagos.get(i);
                        Alumno a = (Alumno) DataRepository.getInstance().getUsuariosById().get(p.getIdAlumno());
                        jsonBuilder.append(String.format(Locale.US,
                            "{\"id\": \"%s\", \"alumno\": \"%s\", \"monto\": %f, \"concepto\": \"%s\", \"pagado\": %b, \"vencimiento\": \"%s\", \"fechaPago\": \"%s\"}",
                            p.getId(), (a != null ? a.getNombre() : "Desconocido"), p.getMonto(), p.getConcepto(), p.isPagado(), p.getFechaVencimiento(), p.getFechaPago()
                        ));
                        if (i < pagos.size() - 1) jsonBuilder.append(",");
                    }
                    jsonBuilder.append("]");

                } else if ("Profesor".equalsIgnoreCase(rol)) {
                    // Módulo del Docente: RF02 Dashboard
                    List<Curso> cursos = controller.obtenerCursosPorDocente(userId);
                    
                    jsonBuilder.append("\"cursos\": [");
                    for (int i = 0; i < cursos.size(); i++) {
                        Curso c = cursos.get(i);
                        long cantAlumnos = controller.getService().obtenerAlumnosPorCurso(c.getId()).size();
                        jsonBuilder.append(String.format(
                            "{\"id\": \"%s\", \"nombre\": \"%s\", \"grado\": \"%s\", \"seccion\": \"%s\", \"alumnosCount\": %d}",
                            c.getId(), c.getNombre(), c.getGrado(), c.getSeccion(), cantAlumnos
                        ));
                        if (i < cursos.size() - 1) jsonBuilder.append(",");
                    }
                    jsonBuilder.append("],");

                    // Tareas pendientes
                    List<Tarea> tareas = DataRepository.getInstance().getTareas().stream()
                        .filter(t -> {
                            Curso c = DataRepository.getInstance().getCursosById().get(t.getIdCurso());
                            return c != null && c.getIdDocente().equals(userId);
                        }).collect(Collectors.toList());

                    jsonBuilder.append("\"tareas\": [");
                    for (int i = 0; i < tareas.size(); i++) {
                        Tarea t = tareas.get(i);
                        long entregadas = t.getEntregas().values().stream().filter(e -> e.getEstado().equals("ENVIADO")).count();
                        jsonBuilder.append(String.format(
                            "{\"id\": \"%s\", \"cursoId\": \"%s\", \"titulo\": \"%s\", \"fechaEntrega\": \"%s\", \"entregasPendientes\": %d}",
                            t.getId(), t.getIdCurso(), t.getTitulo(), t.getFechaEntrega(), entregadas
                        ));
                        if (i < tareas.size() - 1) jsonBuilder.append(",");
                    }
                    jsonBuilder.append("],");

                    // Anuncios
                    jsonBuilder.append("\"anuncios\": [");
                    List<Anuncio> anuncios = DataRepository.getInstance().getAnuncios();
                    for (int i = 0; i < anuncios.size(); i++) {
                        Anuncio a = anuncios.get(i);
                        jsonBuilder.append(String.format(
                            "{\"id\": \"%s\", \"titulo\": \"%s\", \"contenido\": \"%s\", \"fecha\": \"%s\", \"autor\": \"%s\"}",
                            a.getId(), a.getTitulo(), a.getContenido(), a.getFechaPublicacion(), a.getAutor()
                        ));
                        if (i < anuncios.size() - 1) jsonBuilder.append(",");
                    }
                    jsonBuilder.append("],");

                    // Calendario / Horario
                    jsonBuilder.append("\"horarios\": [");
                    List<Horario> horarios = DataRepository.getInstance().getHorarios().stream()
                        .filter(h -> {
                            Curso c = DataRepository.getInstance().getCursosById().get(h.getIdCurso());
                            return c != null && c.getIdDocente().equals(userId);
                        }).collect(Collectors.toList());

                    for (int i = 0; i < horarios.size(); i++) {
                        Horario h = horarios.get(i);
                        Curso c = DataRepository.getInstance().getCursosById().get(h.getIdCurso());
                        jsonBuilder.append(String.format(
                            "{\"cursoNombre\": \"%s\", \"dia\": \"%s\", \"inicio\": \"%s\", \"fin\": \"%s\", \"aula\": \"%s\"}",
                            (c != null ? c.getNombre() : ""), h.getDiaSemana(), h.getHoraInicio(), h.getHoraFin(), h.getAula()
                        ));
                        if (i < horarios.size() - 1) jsonBuilder.append(",");
                    }
                    jsonBuilder.append("]");

                } else if ("Alumno".equalsIgnoreCase(rol)) {
                    // Módulo del Alumno: RF09 Dashboard
                    Alumno alumno = controller.getService().obtenerAlumnoPorId(userId);
                    List<Curso> matriculados = controller.obtenerCursosPorAlumno(userId);

                    jsonBuilder.append("\"dashboard\": {")
                        .append("\"desempenoGeneral\": \"").append(controller.getService().obtenerDesempenoGeneral(userId)).append("\",")
                        .append("\"grado\": \"").append(alumno.getGrado()).append("\",")
                        .append("\"seccion\": \"").append(alumno.getSeccion()).append("\"")
                        .append("},");

                    // Cursos y notas
                    jsonBuilder.append("\"cursos\": [");
                    for (int i = 0; i < matriculados.size(); i++) {
                        Curso c = matriculados.get(i);
                        List<Nota> notasCurso = controller.getService().obtenerNotasPorAlumno(userId).stream()
                            .filter(n -> n.getIdCurso().equals(c.getId()))
                            .collect(Collectors.toList());

                        String notasJson = notasCurso.stream()
                            .map(n -> String.format(Locale.US, "{\"periodo\": \"%s\", \"letra\": \"%s\", \"nota\": %.1f}", n.getPeriodo(), n.getValorLetra(), n.getValorNumerico()))
                            .collect(Collectors.joining(",", "[", "]"));

                        jsonBuilder.append(String.format(
                            "{\"id\": \"%s\", \"nombre\": \"%s\", \"notas\": %s}",
                            c.getId(), c.getNombre(), notasJson
                        ));
                        if (i < matriculados.size() - 1) jsonBuilder.append(",");
                    }
                    jsonBuilder.append("],");

                    // Próximas tareas
                    List<Tarea> tareas = DataRepository.getInstance().getTareas().stream()
                        .filter(t -> matriculados.stream().anyMatch(c -> c.getId().equals(t.getIdCurso())))
                        .collect(Collectors.toList());

                    jsonBuilder.append("\"tareas\": [");
                    for (int i = 0; i < tareas.size(); i++) {
                        Tarea t = tareas.get(i);
                        Tarea.Entrega e = t.getEntregas().get(userId);
                        String entregaJson = e == null ? "null" : String.format(
                            "{\"archivo\": \"%s\", \"fecha\": \"%s\", \"calificacion\": \"%s\", \"comentarios\": \"%s\", \"estado\": \"%s\"}",
                            e.getArchivoEnviado(), e.getFechaEnvio(), e.getCalificacion(), e.getComentarios(), e.getEstado()
                        );
                        jsonBuilder.append(String.format(
                            "{\"id\": \"%s\", \"cursoId\": \"%s\", \"titulo\": \"%s\", \"descripcion\": \"%s\", \"fechaEntrega\": \"%s\", \"archivoAdjunto\": \"%s\", \"entrega\": %s}",
                            t.getId(), t.getIdCurso(), t.getTitulo(), t.getDescripcion(), t.getFechaEntrega(), t.getArchivoAdjunto(), entregaJson
                        ));
                        if (i < tareas.size() - 1) jsonBuilder.append(",");
                    }
                    jsonBuilder.append("],");

                    // Anuncios
                    jsonBuilder.append("\"anuncios\": [");
                    List<Anuncio> anuncios = DataRepository.getInstance().getAnuncios();
                    for (int i = 0; i < anuncios.size(); i++) {
                        Anuncio a = anuncios.get(i);
                        jsonBuilder.append(String.format(
                            "{\"id\": \"%s\", \"titulo\": \"%s\", \"contenido\": \"%s\", \"fecha\": \"%s\", \"autor\": \"%s\"}",
                            a.getId(), a.getTitulo(), a.getContenido(), a.getFechaPublicacion(), a.getAutor()
                        ));
                        if (i < anuncios.size() - 1) jsonBuilder.append(",");
                    }
                    jsonBuilder.append("],");

                    // Pagos del Alumno
                    jsonBuilder.append("\"pagos\": [");
                    List<Pago> alumnoPagos = DataRepository.getInstance().getPagos().stream()
                        .filter(p -> p.getIdAlumno().equals(userId))
                        .collect(Collectors.toList());
                    for (int i = 0; i < alumnoPagos.size(); i++) {
                        Pago p = alumnoPagos.get(i);
                        jsonBuilder.append(String.format(Locale.US,
                            "{\"id\": \"%s\", \"monto\": %f, \"concepto\": \"%s\", \"pagado\": %b, \"vencimiento\": \"%s\", \"fechaPago\": \"%s\"}",
                            p.getId(), p.getMonto(), p.getConcepto(), p.isPagado(), p.getFechaVencimiento(), p.getFechaPago()
                        ));
                        if (i < alumnoPagos.size() - 1) jsonBuilder.append(",");
                    }
                    jsonBuilder.append("]");
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, 500, "{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
                return;
            }

            jsonBuilder.append("}");
            sendJsonResponse(exchange, 200, jsonBuilder.toString());
        }
    }

    // 4. API Handler: Actions (POST/GET)
    private class ActionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;

            Map<String, String> params = new HashMap<>();
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(isr);
                String body = br.lines().collect(Collectors.joining("\n"));
                params = parseQueryParams(body);
            } else {
                params = parseQueryParams(exchange.getRequestURI().getQuery());
            }

            String action = params.get("action");
            if (action == null) {
                sendJsonResponse(exchange, 400, "{\"success\": false, \"message\": \"Falta el parámetro action\"}");
                return;
            }

            try {
                switch (action) {
                    case "registrarNota": {
                        // RF04
                        String idAlumno = params.get("idAlumno");
                        String idCurso = params.get("idCurso");
                        double valorNumerico = Double.parseDouble(params.get("valor"));
                        String periodo = params.get("periodo");
                        controller.registrarNota(idAlumno, idCurso, valorNumerico, periodo);
                        sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"Nota registrada exitosamente\"}");
                        break;
                    }
                    case "registrarAsistencia": {
                        // RF05
                        String idUsuario = params.get("idUsuario");
                        String fecha = params.get("fecha");
                        String estado = params.get("estado");
                        String observacion = params.get("observacion");
                        boolean esDocente = Boolean.parseBoolean(params.get("esDocente"));
                        controller.registrarAsistencia(idUsuario, fecha, estado, observacion, esDocente);
                        sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"Asistencia registrada exitosamente\"}");
                        break;
                    }
                    case "crearTarea": {
                        // RF06
                        String idCurso = params.get("idCurso");
                        String titulo = params.get("titulo");
                        String descripcion = params.get("descripcion");
                        String fechaEntrega = params.get("fechaEntrega");
                        String archivoAdjunto = params.get("archivo");
                        controller.crearTarea(idCurso, titulo, descripcion, fechaEntrega, archivoAdjunto);
                        sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"Tarea creada exitosamente\"}");
                        break;
                    }
                    case "enviarTarea": {
                        // RF11
                        String idTarea = params.get("idTarea");
                        String idAlumno = params.get("idAlumno");
                        String archivoEnviado = params.get("archivo");
                        String fechaEnvio = params.get("fecha");
                        controller.enviarTarea(idTarea, idAlumno, archivoEnviado, fechaEnvio);
                        sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"Tarea enviada exitosamente\"}");
                        break;
                    }
                    case "calificarTarea": {
                        // RF06
                        String idTarea = params.get("idTarea");
                        String idAlumno = params.get("idAlumno");
                        String calificacion = params.get("calificacion");
                        String comentarios = params.get("comentarios");
                        controller.calificarTarea(idTarea, idAlumno, calificacion, comentarios);
                        sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"Tarea calificada exitosamente\"}");
                        break;
                    }
                    case "publicarAnuncio": {
                        // RF08
                        String titulo = params.get("titulo");
                        String contenido = params.get("contenido");
                        String fecha = params.get("fecha");
                        String autor = params.get("autor");
                        controller.publicarAnuncio(titulo, contenido, fecha, autor);
                        sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"Anuncio publicado exitosamente\"}");
                        break;
                    }
                    case "registrarDocente": {
                        // RF17
                        String id = params.get("id");
                        String username = params.get("username");
                        String password = params.get("password");
                        String nombre = params.get("nombre");
                        String especialidad = params.get("especialidad");
                        controller.registrarDocente(id, username, password, nombre, especialidad);
                        sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"Docente registrado exitosamente\"}");
                        break;
                    }
                    case "registrarAlumno": {
                        // RF18
                        String id = params.get("id");
                        String username = params.get("username");
                        String password = params.get("password");
                        String nombre = params.get("nombre");
                        String grado = params.get("grado");
                        String seccion = params.get("seccion");
                        controller.registrarAlumno(id, username, password, nombre, grado, seccion);
                        sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"Alumno registrado exitosamente\"}");
                        break;
                    }
                    case "registrarMatricula": {
                        // RF19: Regla de Matrícula (valida deudas)
                        String idAlumno = params.get("idAlumno");
                        String grado = params.get("grado");
                        String seccion = params.get("seccion");
                        controller.registrarMatricula(idAlumno, grado, seccion);
                        sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"Matrícula registrada exitosamente\"}");
                        break;
                    }
                    case "pagar": {
                        // RF20
                        String idPago = params.get("idPago");
                        String fechaPago = params.get("fecha");
                        controller.realizarPago(idPago, fechaPago);
                        sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"Pago realizado exitosamente\"}");
                        break;
                    }
                    case "eliminarUsuario": {
                        // RF17, RF18
                        String id = params.get("id");
                        controller.eliminarUsuario(id);
                        sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"Usuario eliminado exitosamente\"}");
                        break;
                    }
                    default:
                        sendJsonResponse(exchange, 400, "{\"success\": false, \"message\": \"Acción desconocida: " + action + "\"}");
                }
            } catch (EscuelaException e) {
                sendJsonResponse(exchange, 400, "{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, 500, "{\"success\": false, \"message\": \"Error interno: " + e.toString() + "\"}");
            }
        }
    }
}
