package com.miperu.controller;

import com.miperu.exception.EscuelaException;
import com.miperu.model.Usuario;
import com.miperu.repository.CursoRepository;
import com.miperu.repository.AsistenciaRepository;
import com.miperu.repository.PagoRepository;
import com.miperu.repository.UsuarioRepository;
import com.miperu.service.AlumnoService;
import com.miperu.service.DirectorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class DirectorController {
    private final DirectorService directorService;
    private final AlumnoService alumnoService;
    private final CursoRepository cursoRepo;
    private final AsistenciaRepository asistenciaRepo;
    private final PagoRepository pagoRepo;
    private final UsuarioRepository usuarioRepo;

    @Autowired
    public DirectorController(DirectorService directorService, AlumnoService alumnoService,
                              CursoRepository cursoRepo, AsistenciaRepository asistenciaRepo,
                              PagoRepository pagoRepo, UsuarioRepository usuarioRepo) {
        this.directorService = directorService;
        this.alumnoService = alumnoService;
        this.cursoRepo = cursoRepo;
        this.asistenciaRepo = asistenciaRepo;
        this.pagoRepo = pagoRepo;
        this.usuarioRepo = usuarioRepo;
    }

    @GetMapping(value = "/dashboard", params = "rol=Director")
    public ResponseEntity<?> getDashboard(@RequestParam String userId) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);

        try {
            long totalAlumnos = directorService.obtenerTodosLosAlumnos().size();
            long totalDocentes = directorService.obtenerTodosLosDocentes().size();
            long totalCursos = cursoRepo.findAll().size();

            long matriculados = directorService.obtenerTodosLosAlumnos().stream()
                .filter(a -> a.getGrado() != null && !a.getGrado().isEmpty()).count();

            double totalRecaudado = directorService.calcularTotalRecaudado();
            long pagosPendientesCount = directorService.obtenerPagosPendientes().size();

            long asistenciasDocentesHoy = asistenciaRepo.findAll().stream()
                .filter(a -> a.isEsDocente() && a.getEstado().equals("ASISTENCIA"))
                .count();

            response.put("dashboard", Map.of(
                "totalAlumnos", totalAlumnos,
                "totalDocentes", totalDocentes,
                "totalCursos", totalCursos,
                "matriculados", matriculados,
                "totalRecaudado", totalRecaudado,
                "pagosPendientes", pagosPendientesCount,
                "asistenciaDocenteHoy", asistenciasDocentesHoy
            ));

            List<Map<String, Object>> alumnosJson = directorService.obtenerTodosLosAlumnos().stream().map(a -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", a.getId());
                map.put("nombre", a.getNombre());
                map.put("grado", a.getGrado());
                map.put("seccion", a.getSeccion());
                map.put("desempeno", alumnoService.obtenerDesempenoGeneral(a.getId()));
                return map;
            }).collect(Collectors.toList());
            response.put("alumnos", alumnosJson);

            List<Map<String, Object>> docentesJson = directorService.obtenerTodosLosDocentes().stream().map(d -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", d.getId());
                map.put("nombre", d.getNombre());
                map.put("especialidad", d.getEspecialidad());
                return map;
            }).collect(Collectors.toList());
            response.put("docentes", docentesJson);

            List<Map<String, Object>> pagosJson = pagoRepo.findAll().stream().map(p -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", p.getId());
                Usuario u = usuarioRepo.findById(p.getIdAlumno());
                map.put("alumno", u != null ? u.getNombre() : "Desconocido");
                map.put("monto", p.getMonto());
                map.put("concepto", p.getConcepto());
                map.put("pagado", p.isPagado());
                map.put("vencimiento", p.getFechaVencimiento());
                map.put("fechaPago", p.getFechaPago());
                return map;
            }).collect(Collectors.toList());
            response.put("pagos", pagosJson);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping(value = "/action", params = "action=registrarDocente")
    public ResponseEntity<?> registrarDocente(@RequestParam Map<String, String> params) {
        try {
            String id = params.get("id");
            String username = params.get("username");
            String password = params.get("password");
            String nombre = params.get("nombre");
            String especialidad = params.get("especialidad");
            directorService.registrarDocente(id, username, password, nombre, especialidad);
            return ResponseEntity.ok(Map.of("success", true, "message", "Docente registrado exitosamente"));
        } catch (EscuelaException e) {
            return ResponseEntity.status(400).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping(value = "/action", params = "action=registrarAlumno")
    public ResponseEntity<?> registrarAlumno(@RequestParam Map<String, String> params) {
        try {
            String id = params.get("id");
            String username = params.get("username");
            String password = params.get("password");
            String nombre = params.get("nombre");
            String grado = params.get("grado");
            String seccion = params.get("seccion");
            directorService.registrarAlumno(id, username, password, nombre, grado, seccion);
            return ResponseEntity.ok(Map.of("success", true, "message", "Alumno registrado exitosamente"));
        } catch (EscuelaException e) {
            return ResponseEntity.status(400).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping(value = "/action", params = "action=registrarMatricula")
    public ResponseEntity<?> registrarMatricula(@RequestParam Map<String, String> params) {
        try {
            String idAlumno = params.get("idAlumno");
            String grado = params.get("grado");
            String seccion = params.get("seccion");
            directorService.registrarMatricula(idAlumno, grado, seccion);
            return ResponseEntity.ok(Map.of("success", true, "message", "Matrícula registrada exitosamente"));
        } catch (EscuelaException e) {
            return ResponseEntity.status(400).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping(value = "/action", params = "action=pagar")
    public ResponseEntity<?> realizarPago(@RequestParam Map<String, String> params) {
        try {
            String idPago = params.get("idPago");
            String fechaPago = params.get("fecha");
            directorService.realizarPago(idPago, fechaPago);
            return ResponseEntity.ok(Map.of("success", true, "message", "Pago realizado exitosamente"));
        } catch (EscuelaException e) {
            return ResponseEntity.status(400).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping(value = "/action", params = "action=eliminarUsuario")
    public ResponseEntity<?> eliminarUsuario(@RequestParam Map<String, String> params) {
        try {
            String id = params.get("id");
            directorService.eliminarUsuario(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Usuario eliminado exitosamente"));
        } catch (EscuelaException e) {
            return ResponseEntity.status(400).body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
