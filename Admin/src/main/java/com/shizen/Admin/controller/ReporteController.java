package com.shizen.Admin.controller;

import com.shizen.Admin.dto.ReporteAuditoriaDTO;
import com.shizen.Admin.repository.PedidoRepository;
import com.shizen.Admin.repository.ReporteRepository;
import com.shizen.Admin.service.ReporteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Prepara los filtros y datos que necesita la pantalla del reporte de auditoría. */
@Controller
public class ReporteController {

    private static final Map<String, String> TITULOS_PLANTILLA = Map.of(
            "consolidado", "Consolidado General (Ejecutivo)",
            "financiero", "Cierre Financiero y Recaudo",
            "operacion", "Operación y Horas Pico",
            "crecimiento", "Crecimiento y Retención",
            "comercial", "Rendimiento Comercial / Menú",
            "personalizado", "Reporte Personalizado"
    );

    private final ReporteService reporteService;
    private final PedidoRepository pedidoRepository;
    private final ReporteRepository reporteRepository;

    public ReporteController(
            ReporteService reporteService,
            PedidoRepository pedidoRepository,
            ReporteRepository reporteRepository) {
        this.reporteService = reporteService;
        this.pedidoRepository = pedidoRepository;
        this.reporteRepository = reporteRepository;
    }

    /** Genera el reporte con los filtros recibidos y prepara sus opciones para la vista. */
    @GetMapping("/reportes/auditoria")
    public String generarAuditoria(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false, defaultValue = "Semanal") String agrupacion,
            @RequestParam(required = false, defaultValue = "consolidado") String plantilla,
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String fuenteGanancia,
            @RequestParam(required = false) String metodoPago,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String zona,
            @RequestParam(required = false) List<String> secciones,
            @RequestParam(required = false) List<String> graficas,
            @RequestParam(required = false) String aplicado,
            Model model) {

        LocalDate desdeEfectivo = (fechaDesde != null) ? fechaDesde : LocalDate.now().minusDays(29);
        LocalDate hastaEfectivo = (fechaHasta != null) ? fechaHasta : LocalDate.now();

        boolean esEnvioFormulario = "1".equals(aplicado);
        // La descarga del PDF se habilita después de enviar el formulario.
        model.addAttribute("reporteGenerado", esEnvioFormulario);
        List<String> seccionesEfectivas = esEnvioFormulario
                ? (secciones != null ? secciones : List.of())
                : List.of("picos", "usuarios", "ganancias");
        List<String> graficasEfectivas = esEnvioFormulario
                ? (graficas != null ? graficas : List.of())
                : List.of("pedidos", "ganancias", "ventas", "usuariosActivos");

        ReporteAuditoriaDTO reporte = reporteService.generarAuditoria(
                desdeEfectivo, hastaEfectivo, agrupacion, rol, estado, fuenteGanancia, metodoPago, categoria, zona);

        model.addAttribute("reporte", reporte);
        model.addAttribute("metodosPagoLista", pedidoRepository.listarMetodosPago());
        model.addAttribute("categoriasLista", reporteRepository.listarCategorias());
        model.addAttribute("zonasLista", reporteRepository.listarZonas());

        model.addAttribute("fPlantilla", plantilla);
        model.addAttribute("tituloInforme", TITULOS_PLANTILLA.getOrDefault(plantilla, "Reporte Administrativo"));
        model.addAttribute("fDesde", desdeEfectivo.toString());
        model.addAttribute("fHasta", hastaEfectivo.toString());
        model.addAttribute("fAgrupacion", agrupacion);
        model.addAttribute("fRol", rol != null ? rol : "");
        model.addAttribute("fEstado", estado != null ? estado : "");
        model.addAttribute("fFuente", fuenteGanancia != null ? fuenteGanancia : "");
        model.addAttribute("fMetodoPago", metodoPago != null ? metodoPago : "");
        model.addAttribute("fCategoria", categoria != null ? categoria : "");
        model.addAttribute("fZona", zona != null ? zona : "");

        model.addAttribute("incPicos", seccionesEfectivas.contains("picos"));
        model.addAttribute("incUsuarios", seccionesEfectivas.contains("usuarios"));
        model.addAttribute("incGanancias", seccionesEfectivas.contains("ganancias"));

        model.addAttribute("grafPedidos", graficasEfectivas.contains("pedidos"));
        model.addAttribute("grafGanancias", graficasEfectivas.contains("ganancias"));
        model.addAttribute("grafVentas", graficasEfectivas.contains("ventas"));
        model.addAttribute("grafCompras", graficasEfectivas.contains("compras"));
        model.addAttribute("grafTicket", graficasEfectivas.contains("ticket"));
        model.addAttribute("grafUsuariosNuevos", graficasEfectivas.contains("usuariosNuevos"));
        model.addAttribute("grafUsuariosActivos", graficasEfectivas.contains("usuariosActivos"));
        model.addAttribute("grafMetodosPago", graficasEfectivas.contains("metodosPago"));
        model.addAttribute("grafCategorias", graficasEfectivas.contains("categorias"));
        model.addAttribute("grafEstados", graficasEfectivas.contains("estados"));

        return "auditoria-reporte";
    }
}
