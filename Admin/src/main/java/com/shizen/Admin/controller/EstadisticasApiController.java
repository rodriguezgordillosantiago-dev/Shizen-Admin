package com.shizen.Admin.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shizen.Admin.dto.CategoriaEstadistica;
import com.shizen.Admin.dto.EstadisticaPunto;
import com.shizen.Admin.dto.KpiResponse;
import com.shizen.Admin.service.EstadisticasService;

/** Expone los datos que la pantalla de estadísticas usa para sus tarjetas y gráficas. */
@RestController
@RequestMapping("/api/estadisticas")
public class EstadisticasApiController {

    private final EstadisticasService service;

    public EstadisticasApiController(EstadisticasService service) {
        this.service = service;
    }

    /**
     * Devuelve los indicadores y sus comparaciones para el período solicitado.
     * @param dias cantidad de días, limitada al rango de 1 a 3650.
     */
    @GetMapping("/kpis")
    public KpiResponse kpis(@RequestParam(defaultValue = "30") int dias) {
        int diasValidos = Math.max(1, Math.min(dias, 3650));
        return service.obtenerKpis(diasValidos);
    }

    /**
     * Devuelve los datos diarios que alimentan la gráfica principal.
     * @param dias cantidad de días, limitada al rango de 1 a 3650.
     */
    @GetMapping("/grafica")
    public List<EstadisticaPunto> grafica(@RequestParam(defaultValue = "30") int dias) {
        int diasValidos = Math.max(1, Math.min(dias, 3650));
        return service.obtenerDatosGrafica(diasValidos);
    }

    /**
     * Devuelve las categorías con más unidades vendidas en el período.
     * @param dias cantidad de días, limitada al rango de 1 a 3650.
     */
    @GetMapping("/categorias")
    public List<CategoriaEstadistica> categorias(@RequestParam(defaultValue = "30") int dias) {
        int diasValidos = Math.max(1, Math.min(dias, 3650));
        return service.obtenerCategoriasMasCompradas(diasValidos);
    }
}
