package com.shizen.Admin.dto;

import java.time.LocalDateTime;

/**
 * DTO que representa un pedido con su cliente, negocio, monto y estado
 * para el listado y detalle en el panel de administración.
 */
public record PedidoResumenDTO(
        Integer idPedido,
        LocalDateTime fechaCreacion,
        String cliente,
        String negocio,
        Integer idNegocio,
        String descripcion,
        long monto,
        String estado,
        String direccionEntrega,
        String repartidor
) {
}
