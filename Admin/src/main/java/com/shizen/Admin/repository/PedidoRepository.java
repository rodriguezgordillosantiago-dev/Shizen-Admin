package com.shizen.Admin.repository;

import com.shizen.Admin.dto.PedidoResumenDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Consulta y actualiza pedidos, calcula sus métricas y obtiene los datos de actividad
 * que muestran las pantallas de pedidos y auditoría.
 */
@Repository
public class PedidoRepository {

    @PersistenceContext
    private EntityManager em;

    /**
     * Consulta pedidos aplicando filtros multicriterio opcionales de forma dinámica:
     * texto (ID, cliente, negocio, dirección, descripción), estado, negocio y rango de fechas.
     */
    @SuppressWarnings("unchecked")
    public List<PedidoResumenDTO> buscarPedidos(
            String buscar,
            String estado,
            Integer idNegocio,
            LocalDateTime desde,
            LocalDateTime hasta) {

        StringBuilder sql = new StringBuilder(
            "SELECT p.id_pedido, " +
            "       p.fecha_creacion, " +
            "       TRIM(COALESCE(u.nombre, '') || ' ' || COALESCE(u.apellido, '')) AS cliente, " +
            "       COALESCE(n.nombre, 'Sin negocio') AS negocio, " +
            "       p.id_negocio, " +
            "       COALESCE(NULLIF(TRIM(p.descripcion), ''), 'Pedido Shizen #' || p.id_pedido) AS descripcion, " +
            "       COALESCE(c.total, 0) AS monto, " +
            "       COALESCE(p.estado, 'pendiente') AS estado, " +
            "       COALESCE(p.direccion_entrega, 'Sin dirección') AS direccion_entrega, " +
            "       TRIM(COALESCE(r.nombre, '') || ' ' || COALESCE(r.apellido, '')) AS repartidor " +
            "FROM pedido p " +
            "LEFT JOIN usuario u ON u.id_usuario = p.id_usuario " +
            "LEFT JOIN negocios n ON n.id_negocio = p.id_negocio " +
            "LEFT JOIN compra c ON c.id_pedido = p.id_pedido " +
            "LEFT JOIN entrega e ON e.id_compra = c.id_compra " +
            "LEFT JOIN repartidor r ON r.id_repartidor = e.id_repartidor " +
            "WHERE 1=1 "
        );

        Map<String, Object> params = new HashMap<>();

        if (estado != null && !estado.isBlank() && !"Todos".equalsIgnoreCase(estado)) {
            sql.append("AND LOWER(p.estado) = LOWER(:estado) ");
            params.put("estado", estado.trim());
        }
        if (idNegocio != null) {
            sql.append("AND p.id_negocio = :idNegocio ");
            params.put("idNegocio", idNegocio);
        }
        if (desde != null) {
            sql.append("AND p.fecha_creacion >= :desde ");
            params.put("desde", desde);
        }
        if (hasta != null) {
            sql.append("AND p.fecha_creacion < :hasta ");
            params.put("hasta", hasta);
        }
        if (buscar != null && !buscar.isBlank()) {
            sql.append(
                "AND (CAST(p.id_pedido AS varchar) ILIKE :buscar " +
                "  OR COALESCE(u.nombre, '') ILIKE :buscar " +
                "  OR COALESCE(u.apellido, '') ILIKE :buscar " +
                "  OR COALESCE(n.nombre, '') ILIKE :buscar " +
                "  OR COALESCE(p.descripcion, '') ILIKE :buscar " +
                "  OR COALESCE(p.direccion_entrega, '') ILIKE :buscar) "
            );
            params.put("buscar", "%" + buscar.trim() + "%");
        }

        sql.append("ORDER BY p.fecha_creacion DESC NULLS LAST, p.id_pedido DESC LIMIT 200");

        Query q = em.createNativeQuery(sql.toString());
        params.forEach(q::setParameter);

        List<Object[]> rows = q.getResultList();
        return rows.stream().map(row -> new PedidoResumenDTO(
                ((Number) row[0]).intValue(),
                toLocalDateTime(row[1]),
                row[2] != null && !row[2].toString().isBlank() ? row[2].toString() : "Cliente",
                row[3] != null ? row[3].toString() : "Sin negocio",
                row[4] != null ? ((Number) row[4]).intValue() : null,
                row[5] != null ? row[5].toString() : "",
                row[6] != null ? ((Number) row[6]).longValue() : 0L,
                row[7] != null ? row[7].toString() : "pendiente",
                row[8] != null ? row[8].toString() : "Sin dirección",
                row[9] != null && !row[9].toString().isBlank() ? row[9].toString() : "Sin asignar"
        )).toList();
    }

