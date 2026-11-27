package com.shizen.Admin.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Ejecuta las consultas que alimentan los indicadores y desgloses del reporte. */
@Repository
public class ReporteRepository {

    @PersistenceContext
    private EntityManager em;

    // Totales de pedidos por estado y ventas brutas completadas dentro del rango y filtros activos.
    public Object[] obtenerKpisPedidos(
            LocalDateTime desde, LocalDateTime hasta,
            String rol, String estado, String metodoPago, String categoria, String zona) {

        Query q = crearQueryFiltrada(
            "SELECT COUNT(DISTINCT p.id_pedido) AS pedidos_totales, " +
            "       COUNT(DISTINCT CASE WHEN LOWER(COALESCE(p.estado,'')) IN ('entregado','completado') THEN p.id_pedido END) AS completados, " +
            "       COUNT(DISTINCT CASE WHEN LOWER(COALESCE(p.estado,'')) IN ('pendiente','asignado','recogido en negocio','en camino') THEN p.id_pedido END) AS en_curso, " +
            "       COUNT(DISTINCT CASE WHEN LOWER(COALESCE(p.estado,'')) = 'cancelado' THEN p.id_pedido END) AS cancelados, " +
            "       COALESCE(SUM(CASE WHEN LOWER(COALESCE(c.estado,'')) = 'completado' THEN c.total ELSE 0 END), 0) AS ventas_totales " +
            "FROM pedido p " +
            "LEFT JOIN usuario u ON u.id_usuario = p.id_usuario " +
            "LEFT JOIN compra c ON c.id_pedido = p.id_pedido " +
            "LEFT JOIN entrega e ON e.id_compra = c.id_compra ",
            "",
            desde, hasta, rol, estado, metodoPago, categoria, zona
        );
        return (Object[]) q.getSingleResult();
    }

    // Serie agrupada por el criterio temporal elegido (por hora, diaria, semanal, mensual, trimestral, semestral o anual).
    @SuppressWarnings("unchecked")
    public List<Object[]> obtenerSerieTemporal(
            LocalDateTime desde, LocalDateTime hasta, String agrupacion,
            String rol, String estado, String metodoPago, String categoria, String zona) {

        String expr = expresionAgrupacion("p.fecha_creacion", agrupacion);

        Query q = crearQueryFiltrada(
            "SELECT " + expr + " AS etiqueta, " +
            "       COUNT(DISTINCT p.id_pedido) AS pedidos, " +
            "       COALESCE(SUM(CASE WHEN LOWER(COALESCE(c.estado,'')) = 'completado' THEN c.total ELSE 0 END), 0) AS ventas, " +
            "       COUNT(DISTINCT CASE WHEN LOWER(COALESCE(c.estado,'')) = 'completado' THEN c.id_compra END) AS compras, " +
            "       COUNT(DISTINCT CASE WHEN LOWER(COALESCE(c.estado,'')) = 'completado' THEN p.id_usuario END) AS usuarios_activos " +
            "FROM pedido p " +
            "LEFT JOIN usuario u ON u.id_usuario = p.id_usuario " +
            "LEFT JOIN compra c ON c.id_pedido = p.id_pedido " +
            "LEFT JOIN entrega e ON e.id_compra = c.id_compra ",
            "GROUP BY " + expr + " ORDER BY " + expr,
            desde, hasta, rol, estado, metodoPago, categoria, zona
        );
        return q.getResultList();
    }

    // Usuarios registrados agrupados con el mismo criterio temporal que las gráficas de pedidos.
    @SuppressWarnings("unchecked")
    public List<Object[]> obtenerSerieUsuariosNuevos(
            LocalDateTime desde, LocalDateTime hasta, String agrupacion, String rol, String zona) {

        String expr = expresionAgrupacion("fecha_registro", agrupacion);

        StringBuilder sql = new StringBuilder(
            "SELECT " + expr + " AS etiqueta, COUNT(*) AS nuevos " +
            "FROM usuario " +
            "WHERE fecha_registro >= :desde AND fecha_registro < :hasta "
        );

        String rolNorm = normalizarRolDb(rol);
        if (rolNorm != null) {
            sql.append("AND LOWER(COALESCE(rol,'')) = :rol ");
        }
        if (tieneValor(zona)) {
            sql.append("AND LOWER(COALESCE(localidad,'')) = LOWER(:zona) ");
        }
        sql.append("GROUP BY ").append(expr).append(" ORDER BY ").append(expr);

        Query q = em.createNativeQuery(sql.toString());
        q.setParameter("desde", desde);
        q.setParameter("hasta", hasta);
        if (rolNorm != null) q.setParameter("rol", rolNorm);
        if (tieneValor(zona)) q.setParameter("zona", zona.trim());
        return q.getResultList();
    }

