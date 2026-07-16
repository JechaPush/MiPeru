package com.miperu.controller;

import com.miperu.exception.EscuelaException;
import com.miperu.model.Alumno;
import com.miperu.model.Curso;
import com.miperu.model.Nota;
import com.miperu.model.Tarea;
import com.miperu.repository.TareaRepository;
import com.miperu.repository.AnuncioRepository;
import com.miperu.service.AlumnoService;
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
public class AlumnoController {
    private final AlumnoService alumnoService;
    private final TareaRepository tareaRepo;
    private final AnuncioRepository anuncioRepo;

    @Autowired
    public AlumnoController(AlumnoService alumnoService, TareaRepository tareaRepo, AnuncioRepository anuncioRepo) {
        this.alumnoService = alumnoService;
        this.tareaRepo = tareaRepo;
        this.anuncioRepo = anuncioRepo;
    }

    @GetMapping(value = "/dashboard", params = "rol=Alumno")
    public ResponseEntity<?> getDashboard(@RequestParam String userId) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);

        try {
            Alumno alumno = alumnoService.obtenerAlumnoPorId(userId);
            List<Curso> matriculados = alumnoService.obtenerCursosPorAlumno(userId);

            response.put("dashboard", Map.of(
                "desempenoGeneral", alumnoService.obtenerDesempenoGeneral(userId),
                "grado", alumno.getGrado() == null ? "" : alumno.getGrado(),
                "seccion", alumno.getSeccion() == null ? "" : alumno.getSeccion()
            ));

            List<Map<String, Object>> cursosJson = new ArrayList<>();
            for (Curso c : matriculados) {
                List<Nota> notasCurso = alumnoService.obtenerNotasPorAlumno(userId).stream()
                    .filter(n -> n.getIdCurso().equals(c.getId()))
                    .collect(Collectors.toList());

                List<Map<String, Object>> notasList = notasCurso.stream().map(n -> {
                    Map<String, Object> nMap = new HashMap<>();
                    nMap.put("periodo", n.getPeriodo());
                    nMap.put("letra", n.getValorLetra());
                    nMap.put("nota", n.getValorNumerico());
                    return nMap;
                }).collect(Collectors.toList());

                cursosJson.add(Map.of(
                    "id", c.getId(),
                    "nombre", c.getNombre(),
                    "notas", notasList
                ));
            }
            response.put("cursos", cursosJson);

            List<Tarea> tareas = tareaRepo.findAll().stream()
                .filter(t -> matriculados.stream().anyMatch(c -> c.getId().equals(t.getIdCurso())))
                .collect(Collectors.toList());

            List<Map<String, Object>> tareasJson = new ArrayList<>();
            for (Tarea t : tareas) {
                Tarea.Entrega e = t.getEntregas().get(userId);
                Object entregaVal = e == null ? null : Map.of(
                    "archivo", e.getArchivoEnviado(),
                    "fecha", e.getFechaEnvio(),
                    "calificacion", e.getCalificacion(),
                    "comentarios", e.getComentarios(),
                    "estado", e.getEstado()
                );
                Map<String, Object> map = new HashMap<>();
                map.put("id", t.getId());
                map.put("cursoId", t.getIdCurso());
                map.put("titulo", t.getTitulo());
                map.put("descripcion", t.getDescripcion());
                map.put("fechaEntrega", t.getFechaEntrega());
                map.put("archivoAdjunto", t.getArchivoAdjunto() == null ? "" : t.getArchivoAdjunto());
                map.put("entrega", entregaVal);
                tareasJson.add(map);
            }
            response.put("tareas", tareasJson);
            response.put("anuncios", anuncioRepo.findAll());
            response.put("pagos", alumnoService.obtenerPagosPorAlumno(userId));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping(value = "/action", params = "action=enviarTarea")
    public ResponseEntity<?> enviarTarea(@RequestParam Map<String, String> params) {
        try {
            String idTarea = params.get("idTarea");
            String idAlumno = params.get("idAlumno");
            String archivoEnviado = params.get("archivo");
            String fechaEnvio = params.get("fecha");
            alumnoService.enviarTarea(idTarea, idAlumno, archivoEnviado, fechaEnvio);
            return ResponseEntity.ok(Map.of("success", true, "message", "Tarea enviada exitosamente"));
        } catch (EscuelaException e) {
            return ResponseEntity.status(400).body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