    /**
     * Obtiene ventas y conteo de pedidos para Hoy, Semana (7 días) y Mes (30 días),
     * comparados contra su período inmediatamente anterior.
     */
    public Map<String, Object> obtenerMetricas(LocalDateTime ahora) {
        LocalDateTime inicioHoy = ahora.toLocalDate().atStartOfDay();
        LocalDateTime finHoy = inicioHoy.plusDays(1);

        LocalDateTime inicioSemana = inicioHoy.minusDays(6);
        LocalDateTime inicioMes = inicioHoy.minusDays(29);

        Map<String, Object> result = new HashMap<>();
        result.put("hoy", resumenRango(inicioHoy, finHoy, inicioHoy.minusDays(1), inicioHoy));
        result.put("semana", resumenRango(inicioSemana, finHoy, inicioSemana.minusDays(7), inicioSemana));
        result.put("mes", resumenRango(inicioMes, finHoy, inicioMes.minusDays(30), inicioMes));
        return result;
    }

    /** Compara ventas y cantidad entre un período y el período anterior. */
    private Map<String, Object> resumenRango(
            LocalDateTime desde, LocalDateTime hasta,
            LocalDateTime prevDesde, LocalDateTime prevHasta) {
        Object[] actual = queryResumen(desde, hasta);
        Object[] previo = queryResumen(prevDesde, prevHasta);

        long ventas = ((Number) actual[0]).longValue();
        long cantidad = ((Number) actual[1]).longValue();
        long ventasPrev = ((Number) previo[0]).longValue();

        String tendencia;
        if (ventasPrev == 0) {
            tendencia = ventas > 0 ? "Nuevo" : "0%";
        } else {
            double pct = ((ventas - ventasPrev) * 100.0) / ventasPrev;
            tendencia = String.format("%s%.1f%%", pct > 0 ? "+" : "", pct);
        }

        return Map.of(
                "ventas", ventas,
                "cantidad", cantidad,
                "tendencia", tendencia
        );
    }

    /** Resume ventas y cantidad de pedidos en un rango de fechas. */
    private Object[] queryResumen(LocalDateTime desde, LocalDateTime hasta) {
        Query q = em.createNativeQuery(
            "SELECT COALESCE(SUM(c.total), 0), COUNT(p.id_pedido) " +
            "FROM pedido p " +
            "LEFT JOIN compra c ON c.id_pedido = p.id_pedido " +
            "WHERE p.fecha_creacion >= :desde AND p.fecha_creacion < :hasta"
        );
        q.setParameter("desde", desde);
        q.setParameter("hasta", hasta);
        return (Object[]) q.getSingleResult();
    }

    /**
     * Devuelve 16 franjas de 90 minutos (00:00 a 23:59) con la cantidad real de pedidos
     * aplicando los mismos filtros multicriterio de la consulta actual.
     */
    @SuppressWarnings("unchecked")
    public int[] obtenerActividadPorFranja(
            String buscar,
            String estado,
            Integer idNegocio,
            LocalDateTime desde,
            LocalDateTime hasta) {

        StringBuilder sql = new StringBuilder(
            "SELECT FLOOR((EXTRACT(HOUR FROM p.fecha_creacion) * 60 + EXTRACT(MINUTE FROM p.fecha_creacion)) / 90) AS franja, " +
            "       COUNT(*) " +
            "FROM pedido p " +
            "LEFT JOIN usuario u ON u.id_usuario = p.id_usuario " +
            "LEFT JOIN negocios n ON n.id_negocio = p.id_negocio " +
            "WHERE 1=1 "
        );

        Map<String, Object> params = new HashMap<>();

        if (estado != null && !estado.isBlank() && !"Todos".equalsIgnoreCase(estado)) {
            sql.append("AND LOWER(p.estado) = LOWER(:estado) ");
            params.put("estado", estado.trim());
        }
        if (idNegocio != null) {
            sql.append("AND p.id_negocio = :idNegocio ");
            params.put("idNegocio", idNegocio);
        }
        if (desde != null) {
            sql.append("AND p.fecha_creacion >= :desde ");
            params.put("desde", desde);
        }
        if (hasta != null) {
            sql.append("AND p.fecha_creacion < :hasta ");
            params.put("hasta", hasta);
        }
        if (buscar != null && !buscar.isBlank()) {
            sql.append(
                "AND (CAST(p.id_pedido AS varchar) ILIKE :buscar " +
                "  OR COALESCE(u.nombre, '') ILIKE :buscar " +
                "  OR COALESCE(u.apellido, '') ILIKE :buscar " +
                "  OR COALESCE(n.nombre, '') ILIKE :buscar " +
                "  OR COALESCE(p.direccion_entrega, '') ILIKE :buscar " +
                "  OR COALESCE(p.descripcion, '') ILIKE :buscar) "
            );
            params.put("buscar", "%" + buscar.trim() + "%");
        }

        sql.append("GROUP BY franja");

        Query q = em.createNativeQuery(sql.toString());
        params.forEach(q::setParameter);

        int[] buckets = new int[16];
        for (Object[] row : (List<Object[]>) q.getResultList()) {
            if (row[0] == null) continue;
            int idx = Math.max(0, Math.min(15, ((Number) row[0]).intValue()));
            buckets[idx] = ((Number) row[1]).intValue();
        }
        return buckets;
    }

