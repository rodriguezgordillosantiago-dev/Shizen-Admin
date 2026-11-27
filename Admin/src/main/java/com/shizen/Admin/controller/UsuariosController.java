package com.shizen.Admin.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.shizen.Admin.model.Usuario;
import com.shizen.Admin.repository.PedidoRepository;
import com.shizen.Admin.repository.UsuarioRepository;
import com.shizen.Admin.repository.UsuariosRepository;

@Controller 
public class UsuariosController {

    private final UsuariosRepository usuariosRepository;
    private final UsuarioRepository usuarioRepository;
    private final PedidoRepository pedidoRepository;

    // Constructor para la inyección de dependencias
    public UsuariosController(
            UsuariosRepository usuariosRepository,
            UsuarioRepository usuarioRepository,
            PedidoRepository pedidoRepository) {
        this.usuariosRepository = usuariosRepository;
        this.usuarioRepository = usuarioRepository;
        this.pedidoRepository = pedidoRepository;
    }

    // GET /usuarios -> lista los usuarios con filtros multicriterio y los envía al HTML
    @GetMapping({"/usuarios", "/usuarios.html"})
    public String listarUsuarios(
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) String baneado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Model model) {

        // "Todos" se convierte en null para que la query no filtre por ese campo
        String rolFiltro    = (rol     != null && !rol.equalsIgnoreCase("Todos"))  ? rol     : null;
        String baneadoFiltro= (baneado != null && !baneado.equalsIgnoreCase("todos")) ? baneado : null;

        // Convertir LocalDate a LocalDateTime para la query JPQL
        LocalDateTime fechaDesde = (desde != null) ? desde.atStartOfDay() : null;
        LocalDateTime fechaHasta = (hasta != null) ? hasta.plusDays(1).atStartOfDay() : null;

        List<Usuario> lista = usuariosRepository.buscarConFiltros(buscar, rolFiltro, baneadoFiltro, fechaDesde, fechaHasta);
        List<Usuario> todos = usuariosRepository.findAll();

        long totalCuentas      = todos.size();
        long totalClientes     = contarPorRol(todos, "usuario", "cliente");
        long totalNegocios     = contarPorRol(todos, "negocio");
        long totalRepartidores = contarPorRol(todos, "repartidor");
        long totalBaneados     = todos.stream().filter(u -> Boolean.TRUE.equals(u.getBaneado())).count();
        long totalActivos      = totalCuentas - totalBaneados;

        long porcentajeActivos  = totalCuentas > 0 ? Math.round((totalActivos * 100.0) / totalCuentas) : 100;
        long porcentajeClientes = totalCuentas > 0 ? Math.round((totalClientes * 100.0) / totalCuentas) : 0;

        model.addAttribute("usuarios",           lista);
        model.addAttribute("fotosPorUsuario",    usuariosRepository.obtenerFotosPorUsuario());
        model.addAttribute("totalCuentas",       totalCuentas);
        model.addAttribute("totalClientes",      totalClientes);
        model.addAttribute("totalNegocios",      totalNegocios);
        model.addAttribute("totalRepartidores",  totalRepartidores);
        model.addAttribute("totalActivos",       totalActivos);
        model.addAttribute("totalBaneados",      totalBaneados);
        model.addAttribute("porcentajeActivos",  porcentajeActivos);
        model.addAttribute("porcentajeClientes", porcentajeClientes);
        model.addAttribute("registrosMensuales", construirRegistrosMensuales(todos));
        model.addAttribute("cuentasVisibles",    lista.size());
        model.addAttribute("buscar",             buscar);
        model.addAttribute("rolActual",          rol     != null ? rol     : "Todos");
        model.addAttribute("baneadoActual",      baneado != null ? baneado : "todos");
        model.addAttribute("desdeActual",        desde);
        model.addAttribute("hastaActual",        hasta);

        return "usuarios";
    }

    /** Cuenta los usuarios que coinciden con alguno de los roles indicados. */
    private long contarPorRol(List<Usuario> usuarios, String... roles) {
        return usuarios.stream()
                .filter(u -> u.getRol() != null && java.util.Arrays.stream(roles).anyMatch(r -> r.equalsIgnoreCase(u.getRol())))
                .count();
    }

    /** Construye la serie de registros de los últimos 12 meses para la gráfica de Usuarios. */
    private List<Map<String, Object>> construirRegistrosMensuales(List<Usuario> todos) {
        YearMonth mesActual = YearMonth.now();
        Map<YearMonth, Long> conteoPorMes = new LinkedHashMap<>();
        for (int i = 11; i >= 0; i--) {
            conteoPorMes.put(mesActual.minusMonths(i), 0L);
        }
        for (Usuario u : todos) {
            if (u.getFechaRegistro() != null) {
                YearMonth ym = YearMonth.from(u.getFechaRegistro());
                conteoPorMes.computeIfPresent(ym, (k, v) -> v + 1);
            }
        }
        long maxRegistrosMes = Math.max(1L, conteoPorMes.values().stream().mapToLong(Long::longValue).max().orElse(1L));
        DateTimeFormatter fmtMes = DateTimeFormatter.ofPattern("MMM yy", Locale.forLanguageTag("es-CO"));

        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Map.Entry<YearMonth, Long> entry : conteoPorMes.entrySet()) {
            long val = entry.getValue();
            int alturaPct = val == 0 ? 6 : (int) Math.max(12, Math.round((val * 100.0) / maxRegistrosMes));
            resultado.add(Map.of(
                "mes", entry.getKey().format(fmtMes),
                "cantidad", val,
                "altura", alturaPct,
                "destacado", val > 0 && val == maxRegistrosMes
            ));
        }
        return resultado;
    }

    // Banea al usuario validando que no sea admin ni el propio usuario en sesión, y registra en auditoría
    @PostMapping("/usuarios/{id}/banear")
    @ResponseBody
    public ResponseEntity<?> banearUsuario(
            @PathVariable Integer id,
            @RequestParam(required = false) String motivo,
            Principal principal) {
        Usuario u = usuariosRepository.findById(id).orElseThrow();
        Integer idAdmin = resolverIdAdmin(principal);

        if ("admin".equalsIgnoreCase(u.getRol()) || (idAdmin != null && idAdmin.equals(u.getIdUsuario()))) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "No puedes banear tu propia cuenta ni a otro administrador."
            ));
        }

        u.setBaneado(true);
        u.setMotivoBaneo(motivo);
        u.setFechaBaneo(LocalDateTime.now());
        usuariosRepository.save(u);

        pedidoRepository.registrarAuditoria(
                idAdmin,
                "Baneo de usuario: " + u.getNombre(),
                "usuario",
                id,
                (motivo != null && !motivo.isBlank()) ? motivo : "Sin motivo especificado"
        );

        return ResponseEntity.ok(Map.of(
            "success", true,
            "accion",  "baneado",
            "nombre",  u.getNombre()
        ));
    }

    // Reactiva la cuenta y registra la acción en auditoria_admin
    @PostMapping("/usuarios/{id}/desbanear")
    @ResponseBody
    public ResponseEntity<?> desbanearUsuario(
            @PathVariable Integer id,
            @RequestParam(required = false) String nota,
            Principal principal) {
        Usuario u = usuariosRepository.findById(id).orElseThrow();
        u.setBaneado(false);
        u.setMotivoBaneo(null);
        u.setFechaBaneo(null);
        usuariosRepository.save(u);

        Integer idAdmin = resolverIdAdmin(principal);
        pedidoRepository.registrarAuditoria(
                idAdmin,
                "Desbaneo / reactivación de usuario: " + u.getNombre(),
                "usuario",
                id,
                (nota != null && !nota.isBlank()) ? nota.trim() : "Cuenta reactivada por el administrador"
        );

        return ResponseEntity.ok(Map.of(
            "success", true,
            "accion",  "desbaneado",
            "nombre",  u.getNombre()
        ));
    }

    private Integer resolverIdAdmin(Principal principal) {
        if (principal == null) return null;
        return usuarioRepository.findByEmail(principal.getName())
                .map(Usuario::getIdUsuario)
                .orElse(null);
    }
}
