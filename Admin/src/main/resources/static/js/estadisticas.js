/**
 * Lógica del módulo de Estadísticas: KPIs, gráfica principal por métrica y gráfica circular de categorías.
 */

const fmtCOP = new Intl.NumberFormat("es-CO", {
  style: "currency",
  currency: "COP",
  maximumFractionDigits: 0
});

const fmtNum = new Intl.NumberFormat("es-CO");
const fmtCOPCompact = new Intl.NumberFormat("es-CO", {
  style: "currency",
  currency: "COP",
  notation: "compact",
  maximumFractionDigits: 1
});

const DIAS_POR_PERIODO = {
  Mes: 30,
  Trimestre: 90,
  Año: 365
};

let periodoActualDias = 30;
let metricaActual = "revenue";
let datosGraficaActuales = [];
let resizeFrame = 0;

// Todas las métricas disponibles (sincronizadas con las cajas KPI)
const METRICAS = {
  revenue: {
    titulo: "Ingresos",
    propiedad: "ingresos",
    color: "#315f40",
    monetaria: true,
    tipo: "linea"
  },
  orders: {
    titulo: "Pedidos Totales",
    propiedad: "pedidos",
    color: "#82a878",
    monetaria: false,
    tipo: "barra"
  },
  commission: {
    titulo: "Comisión estimada",
    propiedad: "comisionEstimada",
    color: "#c98743",
    monetaria: true,
    tipo: "linea"
  },
  ticket: {
    titulo: "Ticket Promedio",
    propiedad: "ticketPromedio",
    color: "#5c6986",
    monetaria: true,
    tipo: "linea"
  },
  users: {
    titulo: "Usuarios registrados (nuevos/día)",
    propiedad: "usuariosNuevos",
    color: "#2c5b27",
    monetaria: false,
    tipo: "barra"
  },
  activeUsers: {
    titulo: "Usuarios activos por día",
    propiedad: "usuariosActivos",
    color: "#0284c7",
    monetaria: false,
    tipo: "barra"
  },
  purchases: {
    titulo: "Compras completadas",
    propiedad: "comprasCompletadas",
    color: "#f47b20",
    monetaria: false,
    tipo: "barra"
  }
};

// Paleta de colores para la gráfica circular de categorías
const COLORES_CATEGORIAS = [
  "#9b51e0", // morado
  "#ff6b81", // coral / rosa
  "#ffd93d", // amarillo
  "#4cc9f0", // azul cielo
  "#2c5b27", // verde Shizen
  "#f47b20", // naranja Shizen
  "#adb5bd"  // gris suave
];

// Funciones auxiliares de texto, fechas y tendencias

function ponerTexto(id, valor) {
  const elemento = document.getElementById(id);
  if (elemento) elemento.textContent = valor;
}

function formatearValor(metrica, valor) {
  return metrica.monetaria ? fmtCOP.format(valor) : fmtNum.format(valor);
}

function formatearEtiquetaFecha(fecha) {
  const fechaLocal = new Date(fecha + "T00:00:00");
  return Number.isNaN(fechaLocal.getTime())
    ? fecha
    : new Intl.DateTimeFormat("es-CO", {
        day: "2-digit",
        month: "short"
      }).format(fechaLocal);
}

function ponerCrecimiento(id, porcentaje, sufijo) {
  const elemento = document.getElementById(id);
  if (!elemento) return;

  if (porcentaje === null || porcentaje === undefined) {
    elemento.textContent = "Sin datos del período anterior";
    elemento.style.color = "#6b7280";
    return;
  }

  const flecha = porcentaje >= 0 ? "↑ +" : "↓ ";
  elemento.textContent =
    flecha +
    Math.abs(porcentaje).toFixed(1).replace(".", ",") +
    "% " + sufijo;

  elemento.style.color = porcentaje >= 0 ? "#397438" : "#c45e0a";
}

// Carga los indicadores KPI desde el backend y actualiza las tarjetas

