package com.shizen.Admin.service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shizen.Admin.dto.CategoriaEstadistica;
import com.shizen.Admin.dto.EstadisticaPunto;
import com.shizen.Admin.dto.KpiResponse;
import com.shizen.Admin.repository.EstadisticasRepository;

/** Calcula los indicadores y transforma las consultas en datos para las gráficas. */
@Service
public class EstadisticasService {

    private final EstadisticasRepository repo;
    private final double comisionPct;
    private final long metaMensual;

    public EstadisticasService(
            EstadisticasRepository repo,
            @Value("${shizen.estadisticas.comision-porcentaje:10}") double comisionPct,
            @Value("${shizen.estadisticas.meta-mensual:0}") long metaMensual) {
        this.repo        = repo;
        this.comisionPct = comisionPct;
        this.metaMensual = metaMensual;
    }

    /** Calcula los KPI del período y los compara con el período anterior. */
    @Transactional(readOnly = true)
    public KpiResponse obtenerKpis(int dias) {
        LocalDateTime hasta          = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime desde          = hasta.minusDays(dias);
        LocalDateTime desdeAnterior  = desde.minusDays(dias);

        long ingresos          = repo.sumarIngresos(desde, hasta);
        long ingresosAnterior  = repo.sumarIngresos(desdeAnterior, desde);
        long compras           = repo.contarComprasCompletadas(desde, hasta);
        long ticket            = compras > 0 ? ingresos / compras : 0;

        long pedidos           = repo.contarPedidos(desde, hasta);
        long pedidosAnterior   = repo.contarPedidos(desdeAnterior, desde);

        LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        long ingresosMes        = repo.sumarIngresos(inicioMes, hasta);
        Double avanceMeta       = metaMensual > 0
                ? redondear(ingresosMes * 100.0 / metaMensual)
                : null;

        return new KpiResponse(
                dias,
                ingresos,
                crecimiento(ingresos, ingresosAnterior),
                compras,
                ticket,
                pedidos,
                crecimiento(pedidos, pedidosAnterior),
                repo.contarClientes(),
                repo.contarClientesNuevos(desde, hasta),
                repo.contarUsuariosActivos(desde, hasta),
                comisionPct,
                Math.round(ingresos * comisionPct / 100.0),
                metaMensual,
                ingresosMes,
                avanceMeta);
    }

    /** Convierte las filas diarias de la base de datos en puntos de la gráfica. */
    @Transactional(readOnly = true)
    public List<EstadisticaPunto> obtenerDatosGrafica(int dias) {
        LocalDateTime hasta = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime desde = hasta.minusDays(dias);

        List<Object[]> filas = repo.obtenerDatosGrafica(desde, hasta);

        return filas.stream().map(fila -> {
            LocalDate fecha = (fila[0] instanceof Date d) ? d.toLocalDate() : (LocalDate) fila[0];
            long ingresos   = ((Number) fila[1]).longValue();
            long pedidos    = ((Number) fila[2]).longValue();
            long compras    = ((Number) fila[3]).longValue();
            long uNuevos    = ((Number) fila[4]).longValue();
            long uActivos   = ((Number) fila[5]).longValue();
            long comision   = Math.round(ingresos * comisionPct / 100.0);
            long ticket     = compras > 0 ? ingresos / compras : 0L;

            return new EstadisticaPunto(fecha, ingresos, pedidos, comision, ticket, compras, uNuevos, uActivos);
        }).toList();
    }

    /** Obtiene las categorías más compradas dentro del período solicitado. */
    @Transactional(readOnly = true)
    public List<CategoriaEstadistica> obtenerCategoriasMasCompradas(int dias) {
        LocalDateTime hasta = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime desde = hasta.minusDays(dias);
        return repo.obtenerCategoriasMasCompradas(desde, hasta)
                .stream()
                .map(r -> new CategoriaEstadistica(r[0].toString(), ((Number) r[1]).longValue()))
                .toList();
    }

    /** Devuelve null cuando no hay un período anterior para comparar. */
    private Double crecimiento(long actual, long anterior) {
        if (anterior == 0) return null;
        return redondear((actual - anterior) * 100.0 / anterior);
    }

    private double redondear(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }
}
