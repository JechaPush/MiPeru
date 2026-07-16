package com.miperu.controller;

import com.miperu.exception.EscuelaException;
import com.miperu.model.Curso;
import com.miperu.model.Tarea;
import com.miperu.repository.TareaRepository;
import com.miperu.repository.AnuncioRepository;
import com.miperu.repository.CursoRepository;
import com.miperu.service.DocenteService;
import com.miperu.service.DirectorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class DocenteController {
    private final DocenteService docenteService;
    private final DirectorService directorService;
    private final TareaRepository tareaRepo;
    private final AnuncioRepository anuncioRepo;
    private final CursoRepository cursoRepo;

    @Autowired
    public DocenteController(DocenteService docenteService, DirectorService directorService,
                             TareaRepository tareaRepo, AnuncioRepository anuncioRepo, CursoRepository cursoRepo) {
        this.docenteService = docenteService;
        this.directorService = directorService;
        this.tareaRepo = tareaRepo;
        this.anuncioRepo = anuncioRepo;
        this.cursoRepo = cursoRepo;
    }

    @GetMapping(value = "/dashboard", params = "rol=Profesor")
    public ResponseEntity<?> getDashboard(@RequestParam String userId) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);

        try {
            List<Curso> cursos = docenteService.obtenerCursosPorDocente(userId);
            List<Map<String, Object>> cursosJson = new ArrayList<>();
            for (Curso c : cursos) {
                long cantAlumnos = directorService.obtenerTodosLosAlumnos().stream()
                    .filter(a -> a.getGrado().equals(c.getGrado()) && a.getSeccion().equals(c.getSeccion()))
                    .count();
                cursosJson.add(Map.of(
                    "id", c.getId(),
                    "nombre", c.getNombre(),
                    "grado", c.getGrado(),
                    "seccion", c.getSeccion(),
                    "alumnosCount", cantAlumnos
                ));
            }
            response.put("cursos", cursosJson);

            List<Tarea> tareas = tareaRepo.findAll().stream()
                .filter(t -> {
                    Curso c = cursoRepo.findById(t.getIdCurso());
                    return c != null && c.getIdDocente().equals(userId);
                }).collect(Collectors.toList());

            List<Map<String, Object>> tareasJson = new ArrayList<>();
            for (Tarea t : tareas) {
                long entregadas = t.getEntregas().values().stream().filter(e -> e.getEstado().equals("ENVIADO")).count();
                Map<String, Object> map = new HashMap<>();
                map.put("id", t.getId());
                map.put("cursoId", t.getIdCurso());
                map.put("titulo", t.getTitulo());
                map.put("fechaEntrega", t.getFechaEntrega());
                map.put("entregasPendientes", entregadas);
                map.put("entregas", t.getEntregas());
                tareasJson.add(map);
            }
            response.put("tareas", tareasJson);

            response.put("anuncios", anuncioRepo.findAll());

            List<Map<String, Object>> horariosJson = docenteService.obtenerHorariosPorDocente(userId).stream().map(h -> {
                Curso c = cursoRepo.findById(h.getIdCurso());
                Map<String, Object> hMap = new HashMap<>();
                hMap.put("cursoNombre", c != null ? c.getNombre() : "");
                hMap.put("dia", h.getDiaSemana());
                hMap.put("inicio", h.getHoraInicio());
                hMap.put("fin", h.getHoraFin());
                hMap.put("aula", h.getAula());
                return hMap;
            }).collect(Collectors.toList());
            response.put("horarios", horariosJson);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping(value = "/action", params = "action=registrarNota")
    public ResponseEntity<?> registrarNota(@RequestParam Map<String, String> params) {
        try {
            String idAlumno = params.get("idAlumno");
            String idCurso = params.get("idCurso");
            double valorNumerico = Double.parseDouble(params.get("valor"));
            String periodo = params.get("periodo");
            docenteService.registrarNota(idAlumno, idCurso, valorNumerico, periodo);
            return ResponseEntity.ok(Map.of("success", true, "message", "Nota registrada exitosamente"));
        } catch (EscuelaException e) {
            return ResponseEntity.status(400).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping(value = "/action", params = "action=registrarAsistencia")
    public ResponseEntity<?> registrarAsistencia(@RequestParam Map<String, String> params) {
        try {
            String idUsuario = params.get("idUsuario");
            String fecha = params.get("fecha");
            String estado = params.get("estado");
            String observacion = params.get("observacion");
            boolean esDocente = Boolean.parseBoolean(params.get("esDocente"));
            docenteService.registrarAsistencia(idUsuario, fecha, estado, observacion, esDocente);
            return ResponseEntity.ok(Map.of("success", true, "message", "Asistencia registrada exitosamente"));
        } catch (EscuelaException e) {
            return ResponseEntity.status(400).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping(value = "/action", params = "action=crearTarea")
    public ResponseEntity<?> crearTarea(@RequestParam Map<String, String> params) {
        try {
            String idCurso = params.get("idCurso");
            String titulo = params.get("titulo");
            String descripcion = params.get("descripcion");
            String fechaEntrega = params.get("fechaEntrega");
            String archivoAdjunto = params.get("archivo");
            docenteService.crearTarea(idCurso, titulo, descripcion, fechaEntrega, archivoAdjunto);
            return ResponseEntity.ok(Map.of("success", true, "message", "Tarea creada exitosamente"));
        } catch (EscuelaException e) {
            return ResponseEntity.status(400).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping(value = "/action", params = "action=calificarTarea")
    public ResponseEntity<?> calificarTarea(@RequestParam Map<String, String> params) {
        try {
            String idTarea = params.get("idTarea");
            String idAlumno = params.get("idAlumno");
            String calificacion = params.get("calificacion");
            String comentarios = params.get("comentarios");
            docenteService.calificarTarea(idTarea, idAlumno, calificacion, comentarios);
            return ResponseEntity.ok(Map.of("success", true, "message", "Tarea calificada exitosamente"));
        } catch (EscuelaException e) {
            return ResponseEntity.status(400).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping(value = "/action", params = "action=publicarAnuncio")
    public ResponseEntity<?> publicarAnuncio(@RequestParam Map<String, String> params) {
        String titulo = params.get("titulo");
        String contenido = params.get("contenido");
        String fecha = params.get("fecha");
        String autor = params.get("autor");
        docenteService.publicarAnuncio(titulo, contenido, fecha, autor);
        return ResponseEntity.ok(Map.of("success", true, "message", "Anuncio publicado exitosamente"));
    }
}