async function cargarKpis(dias) {
  try {
    const respuesta = await fetch("/api/estadisticas/kpis?dias=" + dias);
    if (!respuesta.ok) throw new Error("Error HTTP: " + respuesta.status);

    const k = await respuesta.json();

    ponerTexto("kpi-ingresos", fmtCOP.format(k.ingresos));
    ponerCrecimiento("kpi-ingresos-trend", k.crecimientoIngresosPct, "vs período anterior");

    ponerTexto("kpi-pedidos", fmtNum.format(k.pedidosTotales));
    ponerCrecimiento("kpi-pedidos-trend", k.crecimientoPedidosPct, "vs período anterior");

    ponerTexto("kpi-comision", fmtCOP.format(k.comisionEstimada));
    ponerTexto("kpi-comision-trend", "Estimado al " + k.comisionPct + "%");

    ponerTexto("kpi-ticket", fmtCOP.format(k.ticketPromedio));
    ponerTexto("kpi-ticket-trend", "Ingresos / compras completadas");

    ponerTexto("kpi-usuarios", fmtNum.format(k.usuariosRegistrados));
    ponerTexto("kpi-usuarios-trend", "+" + fmtNum.format(k.usuariosNuevos) + " nuevos en el período");

    ponerTexto("kpi-activos", fmtNum.format(k.usuariosActivos));
    ponerTexto("kpi-activos-trend", "Con compra completada en el período");

    ponerTexto("kpi-compras", fmtNum.format(k.comprasCompletadas));
    ponerTexto("kpi-compras-trend", "Compras completadas en el período");

    ponerTexto("kpi-meta", fmtCOP.format(k.ingresosMesActual));
    if (k.avanceMetaPct === null || k.avanceMetaPct === undefined) {
      ponerTexto("kpi-meta-trend", "Sin meta mensual definida");
    } else {
      ponerTexto(
        "kpi-meta-trend",
        k.avanceMetaPct.toFixed(1).replace(".", ",") + "% de " + fmtCOP.format(k.metaMensual)
      );
    }
  } catch (error) {
    console.error("Error cargando KPI:", error);
  }
}

// Consulta la serie diaria de datos y dibuja la gráfica principal SVG

async function cargarGrafica(dias) {
  const contenedor = document.getElementById("analytics-bars-container");
  datosGraficaActuales = [];
  ponerTexto("analytics-chart-current", "Cargando...");
  if (contenedor) {
    contenedor.innerHTML = '<p class="analytics-chart-message">Cargando datos de la gráfica...</p>';
  }

  try {
    const respuesta = await fetch("/api/estadisticas/grafica?dias=" + dias);
    if (!respuesta.ok) throw new Error("Error HTTP: " + respuesta.status);

    datosGraficaActuales = await respuesta.json();
    dibujarGrafica();
  } catch (error) {
    console.error("Error cargando gráfica:", error);
    ponerTexto("analytics-chart-current", "No disponible");
    if (contenedor) {
      contenedor.innerHTML = '<p class="analytics-chart-message">No se pudo cargar la gráfica.</p>';
    }
  }
}

