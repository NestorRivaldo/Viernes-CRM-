package com.viernes.leads.application.port.out;

import com.viernes.leads.domain.Fuente;
import java.util.List;
import java.util.Optional;

public interface FuentePersistencePort {
    List<Fuente> findAll();
    List<Fuente> findByActiva(Boolean activa);
    Optional<Fuente> findById(Long id);
    boolean existsByCodigo(String codigo);
    Fuente save(Fuente fuente);
    void deleteById(Long id);
}
