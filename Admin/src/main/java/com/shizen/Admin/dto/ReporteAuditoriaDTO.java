package com.shizen.Admin.dto;

import java.util.List;

/** Datos consolidados y anónimos que la vista de auditoría presenta y exporta. */
public record ReporteAuditoriaDTO(
        String codigoAuditoria,
        String fechaGeneracion,
        String periodoTexto,
        String agrupacionTexto,
        String alcanceTexto,
        long pedidosTotales,
        long pedidosCompletados,
        long pedidosEnCurso,
        long pedidosCancelados,
        long ventasTotales,
        long gananciasNetas,
        long gananciasComision,
        long gananciasEnvio,
        String picoDemandaEtiqueta,
        long picoDemandaPedidos,
        long usuariosRegistrados,
        long usuariosConsumidores,
        long usuariosNegocios,
        long usuariosRepartidores,
        String metodoPagoMasUsado,
        long metodoPagoMasUsadoCantidad,
        double metodoPagoMasUsadoPct,
        List<PuntoSerie> serieTemporal,
        List<EstadoConteo> pedidosPorEstado,
        List<MetodoPagoConteo> metodosPago,
        List<FuenteGananciaConteo> fuentesGanancia,
        List<CategoriaConteo> categoriasMasCompradas
) {
    public record PuntoSerie(
            String etiqueta,
            long pedidos,
            long ventas,
            long ganancias,
            long compras,
            long ticket,
            long usuariosNuevos,
            long usuariosActivos
    ) {}
    public record EstadoConteo(String estado, long cantidad) {}
    public record MetodoPagoConteo(String metodo, long cantidad, long total, double porcentaje) {}
    public record FuenteGananciaConteo(String fuente, long monto, double porcentaje) {}
    public record CategoriaConteo(String categoria, long unidades) {}
}