function dibujarGrafica() {
  const contenedor = document.getElementById("analytics-bars-container");
  if (!contenedor) return;

  const metrica = METRICAS[metricaActual];
  if (!metrica) return;

  ponerTexto("analytics-chart-title", metrica.titulo);
  ponerTexto("analytics-chart-heading-metric", metrica.titulo);

  if (!datosGraficaActuales.length) {
    contenedor.innerHTML = '<p class="analytics-chart-message">No hay datos disponibles para este período.</p>';
    ponerTexto("analytics-chart-current", "Sin datos");
    return;
  }

  const valores = datosGraficaActuales.map(p => {
    const valor = Number(p[metrica.propiedad]);
    return Number.isFinite(valor) ? Math.max(valor, 0) : 0;
  });

  const maximo = Math.max(...valores, 1);
  const alto = 300;
  const margen = { arriba: 18, derecha: 14, abajo: 42, izquierda: 68 };
  const ancho = Math.max(
    contenedor.clientWidth,
    valores.length * 5 + margen.izquierda + margen.derecha,
    300
  );
  const anchoGrafica = ancho - margen.izquierda - margen.derecha;
  const altoGrafica = alto - margen.arriba - margen.abajo;
  const espacio = anchoGrafica / Math.max(valores.length - 1, 1);

  const puntos = valores.map((valor, indice) => ({
    x: margen.izquierda + indice * espacio,
    y: margen.arriba + altoGrafica - (valor / maximo) * altoGrafica,
    valor,
    fecha: datosGraficaActuales[indice].fecha
  }));

  const etiquetasEje = [0, 1, 2, 3, 4].map(indice => {
    const valor = maximo * (4 - indice) / 4;
    const y = margen.arriba + altoGrafica * indice / 4;
    const texto = metrica.monetaria
      ? fmtCOPCompact.format(valor)
      : fmtNum.format(Math.round(valor));

    return `
      <line x1="${margen.izquierda}" y1="${y}"
            x2="${ancho - margen.derecha}" y2="${y}"
            class="analytics-chart-gridline"/>
      <text x="${margen.izquierda - 10}" y="${y + 3}"
            text-anchor="end" class="analytics-chart-axis-label">${texto}</text>
    `;
  });

  const puntosLinea = puntos.map(p => `${p.x},${p.y}`).join(" ");
  const marcasFecha = [
    { indice: 0, ancla: "start" },
    { indice: Math.floor((puntos.length - 1) / 4), ancla: "middle" },
    { indice: Math.floor((puntos.length - 1) / 2), ancla: "middle" },
    { indice: Math.floor((puntos.length - 1) * 3 / 4), ancla: "middle" },
    { indice: puntos.length - 1, ancla: "end" }
  ].filter((p, indice, lista) =>
    lista.findIndex(elemento => elemento.indice === p.indice) === indice
  ).map(({ indice, ancla }) => {
    const punto = puntos[indice];
    return `
      <text x="${punto.x}" y="${alto - 12}" text-anchor="${ancla}"
            class="analytics-chart-axis-label">${formatearEtiquetaFecha(punto.fecha)}</text>
    `;
  }).join("");

  const forma = metrica.tipo === "barra"
    ? puntos.map(p => {
        const anchoBarra = Math.max(2, Math.min(18, anchoGrafica / puntos.length * 0.68));
        const alturaBarra = margen.arriba + altoGrafica - p.y;
        const fechaTxt = formatearEtiquetaFecha(p.fecha);
        const valorTxt = formatearValor(metrica, p.valor);
        return `
          <rect x="${p.x - anchoBarra / 2}" y="${p.y}"
                width="${anchoBarra}" height="${Math.max(alturaBarra, 1)}"
                rx="2" fill="${metrica.color}" class="analytics-chart-bar"
                data-fecha="${fechaTxt}" data-valor="${valorTxt}"
                data-x="${p.x}" data-y="${p.y}" style="cursor:pointer"></rect>
        `;
      }).join("")
    : `
      <polyline points="${puntosLinea}" class="analytics-chart-line"
                stroke="${metrica.color}"/>
      ${puntos.map(p => {
        const fechaTxt = formatearEtiquetaFecha(p.fecha);
        const valorTxt = formatearValor(metrica, p.valor);
        return `
          <circle cx="${p.x}" cy="${p.y}" r="4.5"
                  fill="${metrica.color}" class="analytics-chart-point"
                  data-fecha="${fechaTxt}" data-valor="${valorTxt}"
                  data-x="${p.x}" data-y="${p.y}" style="cursor:pointer"></circle>
        `;
      }).join("")}
    `;

  const puntoActual = puntos[puntos.length - 1];
  const resumenDefault = `${formatearValor(metrica, puntoActual.valor)} · ${formatearEtiquetaFecha(puntoActual.fecha)}`;
  ponerTexto("analytics-chart-current", resumenDefault);

  contenedor.style.position = "relative";
  contenedor.innerHTML = `
    <svg viewBox="0 0 ${ancho} ${alto}"
         style="width: ${ancho}px; height: ${alto}px; max-width: none"
         role="img"
         aria-label="Gráfica de ${metrica.titulo}, ${datosGraficaActuales.length} días">
      ${etiquetasEje.join("")}
      ${forma}
      ${marcasFecha}
    </svg>
    <div id="analytics-chart-tooltip" class="categories-tooltip" style="display:none; border-color:${metrica.color};"></div>
  `;

  // Tooltip flotante: cada barra/punto guarda fecha, valor y coordenadas en atributos data-*
  // para posicionar #analytics-chart-tooltip justo encima al pasar el cursor
  const tooltip = document.getElementById("analytics-chart-tooltip");
  contenedor.querySelectorAll("[data-valor]").forEach(el => {
    el.addEventListener("mouseenter", () => {
      if (tooltip) {
        tooltip.innerHTML = `<div class="tooltip-title">${el.dataset.fecha}</div><div class="tooltip-val"><strong>${el.dataset.valor}</strong></div>`;
        tooltip.style.left = `${Number(el.dataset.x)}px`;
        tooltip.style.top = `${Math.max(36, Number(el.dataset.y) - 10)}px`;
        tooltip.style.display = "block";
      }
      ponerTexto("analytics-chart-current", `${el.dataset.valor} · ${el.dataset.fecha}`);
    });
    el.addEventListener("mouseleave", () => {
      if (tooltip) tooltip.style.display = "none";
      ponerTexto("analytics-chart-current", resumenDefault);
    });
  });
}

