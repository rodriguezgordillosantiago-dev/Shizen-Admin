package com.shizen.Admin.dto;

/** Indicadores de estadísticas y variaciones del período consultado. */
public record KpiResponse(
        int dias,
        long ingresos,
        Double crecimientoIngresosPct,
        long comprasCompletadas,
        long ticketPromedio,
        long pedidosTotales,
        Double crecimientoPedidosPct,
        long usuariosRegistrados,
        long usuariosNuevos,
        long usuariosActivos,
        double comisionPct,
        long comisionEstimada,
        long metaMensual,
        long ingresosMesActual,
        Double avanceMetaPct) {
}
