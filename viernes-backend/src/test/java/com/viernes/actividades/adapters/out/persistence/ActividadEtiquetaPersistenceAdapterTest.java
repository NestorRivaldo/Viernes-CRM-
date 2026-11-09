package com.viernes.actividades.adapters.out.persistence;

import com.viernes.actividades.adapters.out.persistence.entity.ActividadEtiquetaEntity;
import com.viernes.actividades.adapters.out.persistence.repository.ActividadEtiquetaSpringDataRepository;
import com.viernes.actividades.domain.ActividadEtiqueta;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActividadEtiquetaPersistenceAdapterTest {

    @Mock
    private ActividadEtiquetaSpringDataRepository repository;

    @InjectMocks
    private ActividadEtiquetaPersistenceAdapter adapter;

    @Test
    void reemplazarEtiquetasAsignaElOrdenRecibido() {
        adapter.reemplazarEtiquetas(10L, List.of(5L, 3L, 5L));

        verify(repository).deleteByIdActividad(10L);
        verify(repository).flush();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ActividadEtiquetaEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());

        List<ActividadEtiquetaEntity> guardadas = captor.getValue();
        assertEquals(2, guardadas.size());
        assertEquals(5L, guardadas.get(0).getIdEtiqueta());
        assertEquals(0, guardadas.get(0).getOrden().intValue());
        assertEquals(3L, guardadas.get(1).getIdEtiqueta());
        assertEquals(1, guardadas.get(1).getOrden().intValue());
    }

    @Test
    void findByIdActividadDevuelveElOrdenPersistido() {
        ActividadEtiquetaEntity primera = entidad(5L, 1);
        ActividadEtiquetaEntity segunda = entidad(3L, 2);

        when(repository.findByIdActividadOrderByOrdenAsc(10L)).thenReturn(List.of(primera, segunda));

        List<ActividadEtiqueta> resultado = adapter.findByIdActividad(10L);

        assertEquals(List.of(5L, 3L), resultado.stream().map(ActividadEtiqueta::getIdEtiqueta).toList());
        assertEquals(1, resultado.get(0).getOrden().intValue());
        assertEquals(2, resultado.get(1).getOrden().intValue());
    }

    private ActividadEtiquetaEntity entidad(Long idEtiqueta, int orden) {
        ActividadEtiquetaEntity entity = new ActividadEtiquetaEntity();
        entity.setIdActividad(10L);
        entity.setIdEtiqueta(idEtiqueta);
        entity.setCreadoEn(LocalDateTime.now());
        entity.setOrden(orden);
        return entity;
    }
}
