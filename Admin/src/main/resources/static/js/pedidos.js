/**
 * Lógica de la pantalla de Pedidos: filtros multicriterio, métricas, gráfica por franja y reporte de auditoría.
 */

let ordersPeriod = "Mensual";
let loadedOrders = [];

const fmtCOP = new Intl.NumberFormat("es-CO", {
  style: "currency",
  currency: "COP",
  maximumFractionDigits: 0,
});

// Consulta pedidos, métricas y actividad por franja horaria al backend según filtros activos
async function fetchPedidos() {
  const params = new URLSearchParams();
  const buscar = document.getElementById("orders-search-input")?.value.trim();
  const estado = document.getElementById("orders-filter-estado")?.value;
  const idNegocio = document.getElementById("orders-filter-negocio")?.value;
  const fechaDesde = document.getElementById("orders-filter-desde")?.value;
  const fechaHasta = document.getElementById("orders-filter-hasta")?.value;

  if (buscar) params.set("buscar", buscar);
  if (estado && estado !== "Todos") params.set("estado", estado);
  if (idNegocio) params.set("idNegocio", idNegocio);
  if (fechaDesde) params.set("fechaDesde", fechaDesde);
  if (fechaHasta) params.set("fechaHasta", fechaHasta);
  params.set("periodo", ordersPeriod);

  try {
    const res = await fetch("/api/pedidos?" + params.toString());
    if (!res.ok) throw new Error("Error HTTP " + res.status);
    const data = await res.json();

    loadedOrders = data.pedidos || [];
    const cntEl = document.getElementById("orders-count-visible");
    if (cntEl) {
      const n = loadedOrders.length;
      cntEl.textContent = `${n} ${n === 1 ? "pedido visible" : "pedidos visibles"}`;
    }
    renderOrdersChips();
    renderSalesMetrics(data.metricas);
    renderOrdersTable();
    renderLiveChart(data.actividadHoy || []);
  } catch (err) {
    console.error("Error cargando pedidos:", err);
  }
}

// Actualiza las tarjetas superiores de ventas (Hoy, Semana, Mes)
function renderSalesMetrics(metricas) {
  if (!metricas) return;
  const map = [
    { key: "hoy", val: "today-sales-value", cnt: "today-sales-count", tr: "today-sales-trend" },
    { key: "semana", val: "week-sales-value", cnt: "week-sales-count", tr: "week-sales-trend" },
    { key: "mes", val: "month-sales-value", cnt: "month-sales-count", tr: "month-sales-trend" },
  ];

  map.forEach(({ key, val, cnt, tr }) => {
    const m = metricas[key];
    if (!m) return;
    document.getElementById(val).textContent = fmtCOP.format(m.ventas || 0);
    const n = m.cantidad || 0;
    document.getElementById(cnt).textContent = `${n} ${n === 1 ? "pedido" : "pedidos"}`;
    document.getElementById(tr).textContent = m.tendencia || "0%";
  });
}

// Renderiza las filas de la tabla de pedidos y enlaza el botón de detalle
function getStatusBadgeClass(estado) {
  const e = (estado || "").toLowerCase();
  if (e.includes("entregado")) return "entregado";
  if (e.includes("camino") || e.includes("recogido") || e.includes("asignado")) return "en-camino";
  return "nuevo";
}

function renderOrdersTable() {
  const tbody = document.getElementById("orders-table-body");
  if (!tbody) return;
  tbody.replaceChildren();

  if (!loadedOrders.length) {
    const tr = document.createElement("tr");
    const td = document.createElement("td");
    td.colSpan = 7;
    td.style.textAlign = "center";
    td.style.padding = "28px";
    td.style.color = "#748075";
    td.textContent = "No se encontraron pedidos con los filtros seleccionados.";
    tr.appendChild(td);
    tbody.appendChild(tr);
    return;
  }

  loadedOrders.forEach((order) => {
    const row = document.createElement("tr");
    const fechaTxt = order.fechaCreacion
      ? new Date(order.fechaCreacion).toLocaleDateString("es-CO")
      : "—";

    const cells = [
      "#" + order.idPedido,
      fechaTxt,
      order.cliente || "Cliente",
      order.negocio || order.descripcion || "—",
      fmtCOP.format(order.monto || 0) + " COP",
    ];

    cells.forEach((val) => {
      const td = document.createElement("td");
      td.textContent = val;
      row.appendChild(td);
    });

    const statusTd = document.createElement("td");
    const badge = document.createElement("span");
    badge.className = "badge-status " + getStatusBadgeClass(order.estado);
    badge.textContent = order.estado || "pendiente";
    statusTd.appendChild(badge);
    row.appendChild(statusTd);

    const actionTd = document.createElement("td");
    actionTd.style.textAlign = "right";
    const btn = document.createElement("button");
    btn.type = "button";
    btn.className = "btn-icon-action";
    btn.title = "Ver detalle";
    btn.setAttribute("aria-label", "Ver detalle");
    const eyeIcon = document.createElement("i");
    eyeIcon.setAttribute("data-lucide", "eye");
    eyeIcon.style.cssText = "width:16px;height:16px";
    btn.appendChild(eyeIcon);
    btn.addEventListener("click", () => showOrderDetail(order.idPedido));
    actionTd.appendChild(btn);
    row.appendChild(actionTd);

    tbody.appendChild(row);
  });

  if (window.lucide) lucide.createIcons();
}

