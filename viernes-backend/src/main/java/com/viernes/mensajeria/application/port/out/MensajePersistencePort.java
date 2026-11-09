package com.viernes.mensajeria.application.port.out;

import com.viernes.mensajeria.domain.Mensaje;

import java.util.List;

public interface MensajePersistencePort {

    List<Mensaje> findRecientes(int limite);

    List<Mensaje> findDespuesDeId(Long despuesDeId, int limite);

    long count();

    long countDespuesDeId(Long despuesDeId);

    Long findUltimoId();

    Mensaje save(Mensaje mensaje);
}
