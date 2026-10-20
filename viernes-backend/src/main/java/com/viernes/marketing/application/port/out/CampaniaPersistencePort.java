package com.viernes.marketing.application.port.out;

import com.viernes.marketing.domain.Campania;
import java.util.List;
import java.util.Optional;

public interface CampaniaPersistencePort {
    List<Campania> findAll();
    Optional<Campania> findById(Long id);
    Campania save(Campania campania);
    void deleteById(Long id);
}
