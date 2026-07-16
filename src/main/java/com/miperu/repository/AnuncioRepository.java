package com.miperu.repository;

import com.miperu.model.Anuncio;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AnuncioRepository {
    private final List<Anuncio> anuncios = new ArrayList<>();

    public void save(Anuncio a) {
        anuncios.removeIf(existing -> existing.getId().equals(a.getId()));
        anuncios.add(a);
    }

    public List<Anuncio> findAll() {
        return anuncios;
    }
}
