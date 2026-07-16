package com.miperu.repository;

import com.miperu.model.Tarea;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;

@Repository
public class TareaRepository {
    private final List<Tarea> tareas = new ArrayList<>();

    public void save(Tarea t) {
        tareas.removeIf(existing -> existing.getId().equals(t.getId()));
        tareas.add(t);
    }

    public List<Tarea> findAll() {
        return tareas;
    }

    public Tarea findById(String id) {
        return tareas.stream()
            .filter(t -> t.getId().equals(id))
            .findFirst()
            .orElse(null);
    }
}