    /** Lista de negocios (id y nombre) para poblar el filtro en la interfaz. */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listarNegocios() {
        List<Object[]> rows = em.createNativeQuery(
                "SELECT id_negocio, nombre FROM negocios ORDER BY nombre").getResultList();
        return rows.stream().map(r -> Map.<String, Object>of(
                "id", ((Number) r[0]).intValue(),
                "nombre", r[1].toString()
        )).toList();
    }

    /** Lista de estados distintos de pedidos en BD. */
    @SuppressWarnings("unchecked")
    public List<String> listarEstados() {
        return em.createNativeQuery(
                "SELECT DISTINCT estado FROM pedido WHERE estado IS NOT NULL ORDER BY estado")
                .getResultList();
    }

    /** Lista de repartidores (id y nombre) para poblar filtros de reporte. */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listarRepartidores() {
        List<Object[]> rows = em.createNativeQuery(
                "SELECT id_repartidor, TRIM(COALESCE(nombre,'') || ' ' || COALESCE(apellido,'')) " +
                "FROM repartidor ORDER BY nombre").getResultList();
        return rows.stream().map(r -> Map.<String, Object>of(
                "id", ((Number) r[0]).intValue(),
                "nombre", r[1].toString()
        )).toList();
    }

    /** Lista de métodos de pago distintos registrados en compra. */
    @SuppressWarnings("unchecked")
    public List<String> listarMetodosPago() {
        return em.createNativeQuery(
                "SELECT DISTINCT metodo_pago FROM compra WHERE metodo_pago IS NOT NULL ORDER BY metodo_pago")
                .getResultList();
    }

    /** Actualiza el estado de un pedido en PostgreSQL. */
    @Transactional
    public int actualizarEstado(Integer idPedido, String nuevoEstado) {
        Query q = em.createNativeQuery("UPDATE pedido SET estado = :estado WHERE id_pedido = :id");
        q.setParameter("estado", nuevoEstado);
        q.setParameter("id", idPedido);
        return q.executeUpdate();
    }

    /** Registra una acción del administrador en la tabla auditoria_admin. */
    @Transactional
    public void registrarAuditoria(Integer idAdmin, String accion, String entidad, Integer idEntidad, String detalle) {
        Query q = em.createNativeQuery(
            "INSERT INTO auditoria_admin (id_admin, accion, entidad, id_entidad, detalle, fecha) " +
            "VALUES (:idAdmin, :accion, :entidad, :idEntidad, :detalle, NOW())"
        );
        q.setParameter("idAdmin", idAdmin);
        q.setParameter("accion", accion);
        q.setParameter("entidad", entidad);
        q.setParameter("idEntidad", idEntidad);
        q.setParameter("detalle", detalle);
        q.executeUpdate();
    }

    /** Convierte el valor de fecha devuelto por JDBC a LocalDateTime. */
    private LocalDateTime toLocalDateTime(Object val) {
        if (val == null) return null;
        if (val instanceof LocalDateTime ldt) return ldt;
        if (val instanceof Timestamp ts) return ts.toLocalDateTime();
        return null;
    }
}
