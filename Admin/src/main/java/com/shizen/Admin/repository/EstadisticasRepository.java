package com.shizen.Admin.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/** Consulta en PostgreSQL los datos usados por las tarjetas y gráficas de estadísticas. */
@Repository
public class EstadisticasRepository {

    @PersistenceContext
    private EntityManager em;

    /** Combina ingresos, pedidos y usuarios en una fila por cada día del período. */
    @SuppressWarnings("unchecked")
    public List<Object[]> obtenerDatosGrafica(LocalDateTime desde, LocalDateTime hasta) {

        Query q = em.createNativeQuery(
            "SELECT serie.fecha::date AS fecha, " +
            "       COALESCE(ingresos.total, 0) AS ingresos, " +
            "       COALESCE(pedidos.cantidad, 0) AS pedidos, " +
            "       COALESCE(ingresos.compras, 0) AS compras, " +
            "       COALESCE(usuarios_reg.nuevos, 0) AS usuarios_nuevos, " +
            "       COALESCE(activos.cant, 0) AS usuarios_activos " +
            "FROM generate_series(" +
            "     CAST(:desde AS date), " +
            "     CAST(:hasta AS date) - INTERVAL '1 day', " +
            "     INTERVAL '1 day') AS serie(fecha) " +

            "LEFT JOIN (" +
            "  SELECT fecha_pago::date AS fecha, SUM(total) AS total, COUNT(*) AS compras " +
            "  FROM compra " +
            "  WHERE LOWER(estado) = 'completado' " +
            "    AND fecha_pago >= :desde AND fecha_pago < :hasta " +
            "  GROUP BY fecha_pago::date" +
            ") ingresos ON ingresos.fecha = serie.fecha::date " +

            "LEFT JOIN (" +
            "  SELECT fecha_creacion::date AS fecha, COUNT(*) AS cantidad " +
            "  FROM pedido " +
            "  WHERE fecha_creacion >= :desde AND fecha_creacion < :hasta " +
            "  GROUP BY fecha_creacion::date" +
            ") pedidos ON pedidos.fecha = serie.fecha::date " +

            "LEFT JOIN (" +
            "  SELECT fecha_registro::date AS fecha, COUNT(*) AS nuevos " +
            "  FROM usuario " +
            "  WHERE LOWER(rol) = 'usuario' " +
            "    AND fecha_registro >= :desde AND fecha_registro < :hasta " +
            "  GROUP BY fecha_registro::date" +
            ") usuarios_reg ON usuarios_reg.fecha = serie.fecha::date " +

            "LEFT JOIN (" +
            "  SELECT c.fecha_pago::date AS fecha, COUNT(DISTINCT p.id_usuario) AS cant " +
            "  FROM compra c " +
            "  JOIN pedido p ON p.id_pedido = c.id_pedido " +
            "  WHERE LOWER(c.estado) = 'completado' " +
            "    AND c.fecha_pago >= :desde AND c.fecha_pago < :hasta " +
            "  GROUP BY c.fecha_pago::date" +
            ") activos ON activos.fecha = serie.fecha::date " +

            "ORDER BY serie.fecha"
        );

        q.setParameter("desde", desde);
        q.setParameter("hasta", hasta);
        return q.getResultList();
    }

    /** Suma las unidades pedidas por categoría dentro del rango de fechas. */
    @SuppressWarnings("unchecked")
    public List<Object[]> obtenerCategoriasMasCompradas(LocalDateTime desde, LocalDateTime hasta) {
        Query q = em.createNativeQuery(
            "SELECT cat.nombre AS categoria, COALESCE(SUM(dp.cantidad), 0) AS unidades " +
            "FROM detalle_pedido dp " +
            "JOIN pedido p ON p.id_pedido = dp.id_pedido " +
            "JOIN menu_items m ON m.id_menu_item = dp.id_menu_item " +
            "JOIN categorias cat ON cat.id_categoria = m.id_categoria " +
            "WHERE p.fecha_creacion >= :desde AND p.fecha_creacion < :hasta " +
            "GROUP BY cat.nombre " +
            "ORDER BY unidades DESC"
        );
        q.setParameter("desde", desde);
        q.setParameter("hasta", hasta);
        return q.getResultList();
    }

    /** Suma únicamente las compras completadas y pagadas dentro del período. */
    public long sumarIngresos(LocalDateTime desde, LocalDateTime hasta) {
        return conRango("SELECT COALESCE(SUM(total), 0) FROM compra "
                + "WHERE LOWER(estado) = 'completado' "
                + "AND fecha_pago >= :desde AND fecha_pago < :hasta", desde, hasta);
    }

    /** Cuenta las compras completadas y pagadas dentro del período. */
    public long contarComprasCompletadas(LocalDateTime desde, LocalDateTime hasta) {
        return conRango("SELECT COUNT(*) FROM compra "
                + "WHERE LOWER(estado) = 'completado' "
                + "AND fecha_pago >= :desde AND fecha_pago < :hasta", desde, hasta);
    }

    /** Cuenta los pedidos creados dentro del período. */
    public long contarPedidos(LocalDateTime desde, LocalDateTime hasta) {
        return conRango("SELECT COUNT(*) FROM pedido "
                + "WHERE fecha_creacion >= :desde AND fecha_creacion < :hasta", desde, hasta);
    }

    /** Cuenta todas las cuentas de clientes registradas. */
    public long contarClientes() {
        Query q = em.createNativeQuery(
                "SELECT COUNT(*) FROM usuario WHERE LOWER(rol) = 'usuario'");
        return aLong(q.getSingleResult());
    }

    /** Cuenta los clientes registrados dentro del período. */
    public long contarClientesNuevos(LocalDateTime desde, LocalDateTime hasta) {
        return conRango("SELECT COUNT(*) FROM usuario WHERE LOWER(rol) = 'usuario' "
                + "AND fecha_registro >= :desde AND fecha_registro < :hasta", desde, hasta);
    }

    /** Cuenta clientes con al menos una compra completada en el período. */
    public long contarUsuariosActivos(LocalDateTime desde, LocalDateTime hasta) {
        return conRango("SELECT COUNT(DISTINCT p.id_usuario) FROM compra c "
                + "JOIN pedido p ON p.id_pedido = c.id_pedido "
                + "WHERE LOWER(c.estado) = 'completado' "
                + "AND c.fecha_pago >= :desde AND c.fecha_pago < :hasta", desde, hasta);
    }

    /** Ejecuta una consulta numérica con límites de fecha y convierte su resultado. */
    private long conRango(String sql, LocalDateTime desde, LocalDateTime hasta) {
        Query q = em.createNativeQuery(sql);
        q.setParameter("desde", desde);
        q.setParameter("hasta", hasta);
        return aLong(q.getSingleResult());
    }

    private long aLong(Object valor) {
        return valor == null ? 0L : ((Number) valor).longValue();
    }
}