// Consulta las categorías más compradas y dibuja la gráfica circular (dona)

async function cargarCategorias(dias) {
  const donutEl = document.getElementById("categorias-donut");
  const legendEl = document.getElementById("categorias-legend");
  if (!donutEl || !legendEl) return;

  try {
    const res = await fetch("/api/estadisticas/categorias?dias=" + dias);
    if (!res.ok) throw new Error("Error HTTP: " + res.status);
    const categorias = await res.json();
    dibujarDonutCategorias(categorias);
  } catch (err) {
    console.error("Error cargando categorías:", err);
    donutEl.innerHTML = '<p class="analytics-chart-message">No disponible</p>';
  }
}

function dibujarDonutCategorias(categorias) {
  const donutEl = document.getElementById("categorias-donut");
  const legendEl = document.getElementById("categorias-legend");
  const tooltipEl = document.getElementById("categorias-tooltip");
  if (!donutEl || !legendEl) return;

  legendEl.replaceChildren();
  if (!categorias || !categorias.length) {
    donutEl.innerHTML = '<p class="analytics-chart-message">Sin compras en este período.</p>';
    return;
  }

  const total = categorias.reduce((sum, c) => sum + Number(c.cantidad || 0), 0);
  if (total === 0) {
    donutEl.innerHTML = '<p class="analytics-chart-message">Sin compras registradas.</p>';
    return;
  }

  // Construir leyenda vertical a la izquierda
  categorias.forEach((cat, idx) => {
    const color = COLORES_CATEGORIAS[idx % COLORES_CATEGORIAS.length];
    const li = document.createElement("li");
    li.className = "categories-legend-item";
    li.innerHTML = `
      <span class="categories-legend-left">
        <span class="categories-legend-dot" style="background:${color}"></span>
        <span class="categories-legend-name">${cat.categoria}</span>
      </span>
      <span class="categories-legend-val">${fmtNum.format(cat.cantidad)}</span>
    `;
    legendEl.appendChild(li);
  });

  // Construir Dona SVG
  const size = 220;
  const cx = size / 2;
  const cy = size / 2;
  const rOuter = 98;
  const rInner = 46;

  let anguloActual = -Math.PI / 2; // empieza arriba (12 en punto)
  const paths = categorias.map((cat, idx) => {
    const valor = Number(cat.cantidad || 0);
    const fraccion = valor / total;
    const angulo = fraccion * Math.PI * 2;
    const a1 = anguloActual;
    const a2 = anguloActual + angulo;
    anguloActual = a2;

    const largeArc = angulo > Math.PI ? 1 : 0;
    const x1o = cx + rOuter * Math.cos(a1);
    const y1o = cy + rOuter * Math.sin(a1);
    const x2o = cx + rOuter * Math.cos(a2);
    const y2o = cy + rOuter * Math.sin(a2);

    const x1i = cx + rInner * Math.cos(a2);
    const y1i = cy + rInner * Math.sin(a2);
    const x2i = cx + rInner * Math.cos(a1);
    const y2i = cy + rInner * Math.sin(a1);

    const d = [
      `M ${x1o.toFixed(2)} ${y1o.toFixed(2)}`,
      `A ${rOuter} ${rOuter} 0 ${largeArc} 1 ${x2o.toFixed(2)} ${y2o.toFixed(2)}`,
      `L ${x1i.toFixed(2)} ${y1i.toFixed(2)}`,
      `A ${rInner} ${rInner} 0 ${largeArc} 0 ${x2i.toFixed(2)} ${y2i.toFixed(2)}`,
      "Z"
    ].join(" ");

    const color = COLORES_CATEGORIAS[idx % COLORES_CATEGORIAS.length];
    const pct = (fraccion * 100).toFixed(2);
    return `<path d="${d}" fill="${color}" stroke="#ffffff" stroke-width="1.5"
                  class="donut-slice"
                  data-nombre="${cat.categoria}"
                  data-cantidad="${fmtNum.format(valor)}"
                  data-pct="${pct}"></path>`;
  }).join("");

  donutEl.innerHTML = `<svg viewBox="0 0 ${size} ${size}" class="categories-donut-svg" role="img" aria-label="Categorías más compradas">${paths}</svg>`;

  // Eventos hover para el tooltip flotante estilo imagen
  if (tooltipEl) {
    donutEl.querySelectorAll(".donut-slice").forEach(slice => {
      slice.addEventListener("mouseenter", (e) => {
        const nombre = slice.dataset.nombre;
        const cant = slice.dataset.cantidad;
        const pct = slice.dataset.pct;
        tooltipEl.innerHTML = `<div class="tooltip-title">${nombre}</div><div class="tooltip-val"><strong>${cant}</strong> (${pct}%)</div>`;
        tooltipEl.style.borderColor = slice.getAttribute("fill");
        tooltipEl.hidden = false;
      });
      slice.addEventListener("mousemove", (e) => {
        const rect = donutEl.parentElement.getBoundingClientRect();
        tooltipEl.style.left = (e.clientX - rect.left) + "px";
        tooltipEl.style.top = (e.clientY - rect.top - 14) + "px";
      });
      slice.addEventListener("mouseleave", () => {
        tooltipEl.hidden = true;
      });
    });
  }
}