// Dibuja la gráfica SVG tipo montaña de pedidos agrupados en franjas de 90 minutos
function renderLiveChart(buckets) {
  const chart = document.getElementById("orders-activity-chart");
  if (!chart) return;
  const data = buckets.length === 16 ? buckets : Array(16).fill(0);
  const maxCount = Math.max(1, ...data);

  const w = 640, h = 88;
  const padX = 8, padTop = 10, padBottom = 6;
  const cw = w - padX * 2;
  const ch = h - padTop - padBottom;

  const pts = data.map((count, i) => {
    const x = padX + (i * cw) / (data.length - 1);
    const y = padTop + ch - (count / maxCount) * ch;
    const minInicio = i * 90;
    const minFin = minInicio + 90;
    const fmtH = (m) =>
      String(Math.floor(m / 60)).padStart(2, "0") + ":" + String(m % 60).padStart(2, "0");
    return { x, y, count, hora: `${fmtH(minInicio)}–${fmtH(minFin)}` };
  });

  const linePts = pts.map((p) => `${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(" ");
  const areaPath =
    `M ${pts[0].x.toFixed(1)},${(padTop + ch).toFixed(1)} ` +
    pts.map((p) => `L ${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(" ") +
    ` L ${pts[pts.length - 1].x.toFixed(1)},${(padTop + ch).toFixed(1)} Z`;

  const circles = pts
    .map(
      (p) =>
        `<circle cx="${p.x.toFixed(1)}" cy="${p.y.toFixed(1)}" r="4.5" fill="#39783D" stroke="#FFFEF9" stroke-width="1.5" ` +
        `data-hora="${p.hora}" data-count="${p.count}" data-xpct="${((p.x / w) * 100).toFixed(2)}" data-ypct="${((p.y / h) * 100).toFixed(2)}" style="cursor:pointer"></circle>`
    )
    .join("");

  chart.style.position = "relative";
  chart.innerHTML = `
    <svg viewBox="0 0 ${w} ${h}" style="width:100%;height:${h}px;display:block;overflow:visible" role="img" aria-label="Actividad de pedidos por franja horaria">
      <defs>
        <linearGradient id="orders-mountain-grad" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="#39783D" stop-opacity="0.32" />
          <stop offset="100%" stop-color="#39783D" stop-opacity="0.02" />
        </linearGradient>
      </defs>
      <path d="${areaPath}" fill="url(#orders-mountain-grad)" />
      <polyline points="${linePts}" fill="none" stroke="#39783D" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" />
      ${circles}
    </svg>
    <div id="orders-chart-tooltip" style="display:none;position:absolute;transform:translate(-50%,-100%);background:#fff;border:2px solid #39783D;border-radius:8px;padding:5px 9px;font-size:12px;line-height:1.3;text-align:center;box-shadow:0 6px 18px rgba(0,0,0,.14);pointer-events:none;white-space:nowrap;z-index:20;"></div>
  `;

  // Tooltip flotante: cada punto guarda su franja, conteo y posición en % (data-xpct, data-ypct)
  // para ubicar #orders-chart-tooltip sobre el punto sin depender del ancho del contenedor
  const tooltip = document.getElementById("orders-chart-tooltip");
  chart.querySelectorAll("circle[data-hora]").forEach((c) => {
    c.addEventListener("mouseenter", () => {
      if (!tooltip) return;
      const n = Number(c.dataset.count);
      tooltip.innerHTML = `<div style="color:#4b5563;font-weight:500">${c.dataset.hora}</div><div style="color:#111827"><strong>${n} ${n === 1 ? "pedido" : "pedidos"}</strong></div>`;
      tooltip.style.left = `${c.dataset.xpct}%`;
      tooltip.style.top = `calc(${c.dataset.ypct}% - 10px)`;
      tooltip.style.display = "block";
    });
    c.addEventListener("mouseleave", () => {
      if (tooltip) tooltip.style.display = "none";
    });
  });
}

// Cambia el periodo rápido (Mensual, Semanal, Todos)
function setOrdersPeriod(period) {
  ordersPeriod = period;
  document.getElementById("period-btn-mensual")?.setAttribute("aria-pressed", String(period === "Mensual"));
  document.getElementById("period-btn-semanal")?.setAttribute("aria-pressed", String(period === "Semanal"));
  document.getElementById("period-btn-todos")?.setAttribute("aria-pressed", String(period === "Todos"));
  fetchPedidos();
}

function aplicarFiltrosPedidos() {
  const desdeEl = document.getElementById("orders-filter-desde");
  const hastaEl = document.getElementById("orders-filter-hasta");
  if (desdeEl?.value && hastaEl?.value && hastaEl.value < desdeEl.value) {
    hastaEl.value = desdeEl.value;
  }
  fetchPedidos();
  const panel = document.getElementById("orders-multifilter-panel");
  const btn = document.getElementById("orders-filter-toggle");
  if (panel?.classList.contains("open")) {
    panel.classList.remove("open");
    if (btn) btn.setAttribute("aria-expanded", "false");
  }
}

function renderOrdersChips() {
  const chipsEl = document.getElementById("orders-filter-chips");
  const dotEl = document.getElementById("orders-filter-dot");
  const estEl = document.getElementById("orders-filter-estado");
  const negEl = document.getElementById("orders-filter-negocio");
  const desdeEl = document.getElementById("orders-filter-desde");
  const hastaEl = document.getElementById("orders-filter-hasta");
  if (!chipsEl) return;

  const fmtDate = (s) =>
    new Date(s + "T12:00:00")
      .toLocaleDateString("es-CO", { day: "numeric", month: "short", year: "numeric" })
      .replace(/\./g, "");

  const activos = [];
  if (estEl && estEl.value && estEl.value !== "Todos") {
    activos.push({ key: "estado", label: "Estado: " + estEl.value });
  }
  if (negEl && negEl.value) {
    const txt = negEl.options[negEl.selectedIndex]?.text || "Negocio";
    activos.push({ key: "negocio", label: "Negocio: " + txt });
  }
  if (desdeEl && desdeEl.value) {
    activos.push({ key: "desde", label: "Desde " + fmtDate(desdeEl.value) });
  }
  if (hastaEl && hastaEl.value) {
    activos.push({ key: "hasta", label: "Hasta " + fmtDate(hastaEl.value) });
  }

  chipsEl.innerHTML = activos
    .map(
      (a) =>
        `<span class="flt-chip">${a.label}<button type="button" data-k="${a.key}" aria-label="Quitar filtro"><i data-lucide="x"></i></button></span>`
    )
    .join("");

  chipsEl.querySelectorAll("button[data-k]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const k = btn.dataset.k;
      if (k === "estado" && estEl) estEl.value = "Todos";
      if (k === "negocio" && negEl) negEl.value = "";
      if (k === "desde" && desdeEl) desdeEl.value = "";
      if (k === "hasta" && hastaEl) hastaEl.value = "";
      fetchPedidos();
    });
  });

  if (dotEl) {
    dotEl.hidden = activos.length === 0;
    dotEl.textContent = String(activos.length);
  }
  if (hastaEl && desdeEl) {
    hastaEl.min = desdeEl.value || "";
  }
  if (window.lucide) lucide.createIcons();
}