    // Las 10 categorías con más unidades vendidas en el rango filtrado.
    @SuppressWarnings("unchecked")
    public List<Object[]> obtenerCategoriasMasCompradas(
            LocalDateTime desde, LocalDateTime hasta,
            String rol, String estado, String metodoPago, String categoria, String zona) {

        String catExtra = tieneValor(categoria) ? "AND LOWER(cat.nombre) = LOWER(:catExacta) " : "";

        Query q = crearQueryFiltrada(
            "SELECT cat.nombre AS categoria, COALESCE(SUM(dp.cantidad), 0) AS unidades " +
            "FROM pedido p " +
            "LEFT JOIN usuario u ON u.id_usuario = p.id_usuario " +
            "LEFT JOIN compra c ON c.id_pedido = p.id_pedido " +
            "LEFT JOIN entrega e ON e.id_compra = c.id_compra " +
            "JOIN detalle_pedido dp ON dp.id_pedido = p.id_pedido " +
            "JOIN menu_items m ON m.id_menu_item = dp.id_menu_item " +
            "JOIN categorias cat ON cat.id_categoria = m.id_categoria ",
            catExtra + "GROUP BY cat.nombre ORDER BY unidades DESC LIMIT 10",
            desde, hasta, rol, estado, metodoPago, categoria, zona
        );
        if (!catExtra.isEmpty()) {
            q.setParameter("catExacta", categoria.trim());
        }
        return q.getResultList();
    }

    /** Lista las categorías disponibles para el filtro del reporte. */
    @SuppressWarnings("unchecked")
    public List<String> listarCategorias() {
        return em.createNativeQuery("SELECT DISTINCT nombre FROM categorias WHERE nombre IS NOT NULL ORDER BY nombre")
                .getResultList();
    }

    /** Lista las zonas disponibles para el filtro del reporte. */
    @SuppressWarnings("unchecked")
    public List<String> listarZonas() {
        return em.createNativeQuery(
                "SELECT DISTINCT TRIM(localidad) FROM usuario WHERE localidad IS NOT NULL AND TRIM(localidad) <> '' ORDER BY 1")
                .getResultList();
    }

    /** Cuenta los pedidos por estado con los filtros activos. */
    @SuppressWarnings("unchecked")
    public List<Object[]> obtenerPedidosPorEstado(
            LocalDateTime desde, LocalDateTime hasta,
            String rol, String estado, String metodoPago, String categoria, String zona) {

        Query q = crearQueryFiltrada(
            "SELECT COALESCE(p.estado, 'Sin estado') AS estado, COUNT(DISTINCT p.id_pedido) AS cantidad " +
            "FROM pedido p " +
            "LEFT JOIN usuario u ON u.id_usuario = p.id_usuario " +
            "LEFT JOIN compra c ON c.id_pedido = p.id_pedido " +
            "LEFT JOIN entrega e ON e.id_compra = c.id_compra ",
            "GROUP BY p.estado ORDER BY cantidad DESC",
            desde, hasta, rol, estado, metodoPago, categoria, zona
        );
        return q.getResultList();
    }

    // Ordenado de mayor a menor uso para identificar el método de pago más utilizado.
    @SuppressWarnings("unchecked")
    public List<Object[]> obtenerMetodosPago(
            LocalDateTime desde, LocalDateTime hasta,
            String rol, String estado, String metodoPago, String categoria, String zona) {

        Query q = crearQueryFiltrada(
            "SELECT COALESCE(c.metodo_pago, 'Sin registrar') AS metodo, " +
            "       COUNT(DISTINCT c.id_compra) AS cantidad, " +
            "       COALESCE(SUM(c.total), 0) AS total " +
            "FROM pedido p " +
            "LEFT JOIN usuario u ON u.id_usuario = p.id_usuario " +
            "JOIN compra c ON c.id_pedido = p.id_pedido " +
            "LEFT JOIN entrega e ON e.id_compra = c.id_compra ",
            "GROUP BY c.metodo_pago ORDER BY cantidad DESC, total DESC",
            desde, hasta, rol, estado, metodoPago, categoria, zona
        );
        return q.getResultList();
    }

