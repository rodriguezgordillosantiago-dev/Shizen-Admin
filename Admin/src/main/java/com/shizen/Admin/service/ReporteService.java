package com.shizen.Admin.service;

import com.shizen.Admin.dto.ReporteAuditoriaDTO;
import com.shizen.Admin.dto.ReporteAuditoriaDTO.*;
import com.shizen.Admin.repository.ReporteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Combina consultas y cálculos para construir el reporte administrativo anónimo. */
@Service
public class ReporteService {

    private final ReporteRepository repo;
    private static final DateTimeFormatter FMT_LARGO =
            DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", Locale.of("es", "CO"));

    // Porcentajes usados para estimar la comisión y el componente de envío.
    private static final double PCT_COMISION = 0.10;
    private static final double PCT_ENVIO = 0.05;

    public ReporteService(ReporteRepository repo) {
        this.repo = repo;
    }

    /** Calcula indicadores, series y desgloses aplicando los filtros del reporte. */
    @Transactional(readOnly = true)
    public ReporteAuditoriaDTO generarAuditoria(
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String agrupacion,
            String rol,
            String estado,
            String fuenteGanancia,
            String metodoPago,
            String categoria,
            String zona) {

        LocalDate inicio = (fechaDesde != null) ? fechaDesde : LocalDate.now().minusDays(29);
        LocalDate fin = (fechaHasta != null) ? fechaHasta : LocalDate.now();
        if (fin.isBefore(inicio)) {
            LocalDate tmp = inicio;
            inicio = fin;
            fin = tmp;
        }

        String agrupacionEfectiva = normalizarAgrupacion(agrupacion);
        LocalDateTime desdeDt = inicio.atStartOfDay();
        LocalDateTime hastaDt = fin.plusDays(1).atStartOfDay();

        Object[] kpis = repo.obtenerKpisPedidos(desdeDt, hastaDt, rol, estado, metodoPago, categoria, zona);
        long pedidosTotales = aLong(kpis[0]);
        long pedidosCompletados = aLong(kpis[1]);
        long pedidosEnCurso = aLong(kpis[2]);
        long pedidosCancelados = aLong(kpis[3]);
        long ventasTotales = aLong(kpis[4]);

        long ganComision = Math.round(ventasTotales * PCT_COMISION);
        long ganEnvio = Math.round(ventasTotales * PCT_ENVIO);
        double factorSerie = factorGanancia(fuenteGanancia);
        long gananciasNetas = Math.round(ventasTotales * factorSerie);

        Map<String, Long> mapaUsuariosNuevos = new LinkedHashMap<>();
        for (Object[] r : repo.obtenerSerieUsuariosNuevos(desdeDt, hastaDt, agrupacionEfectiva, rol, zona)) {
            mapaUsuariosNuevos.put(r[0].toString(), aLong(r[1]));
        }

        Map<String, PuntoSerie> mapaSerie = new LinkedHashMap<>();
        for (Object[] r : repo.obtenerSerieTemporal(
                desdeDt, hastaDt, agrupacionEfectiva, rol, estado, metodoPago, categoria, zona)) {
            String etq = r[0].toString();
            long ped = aLong(r[1]);
            long ven = aLong(r[2]);
            long comp = aLong(r[3]);
            long uAct = aLong(r[4]);
            long ticket = comp > 0 ? ven / comp : 0L;
            long uNue = mapaUsuariosNuevos.getOrDefault(etq, 0L);
            mapaSerie.put(etq, new PuntoSerie(etq, ped, ven, Math.round(ven * factorSerie), comp, ticket, uNue, uAct));
        }

        // Agrega períodos donde solo hubo registros de usuarios sin pedidos
        for (Map.Entry<String, Long> e : mapaUsuariosNuevos.entrySet()) {
            mapaSerie.putIfAbsent(e.getKey(),
                    new PuntoSerie(e.getKey(), 0L, 0L, 0L, 0L, 0L, e.getValue(), 0L));
        }

        List<PuntoSerie> serieTemporal = mapaSerie.values().stream()
                .sorted(Comparator.comparing(PuntoSerie::etiqueta))
                .toList();

        PuntoSerie pico = serieTemporal.stream()
                .max(Comparator.comparingLong(PuntoSerie::pedidos))
                .orElse(null);
        String picoEtiqueta = (pico != null && pico.pedidos() > 0) ? pico.etiqueta() : "Sin actividad";
        long picoPedidos = (pico != null) ? pico.pedidos() : 0L;

        List<EstadoConteo> porEstado = repo.obtenerPedidosPorEstado(
                        desdeDt, hastaDt, rol, estado, metodoPago, categoria, zona)
                .stream()
                .map(r -> new EstadoConteo(r[0].toString(), aLong(r[1])))
                .toList();

        Object[] usuariosRow = repo.contarUsuariosPorRol(desdeDt, hastaDt, rol, zona);
        long totalUsuarios = aLong(usuariosRow[0]);
        long consumidores = aLong(usuariosRow[1]);
        long negocios = aLong(usuariosRow[2]);
        long repartidores = aLong(usuariosRow[3]);

        List<Object[]> metodosRaw = repo.obtenerMetodosPago(
                desdeDt, hastaDt, rol, estado, metodoPago, categoria, zona);
        long totalPagos = metodosRaw.stream().mapToLong(r -> aLong(r[1])).sum();

        List<MetodoPagoConteo> metodosPago = metodosRaw.stream()
                .map(r -> {
                    long cant = aLong(r[1]);
                    long tot = aLong(r[2]);
                    return new MetodoPagoConteo(r[0].toString(), cant, tot, pct(cant, totalPagos));
                })
                .toList();

        String metodoMasUsado = metodosPago.isEmpty() ? "Sin datos" : metodosPago.get(0).metodo();
        long metodoMasUsadoCant = metodosPago.isEmpty() ? 0L : metodosPago.get(0).cantidad();
        double metodoMasUsadoPct = metodosPago.isEmpty() ? 0.0 : metodosPago.get(0).porcentaje();

        List<FuenteGananciaConteo> fuentesGanancia = construirFuentesGanancia(fuenteGanancia, ganComision, ganEnvio);

        List<CategoriaConteo> categorias = repo.obtenerCategoriasMasCompradas(
                        desdeDt, hastaDt, rol, estado, metodoPago, categoria, zona)
                .stream()
                .map(r -> new CategoriaConteo(r[0].toString(), aLong(r[1])))
                .toList();

        String agrupacionTexto = switch (agrupacionEfectiva) {
            case "hora" -> "Por hora";
            case "diaria" -> "Diaria";
            case "mensual" -> "Mensual";
            case "trimestral" -> "Trimestral";
            case "semestral" -> "Semestral";
            case "anual" -> "Anual";
            default -> "Semanal";
        };

        List<String> filtrosActivos = new ArrayList<>();
        if (tieneValor(rol)) filtrosActivos.add("Rol: " + rol);
        if (tieneValor(estado)) filtrosActivos.add("Estado: " + estado);
        if (tieneValor(fuenteGanancia)) filtrosActivos.add("Fuente: " + fuenteGanancia);
        if (tieneValor(metodoPago)) filtrosActivos.add("Pago: " + metodoPago);
        if (tieneValor(categoria)) filtrosActivos.add("Categoría: " + categoria);
        if (tieneValor(zona)) filtrosActivos.add("Zona: " + zona);
        String alcance = filtrosActivos.isEmpty() ? "Consolidado general" : String.join(" · ", filtrosActivos);

        String periodoTexto = inicio.format(FMT_LARGO) + " al " + fin.format(FMT_LARGO);
        String fechaHoy = LocalDate.now().format(FMT_LARGO);
        String codigo = "REP-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        return new ReporteAuditoriaDTO(
                codigo, fechaHoy, periodoTexto, agrupacionTexto, alcance,
                pedidosTotales, pedidosCompletados, pedidosEnCurso, pedidosCancelados,
                ventasTotales, gananciasNetas, ganComision, ganEnvio,
                picoEtiqueta, picoPedidos,
                totalUsuarios, consumidores, negocios, repartidores,
                metodoMasUsado, metodoMasUsadoCant, metodoMasUsadoPct,
                serieTemporal, porEstado, metodosPago, fuentesGanancia, categorias
        );
    }

    /** Prepara el desglose de ganancias según la fuente elegida en el formulario. */
    private List<FuenteGananciaConteo> construirFuentesGanancia(String fuente, long comision, long envio) {
        String f = fuente != null ? fuente.trim().toLowerCase() : "";
        boolean soloComision = f.contains("comisi");
        boolean soloEnvio = f.contains("env");

        long base = soloComision ? comision : (soloEnvio ? envio : comision + envio);
        List<FuenteGananciaConteo> lista = new ArrayList<>();
        if (!soloEnvio) {
            lista.add(new FuenteGananciaConteo("Comisión a negocio (10%)", comision, pct(comision, base)));
        }
        if (!soloComision) {
            lista.add(new FuenteGananciaConteo("Envío (5%)", envio, pct(envio, base)));
        }
        return lista;
    }

    /** Selecciona la proporción de ganancias que se usa en cada punto de la serie. */
    private double factorGanancia(String fuente) {
        String f = fuente != null ? fuente.trim().toLowerCase() : "";
        if (f.contains("comisi")) return PCT_COMISION;
        if (f.contains("env")) return PCT_ENVIO;
        return PCT_COMISION + PCT_ENVIO;
    }

    /** Unifica los valores del formulario con los nombres que esperan las consultas. */
    private String normalizarAgrupacion(String agrupacion) {
        if (agrupacion == null) return "semanal";
        return switch (agrupacion.trim().toLowerCase()) {
            case "por hora", "hora" -> "hora";
            case "diaria", "dia" -> "diaria";
            case "mensual" -> "mensual";
            case "trimestral" -> "trimestral";
            case "semestral" -> "semestral";
            case "anual" -> "anual";
            default -> "semanal";
        };
    }

    private boolean tieneValor(String v) {
        return v != null && !v.isBlank();
    }

    private double pct(long parte, long total) {
        return total > 0 ? Math.round(parte * 1000.0 / total) / 10.0 : 0.0;
    }

    private long aLong(Object v) {
        return v == null ? 0L : ((Number) v).longValue();
    }
}