function showOrdersForPeriod(period) {
  setOrdersPeriod(period);
  document.querySelector(".table-card")?.scrollIntoView({ behavior: "smooth", block: "start" });
}

function limpiarFiltrosPedidos() {
  const s = document.getElementById("orders-search-input");
  const e = document.getElementById("orders-filter-estado");
  const n = document.getElementById("orders-filter-negocio");
  const d = document.getElementById("orders-filter-desde");
  const h = document.getElementById("orders-filter-hasta");
  if (s) s.value = "";
  if (e) e.value = "Todos";
  if (n) n.value = "";
  if (d) d.value = "";
  if (h) h.value = "";
  setOrdersPeriod("Mensual");
}

// Abre el modal de detalle del pedido y permite actualizar su estado
let currentDetailOrderId = null;

function showOrderDetail(orderId) {
  const order = loadedOrders.find((item) => item.idPedido === orderId);
  if (!order) return;
  currentDetailOrderId = orderId;

  const initials = (order.cliente || "CL")
    .split(/\s+/)
    .slice(0, 2)
    .map((n) => n[0] || "")
    .join("")
    .toUpperCase();

  const formattedDate = order.fechaCreacion
    ? new Date(order.fechaCreacion).toLocaleString("es-CO", { dateStyle: "medium", timeStyle: "short" })
    : "—";

  document.getElementById("detail-order-eyebrow").textContent = `#${order.idPedido}`;
  document.getElementById("detail-order-initials").textContent = initials || "CL";
  document.getElementById("detail-order-customer").textContent = order.cliente || "Cliente";
  document.getElementById("detail-order-date").textContent = formattedDate;

  const badge = document.getElementById("detail-order-status-badge");
  badge.className = "badge-status " + getStatusBadgeClass(order.estado);
  badge.textContent = order.estado || "pendiente";

  document.getElementById("detail-order-map-address").textContent = order.direccionEntrega || "Sin dirección";
  document.getElementById("detail-order-dish-card").textContent = order.negocio + " · " + (order.descripcion || "");
  document.getElementById("detail-order-amount-card").textContent = fmtCOP.format(order.monto || 0) + " COP";
  document.getElementById("detail-order-date-card").textContent = formattedDate;
  document.getElementById("detail-order-id-card").textContent = `#${order.idPedido}`;

  const selectEstado = document.getElementById("detail-order-new-status");
  if (selectEstado) selectEstado.value = order.estado || "pendiente";

  document.getElementById("view-order-modal").classList.add("open");
}

