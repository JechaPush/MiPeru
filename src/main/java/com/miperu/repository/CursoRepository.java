package com.miperu.repository;

import com.miperu.model.Curso;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class CursoRepository {
    private final List<Curso> cursos = new ArrayList<>();
    private final Map<String, Curso> cursosById = new HashMap<>();

    public void save(Curso c) {
        deleteById(c.getId());
        cursos.add(c);
        cursosById.put(c.getId(), c);
    }

    public void deleteById(String id) {
        Curso c = cursosById.remove(id);
        if (c != null) {
            cursos.remove(c);
        }
    }

    public List<Curso> findAll() {
        return cursos;
    }

    public Curso findById(String id) {
        return cursosById.get(id);
    }
}
