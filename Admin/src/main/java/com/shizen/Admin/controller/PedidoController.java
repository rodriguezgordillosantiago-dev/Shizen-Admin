package com.shizen.Admin.controller;

import com.shizen.Admin.dto.PedidoResumenDTO;
import com.shizen.Admin.repository.PedidoRepository;
import com.shizen.Admin.repository.UsuarioRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Sirve la pantalla de pedidos y coordina sus consultas, filtros y cambios de estado.
 */
@Controller
public class PedidoController {

    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;

    public PedidoController(PedidoRepository pedidoRepository, UsuarioRepository usuarioRepository) {
        this.pedidoRepository = pedidoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** Renderiza la pantalla de pedidos con las listas iniciales para filtros. */
    @GetMapping({"/pedidos", "/pedidos.html"})
    public String vistaPedidos(Model model) {
        model.addAttribute("negocios", pedidoRepository.listarNegocios());
        model.addAttribute("estados", pedidoRepository.listarEstados());
        model.addAttribute("repartidores", pedidoRepository.listarRepartidores());
        model.addAttribute("metodosPago", pedidoRepository.listarMetodosPago());
        return "pedidos";
    }

    /**
     * Devuelve pedidos, métricas y actividad por hora con los filtros de la pantalla.
     * Las fechas se reciben como límites de calendario y el límite final no se incluye.
     */
    @GetMapping("/api/pedidos")
    @ResponseBody
    public Map<String, Object> consultarPedidos(
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Integer idNegocio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false, defaultValue = "Mensual") String periodo) {

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime desde = (fechaDesde != null) ? fechaDesde.atStartOfDay() : null;
        LocalDateTime hasta = (fechaHasta != null) ? fechaHasta.plusDays(1).atStartOfDay() : null;

        // Si faltan fechas, usa el período rápido; "Todos" no limita el rango.
        if (desde == null && hasta == null && !"Todos".equalsIgnoreCase(periodo)) {
            int dias = "Hoy".equalsIgnoreCase(periodo) ? 1 : ("Semanal".equalsIgnoreCase(periodo) ? 7 : 30);
            desde = LocalDate.now().minusDays(dias - 1).atStartOfDay();
            hasta = LocalDate.now().plusDays(1).atStartOfDay();
        }

        List<PedidoResumenDTO> pedidos = pedidoRepository.buscarPedidos(buscar, estado, idNegocio, desde, hasta);
        Map<String, Object> metricas = pedidoRepository.obtenerMetricas(ahora);
        int[] actividadHoy = pedidoRepository.obtenerActividadPorFranja(buscar, estado, idNegocio, desde, hasta);

        return Map.of(
                "pedidos", pedidos,
                "metricas", metricas,
                "actividadHoy", actividadHoy
        );
    }

    /** Actualiza el estado y registra quién realizó el cambio en la auditoría. */
    @PostMapping("/api/pedidos/{id}/estado")
    @ResponseBody
    public ResponseEntity<?> cambiarEstado(
            @PathVariable Integer id,
            @RequestParam String estado,
            Principal principal) {

        int actualizados = pedidoRepository.actualizarEstado(id, estado);
        if (actualizados == 0) {
            return ResponseEntity.notFound().build();
        }

        Integer idAdmin = null;
        if (principal != null) {
            idAdmin = usuarioRepository.findByEmail(principal.getName())
                    .map(u -> u.getIdUsuario())
                    .orElse(null);
        }
        pedidoRepository.registrarAuditoria(
                idAdmin,
                "Cambió estado de pedido #" + id + " a '" + estado + "'",
                "pedido",
                id,
                "Actualización desde panel Admin"
        );

        return ResponseEntity.ok(Map.of("success", true, "estado", estado));
    }
}