function closeOrderDetailModal() {
  document.getElementById("view-order-modal")?.classList.remove("open");
}

async function guardarEstadoPedido() {
  if (!currentDetailOrderId) return;
  const nuevoEstado = document.getElementById("detail-order-new-status")?.value;
  if (!nuevoEstado) return;

  const form = new URLSearchParams();
  form.set("estado", nuevoEstado);

  const res = await fetch(`/api/pedidos/${currentDetailOrderId}/estado`, {
    method: "POST",
    body: form,
  });

  if (res.ok) {
    closeOrderDetailModal();
    await fetchPedidos();
    if (window.Swal) {
      Swal.fire({
        icon: "success",
        title: "Estado actualizado",
        text: `El pedido #${currentDetailOrderId} ahora está "${nuevoEstado}".`,
        confirmButtonColor: "#2c5b27",
      });
    }
  }
}

// Sincroniza los filtros actuales y navega al reporte de auditoría en la misma pestaña
function abrirModalReporte() {
  // Sincroniza los filtros actuales de Pedidos al modal del reporte
  const d = document.getElementById("orders-filter-desde")?.value || "";
  const h = document.getElementById("orders-filter-hasta")?.value || "";
  const e = document.getElementById("orders-filter-estado")?.value || "Todos";
  const n = document.getElementById("orders-filter-negocio")?.value || "";

  if (document.getElementById("rep-fecha-desde")) document.getElementById("rep-fecha-desde").value = d;
  if (document.getElementById("rep-fecha-hasta")) document.getElementById("rep-fecha-hasta").value = h;
  if (document.getElementById("rep-estado")) document.getElementById("rep-estado").value = e;
  if (document.getElementById("rep-negocio")) document.getElementById("rep-negocio").value = n;

  document.getElementById("report-filter-modal")?.classList.add("open");
}

function cerrarModalReporte() {
  document.getElementById("report-filter-modal")?.classList.remove("open");
}

function generarReporteAuditoria(e) {
  if (e) e.preventDefault();
  const params = new URLSearchParams();
  const desde = document.getElementById("rep-fecha-desde")?.value;
  const hasta = document.getElementById("rep-fecha-hasta")?.value;
  const estado = document.getElementById("rep-estado")?.value;
  const seccion = document.getElementById("rep-seccion")?.value;

  if (desde) params.set("fechaDesde", desde);
  if (hasta) params.set("fechaHasta", hasta);
  if (estado && estado !== "Todos") params.set("estado", estado);
  if (seccion && seccion !== "Todas") params.set("seccion", seccion);

  cerrarModalReporte();
  window.location.href = "/reportes/auditoria?" + params.toString();
}

function toggleOrdersFilters() {
  const panel = document.getElementById("orders-multifilter-panel");
  const btn = document.getElementById("orders-filter-toggle");
  if (!panel) return;
  const open = !panel.classList.contains("open");
  panel.classList.toggle("open", open);
  if (btn) btn.setAttribute("aria-expanded", String(open));
}

// Inicializa los íconos, carga inicial de pedidos y eventos de filtros/teclado
document.addEventListener("DOMContentLoaded", () => {
  if (window.lucide) lucide.createIcons();
  fetchPedidos();

  let searchTimer;
  document.getElementById("orders-search-input")?.addEventListener("input", () => {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(fetchPedidos, 260);
  });

  ["orders-filter-estado", "orders-filter-negocio", "orders-filter-desde", "orders-filter-hasta"]
    .forEach((id) => {
      document.getElementById(id)?.addEventListener("change", fetchPedidos);
    });

  document.querySelectorAll(".metric-card[data-period]").forEach((card) => {
    card.addEventListener("click", () => showOrdersForPeriod(card.dataset.period));
  });

  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") {
      closeOrderDetailModal();
      cerrarModalReporte();
    }
  });
});
