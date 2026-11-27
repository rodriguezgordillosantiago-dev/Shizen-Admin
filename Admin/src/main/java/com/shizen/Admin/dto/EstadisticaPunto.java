package com.shizen.Admin.dto;

import java.time.LocalDate;

/** Valores diarios que alimentan las series de la gráfica de estadísticas. */
public record EstadisticaPunto(
        LocalDate fecha,
        long ingresos,
        long pedidos,
        long comisionEstimada,
        long ticketPromedio,
        long comprasCompletadas,
        long usuariosNuevos,
        long usuariosActivos
) {
}