// Cambia el periodo de consulta o la métrica seleccionada al hacer clic en botones o tarjetas KPI

function setAnalyticsPeriod(nombre) {
  if (!Object.prototype.hasOwnProperty.call(DIAS_POR_PERIODO, nombre)) return;
  periodoActualDias = DIAS_POR_PERIODO[nombre];

  document.querySelectorAll(".analytics-period-btn").forEach(boton => {
    const activo = boton.dataset.period === nombre;
    boton.classList.toggle("active", activo);
    boton.setAttribute("aria-pressed", String(activo));
  });

  ponerTexto("analytics-period-info", `Consultando los últimos ${periodoActualDias} días, incluido hoy.`);
  ponerTexto("analytics-chart-current", "Cargando...");
  cargarKpis(periodoActualDias);
  cargarGrafica(periodoActualDias);
  cargarCategorias(periodoActualDias);
}

function setAnalyticsMetric(metrica, clickedCard = null) {
  if (!Object.prototype.hasOwnProperty.call(METRICAS, metrica)) return;
  metricaActual = metrica;

  // Resaltar la caja KPI activa
  document.querySelectorAll(".analytics-kpi-card[data-metric]").forEach(card => {
    const esActiva = clickedCard
      ? card === clickedCard
      : card.dataset.metric === metrica && !card.querySelector("#kpi-meta");
    card.classList.toggle("analytics-kpi-card--featured", esActiva);
  });

  dibujarGrafica();
}

// Inicializa los controles, carga inicial de KPIs, gráfica principal y categorías

window.addEventListener("resize", () => {
  if (!datosGraficaActuales.length) return;
  window.cancelAnimationFrame(resizeFrame);
  resizeFrame = window.requestAnimationFrame(dibujarGrafica);
});

document.addEventListener("DOMContentLoaded", function () {
  ponerTexto("analytics-period-info", `Consultando los últimos ${periodoActualDias} días, incluido hoy.`);
  cargarKpis(periodoActualDias);
  cargarGrafica(periodoActualDias);
  cargarCategorias(periodoActualDias);

  // Hacer clickeables todas las cajas KPI para cambiar la gráfica
  document.querySelectorAll(".analytics-kpi-card[data-metric]").forEach(card => {
    card.addEventListener("click", () => {
      setAnalyticsMetric(card.dataset.metric, card);
    });
    card.addEventListener("keydown", (e) => {
      if (e.key === "Enter" || e.key === " ") {
        e.preventDefault();
        setAnalyticsMetric(card.dataset.metric, card);
      }
    });
  });
});
