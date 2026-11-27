package com.shizen.Admin.repository;

import com.shizen.Admin.model.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación dinámica de filtros multicriterio para Usuario.
 * Evita parámetros nulos en PostgreSQL construyendo la cláusula WHERE condicionalmente.
 */
public class UsuariosRepositoryImpl implements UsuariosRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<Usuario> buscarConFiltros(
            String buscar,
            String rol,
            String baneado,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta) {

        StringBuilder jpql = new StringBuilder("SELECT u FROM Usuario u WHERE 1=1 ");
        Map<String, Object> params = new HashMap<>();

        if (rol != null && !rol.isBlank() && !"Todos".equalsIgnoreCase(rol)) {
            jpql.append("AND LOWER(u.rol) = LOWER(:rol) ");
            params.put("rol", rol.trim());
        }

        if (buscar != null && !buscar.isBlank()) {
            jpql.append(
                "AND (LOWER(COALESCE(u.nombre, '')) LIKE :buscar " +
                "  OR LOWER(COALESCE(u.apellido, '')) LIKE :buscar " +
                "  OR LOWER(COALESCE(u.email, '')) LIKE :buscar) "
            );
            params.put("buscar", "%" + buscar.trim().toLowerCase() + "%");
        }

        if ("activo".equalsIgnoreCase(baneado)) {
            jpql.append("AND (u.baneado IS NULL OR u.baneado = false) ");
        } else if ("baneado".equalsIgnoreCase(baneado)) {
            jpql.append("AND u.baneado = true ");
        }

        if (fechaDesde != null) {
            jpql.append("AND u.fechaRegistro >= :fechaDesde ");
            params.put("fechaDesde", fechaDesde);
        }

        if (fechaHasta != null) {
            jpql.append("AND u.fechaRegistro < :fechaHasta ");
            params.put("fechaHasta", fechaHasta);
        }

        jpql.append("ORDER BY u.fechaRegistro DESC");

        TypedQuery<Usuario> query = em.createQuery(jpql.toString(), Usuario.class);
        params.forEach(query::setParameter);
        return query.getResultList();
    }

    /**
     * Obtiene la foto o logo de cada negocio y repartidor indexado por id_usuario.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Map<Integer, String> obtenerFotosPorUsuario() {
        List<Object[]> filas = em.createNativeQuery(
            "SELECT id_usuario, logo_url FROM negocios WHERE logo_url IS NOT NULL AND TRIM(logo_url) <> '' " +
            "UNION ALL " +
            "SELECT id_usuario, foto_url FROM repartidor WHERE foto_url IS NOT NULL AND TRIM(foto_url) <> ''"
        ).getResultList();

        Map<Integer, String> mapa = new HashMap<>();
        for (Object[] f : filas) {
            if (f[0] == null || f[1] == null) continue;
            Integer idUsuario = ((Number) f[0]).intValue();
            String url = normalizarMediaUrl(f[1].toString().trim());
            if (url != null) {
                mapa.putIfAbsent(idUsuario, url);
            }
        }
        return mapa;
    }

    private String normalizarMediaUrl(String raw) {
        if (raw.isEmpty()) return null;
        if (raw.startsWith("http://") || raw.startsWith("https://")) {
            return raw;
        }
        String limpio = raw.replace("\\", "/");
        if (limpio.startsWith("../Imagenes_prueba/")) {
            limpio = "assets/" + limpio.substring(3);
        } else if (limpio.startsWith("../")) {
            limpio = limpio.substring(3);
        }
        if (limpio.startsWith("/")) {
            limpio = limpio.substring(1);
        }
        return "/shizen-media/" + limpio;
    }
}
