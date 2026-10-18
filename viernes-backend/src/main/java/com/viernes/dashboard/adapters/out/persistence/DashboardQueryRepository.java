package com.viernes.dashboard.adapters.out.persistence;

import com.viernes.dashboard.application.dto.EstadoCantidadResponse;
import com.viernes.dashboard.application.dto.EtapaCantidadValorResponse;
import com.viernes.dashboard.application.dto.TipoCantidadResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Repository
public class DashboardQueryRepository {

    @PersistenceContext
    private EntityManager em;

    public long countClientes() {
        return (long) em.createQuery("SELECT COUNT(c) FROM ClienteEntity c").getSingleResult();
    }

    public long countLeads() {
        return (long) em.createQuery("SELECT COUNT(l) FROM LeadEntity l").getSingleResult();
    }

    public List<EstadoCantidadResponse> countLeadsPorEstado() {
        List<Object[]> results = em.createQuery("SELECT l.estado, COUNT(l) FROM LeadEntity l GROUP BY l.estado", Object[].class).getResultList();
        return results.stream().map(r -> new EstadoCantidadResponse(r[0].toString(), (long) r[1])).collect(Collectors.toList());
    }

    public long countOportunidades() {
        return (long) em.createQuery("SELECT COUNT(o) FROM OportunidadEntity o").getSingleResult();
    }

    public long countOportunidadesAbiertas() {
        return (long) em.createQuery("SELECT COUNT(o) FROM OportunidadEntity o WHERE o.etapa NOT IN ('GANADA', 'PERDIDA')").getSingleResult();
    }

    public long countOportunidadesGanadas() {
        return (long) em.createQuery("SELECT COUNT(o) FROM OportunidadEntity o WHERE o.etapa = 'GANADA'").getSingleResult();
    }

    public long countOportunidadesPerdidas() {
        return (long) em.createQuery("SELECT COUNT(o) FROM OportunidadEntity o WHERE o.etapa = 'PERDIDA'").getSingleResult();
    }

    public BigDecimal sumValorPipeline() {
        BigDecimal sum = (BigDecimal) em.createQuery("SELECT SUM(o.valor) FROM OportunidadEntity o WHERE o.etapa NOT IN ('GANADA', 'PERDIDA')").getSingleResult();
        return sum != null ? sum : BigDecimal.ZERO;
    }

    public BigDecimal sumValorGanado() {
        BigDecimal sum = (BigDecimal) em.createQuery("SELECT SUM(o.valor) FROM OportunidadEntity o WHERE o.etapa = 'GANADA'").getSingleResult();
        return sum != null ? sum : BigDecimal.ZERO;
    }

    public List<EtapaCantidadValorResponse> countOportunidadesPorEtapa() {
        List<Object[]> results = em.createQuery("SELECT o.etapa, COUNT(o), SUM(o.valor) FROM OportunidadEntity o GROUP BY o.etapa", Object[].class).getResultList();
        return results.stream().map(r -> new EtapaCantidadValorResponse(
                r[0].toString(),
                (long) r[1],
                r[2] != null ? (BigDecimal) r[2] : BigDecimal.ZERO
        )).collect(Collectors.toList());
    }

    public long countActividadesPendientes() {
        return (long) em.createQuery("SELECT COUNT(a) FROM ActividadEntity a WHERE a.estado = 'PENDIENTE'").getSingleResult();
    }

    public long countActividadesEnProceso() {
        return (long) em.createQuery("SELECT COUNT(a) FROM ActividadEntity a WHERE a.estado = 'EN_PROCESO'").getSingleResult();
    }

    public long countActividadesCompletadas() {
        return (long) em.createQuery("SELECT COUNT(a) FROM ActividadEntity a WHERE a.estado = 'COMPLETADA'").getSingleResult();
    }

    public long countActividadesCanceladas() {
        return (long) em.createQuery("SELECT COUNT(a) FROM ActividadEntity a WHERE a.estado = 'CANCELADA'").getSingleResult();
    }

    public long countActividadesVencidas() {
        return (long) em.createQuery("SELECT COUNT(a) FROM ActividadEntity a WHERE a.estado = 'PENDIENTE' AND a.fecha < :ahora")
                .setParameter("ahora", LocalDateTime.now())
                .getSingleResult();
    }

    public List<EstadoCantidadResponse> countActividadesPorEstado() {
        List<Object[]> results = em.createQuery("SELECT a.estado, COUNT(a) FROM ActividadEntity a GROUP BY a.estado", Object[].class).getResultList();
        return results.stream().map(r -> new EstadoCantidadResponse(r[0].toString(), (long) r[1])).collect(Collectors.toList());
    }

    public List<TipoCantidadResponse> countActividadesPorTipo() {
        List<Object[]> results = em.createQuery("SELECT a.tipo, COUNT(a) FROM ActividadEntity a GROUP BY a.tipo", Object[].class).getResultList();
        return results.stream().map(r -> new TipoCantidadResponse(r[0].toString(), (long) r[1])).collect(Collectors.toList());
    }

    public long countPropuestas() {
        return (long) em.createQuery("SELECT COUNT(p) FROM PropuestaEntity p").getSingleResult();
    }

    public List<EstadoCantidadResponse> countPropuestasPorEstado() {
        List<Object[]> results = em.createQuery("SELECT p.estado, COUNT(p) FROM PropuestaEntity p GROUP BY p.estado", Object[].class).getResultList();
        return results.stream().map(r -> new EstadoCantidadResponse(r[0].toString(), (long) r[1])).collect(Collectors.toList());
    }

    public BigDecimal sumValorPropuestasAprobadas() {
        BigDecimal sum = (BigDecimal) em.createQuery("SELECT SUM(p.total) FROM PropuestaEntity p WHERE p.estado = 'APROBADA'").getSingleResult();
        return sum != null ? sum : BigDecimal.ZERO;
    }
}