    // Conteo anónimo de cuentas registradas en el rango de fechas, separado por rol.
    public Object[] contarUsuariosPorRol(LocalDateTime desde, LocalDateTime hasta, String rol, String zona) {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) AS total, " +
            "       COUNT(*) FILTER (WHERE LOWER(COALESCE(rol,'')) = 'usuario') AS consumidores, " +
            "       COUNT(*) FILTER (WHERE LOWER(COALESCE(rol,'')) = 'negocio') AS negocios, " +
            "       COUNT(*) FILTER (WHERE LOWER(COALESCE(rol,'')) = 'repartidor') AS repartidores " +
            "FROM usuario " +
            "WHERE fecha_registro >= :desde AND fecha_registro < :hasta "
        );

        String rolNorm = normalizarRolDb(rol);
        if (rolNorm != null) {
            sql.append("AND LOWER(COALESCE(rol,'')) = :rol ");
        }
        if (tieneValor(zona)) {
            sql.append("AND LOWER(COALESCE(localidad,'')) = LOWER(:zona) ");
        }

        Query q = em.createNativeQuery(sql.toString());
        q.setParameter("desde", desde);
        q.setParameter("hasta", hasta);
        if (rolNorm != null) q.setParameter("rol", rolNorm);
        if (tieneValor(zona)) q.setParameter("zona", zona.trim());
        return (Object[]) q.getSingleResult();
    }

    /** Crea la expresión SQL para agrupar resultados por hora, día, semana o período mayor. */
    private String expresionAgrupacion(String columna, String agrupacion) {
        String ag = agrupacion != null ? agrupacion.trim().toLowerCase() : "semanal";
        return switch (ag) {
            case "por hora", "hora" -> "TO_CHAR(" + columna + ", 'HH24') || ':00'";
            case "diaria", "dia" -> "TO_CHAR(" + columna + "::date, 'YYYY-MM-DD')";
            case "mensual" -> "TO_CHAR(" + columna + ", 'YYYY-MM')";
            case "trimestral" -> "TO_CHAR(" + columna + ", 'YYYY') || '-T' || TO_CHAR(" + columna + ", 'Q')";
            case "semestral" -> "TO_CHAR(" + columna + ", 'YYYY') || '-S' || CASE WHEN EXTRACT(MONTH FROM " + columna + ") <= 6 THEN '1' ELSE '2' END";
            case "anual" -> "TO_CHAR(" + columna + ", 'YYYY')";
            default -> "TO_CHAR(DATE_TRUNC('week', " + columna + ")::date, 'YYYY-MM-DD')";
        };
    }

    // Arma el WHERE dinámicamente; si un filtro opcional viene vacío, no agrega condición SQL.
    private Query crearQueryFiltrada(
            String selectFrom, String groupOrder,
            LocalDateTime desde, LocalDateTime hasta,
            String rol, String estado, String metodoPago, String categoria, String zona) {

        StringBuilder where = new StringBuilder(
            "WHERE p.fecha_creacion >= :desde AND p.fecha_creacion < :hasta "
        );
        Map<String, Object> params = new HashMap<>();
        params.put("desde", desde);
        params.put("hasta", hasta);

        if (tieneValor(rol)) {
            if ("Cliente".equalsIgnoreCase(rol)) {
                where.append("AND LOWER(COALESCE(u.rol,'')) = 'usuario' ");
            } else if ("Repartidor".equalsIgnoreCase(rol)) {
                where.append("AND e.id_repartidor IS NOT NULL ");
            } else if ("Negocio".equalsIgnoreCase(rol)) {
                where.append("AND p.id_negocio IS NOT NULL ");
            }
        }

        if (tieneValor(estado)) {
            where.append("AND LOWER(p.estado) = LOWER(:estado) ");
            params.put("estado", estado.trim());
        }

        if (tieneValor(metodoPago)) {
            where.append("AND LOWER(c.metodo_pago) = LOWER(:metodoPago) ");
            params.put("metodoPago", metodoPago.trim());
        }

        if (tieneValor(categoria)) {
            where.append(
                "AND EXISTS (SELECT 1 FROM detalle_pedido dp2 " +
                "JOIN menu_items m2 ON m2.id_menu_item = dp2.id_menu_item " +
                "JOIN categorias cat2 ON cat2.id_categoria = m2.id_categoria " +
                "WHERE dp2.id_pedido = p.id_pedido AND LOWER(cat2.nombre) = LOWER(:categoria)) "
            );
            params.put("categoria", categoria.trim());
        }

        if (tieneValor(zona)) {
            where.append("AND LOWER(COALESCE(u.localidad,'')) = LOWER(:zona) ");
            params.put("zona", zona.trim());
        }

        Query q = em.createNativeQuery(selectFrom + where + groupOrder);
        params.forEach(q::setParameter);
        return q;
    }

    private boolean tieneValor(String v) {
        return v != null && !v.isBlank();
    }

    private String normalizarRolDb(String rol) {
        if (!tieneValor(rol)) return null;
        if ("Cliente".equalsIgnoreCase(rol)) return "usuario";
        if ("Repartidor".equalsIgnoreCase(rol)) return "repartidor";
        if ("Negocio".equalsIgnoreCase(rol)) return "negocio";
        return null;
    }
}
