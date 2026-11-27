let currentActionType = "estado";

function setUserActionType(type) {
  currentActionType = type;
  const btnEstado = document.getElementById("action-btn-estado");
  const btnTemp = document.getElementById("action-btn-temporal");
  const btnPerm = document.getElementById("action-btn-permanente");
  const tempFields = document.getElementById("ban-temp-fields");

  // Quitamos la clase activa de todos
  btnEstado.classList.remove("active-estado");
  btnTemp.classList.remove("active-temporal");
  btnPerm.classList.remove("active-permanente");

  // Activamos solo el seleccionado
  if (type === "estado") {
    btnEstado.classList.add("active-estado");
    tempFields.classList.add("hidden");
  } else if (type === "temporal") {
    btnTemp.classList.add("active-temporal");
    tempFields.classList.remove("hidden");
  } else if (type === "permanente") {
    btnPerm.classList.add("active-permanente");
    tempFields.classList.add("hidden");
  }
}

function openUserDetailModal(btn) {
  const nombre = (btn.dataset.nombre || "").trim() || "Sin nombre";
  const partes = nombre.split(/\s+/);
  const iniciales = ((partes[0]?.[0] || "") + (partes[1]?.[0] || "")).toUpperCase() || "US";
  const baneado = btn.dataset.baneado === "true";

  document.getElementById("detail-user-initials").textContent = iniciales;
  document.getElementById("detail-user-eyebrow").textContent = "USR-" + (btn.dataset.id || "");
  document.getElementById("detail-user-name").textContent = nombre;
  document.getElementById("detail-user-email").textContent = btn.dataset.email || "—";
  document.getElementById("detail-user-rol").textContent = btn.dataset.rol || "—";
  document.getElementById("detail-user-status").textContent = baneado ? "Baneado" : "Activo";
  document.getElementById("detail-user-fecha").textContent = btn.dataset.fecharegistro || "Sin registro";
  document.getElementById("detail-user-direccion").textContent = btn.dataset.direccion || "No registrada";
  document.getElementById("detail-user-localidad").textContent = btn.dataset.localidad || "No registrada";

  const photoWrap = document.getElementById("detail-user-photo-wrap");
  const photoImg = document.getElementById("detail-user-photo");
  const fotoUrl = (btn.dataset.foto || "").trim();
  if (photoWrap && photoImg) {
    if (fotoUrl) {
      photoImg.src = fotoUrl;
      photoWrap.classList.remove("hidden");
      photoWrap.style.display = "flex";
    } else {
      photoImg.removeAttribute("src");
      photoWrap.classList.add("hidden");
      photoWrap.style.display = "none";
    }
  }

  const banBox = document.getElementById("detail-user-ban-box");
  if (banBox) {
    if (baneado) {
      document.getElementById("detail-user-ban-fecha").textContent =
        "Cuenta restringida" + (btn.dataset.fechabaneo ? " el " + btn.dataset.fechabaneo : "");
      document.getElementById("detail-user-ban-motivo").textContent =
        btn.dataset.motivobaneo || "Sin motivo registrado";
      banBox.classList.remove("hidden");
    } else {
      banBox.classList.add("hidden");
    }
  }

  document.getElementById("view-user-modal")?.classList.add("open");
}

function closeUserDetailModal() {
  document.getElementById("view-user-modal")?.classList.remove("open");
}

let currentUserId = null;
let currentUserBaneado = false;

function openManageUserModal(btn) {
  currentUserId = btn.dataset.id;
  currentUserBaneado = btn.dataset.baneado === "true";

  const nombre = (btn.dataset.nombre || "").trim() || "Usuario";
  const partes = nombre.split(/\s+/);
  const iniciales = ((partes[0]?.[0] || "") + (partes[1]?.[0] || "")).toUpperCase() || "US";
  const initialsEl = document.getElementById("modal-user-initials");
  if (initialsEl) initialsEl.textContent = iniciales;

  document.getElementById("modal-user-name").textContent = nombre;
  document.getElementById("modal-user-type").textContent =
    (btn.dataset.rol || "Usuario") + " · USR-" + btn.dataset.id;
  document.getElementById("modal-user-email").textContent = btn.dataset.email;
  document.getElementById("modal-user-status").textContent = currentUserBaneado
    ? "Baneado"
    : "Activo";

  const banActionsWrap = document.getElementById("ban-actions-wrap");
  const unbanPanel = document.getElementById("unban-visual-panel");
  const tempFields = document.getElementById("ban-temp-fields");
  const reasonLabel = document.getElementById("ban-reason-label");
  const reasonInput = document.getElementById("ban-reason-input");
  const confirmBtn = document.getElementById("confirm-action-btn");

  if (reasonInput) {
    reasonInput.value = "";
    reasonInput.style.borderColor = "";
  }

  if (currentUserBaneado) {
    if (banActionsWrap) banActionsWrap.classList.add("hidden");
    if (tempFields) tempFields.classList.add("hidden");
    if (unbanPanel) unbanPanel.classList.remove("hidden");

    const fechaEl = document.getElementById("unban-fecha");
    const motivoEl = document.getElementById("unban-motivo-original");
    if (fechaEl) {
      fechaEl.textContent = btn.dataset.fechabaneo
        ? "Baneado el " + btn.dataset.fechabaneo
        : "Cuenta restringida";
    }
    if (motivoEl) {
      motivoEl.textContent = btn.dataset.motivobaneo || "Sin motivo registrado.";
    }
    if (reasonLabel) reasonLabel.textContent = "Nota de reactivación (opcional)";
    if (reasonInput) {
      reasonInput.placeholder = "Ej. Se verificó el caso y se levanta la restricción...";
    }
    if (confirmBtn) {
      confirmBtn.className = "btn-modal-confirm";
      confirmBtn.textContent = "Reactivar cuenta ahora";
      confirmBtn.disabled = false;
    }
  } else {
    if (banActionsWrap) banActionsWrap.classList.remove("hidden");
    if (unbanPanel) unbanPanel.classList.add("hidden");
    setUserActionType("estado");
    if (reasonLabel) reasonLabel.textContent = "Razón de la acción";
    if (reasonInput) {
      reasonInput.placeholder = "Explica por qué se realiza esta acción en la cuenta...";
    }
    if (confirmBtn) {
      confirmBtn.className = "btn-modal-confirm danger";
      confirmBtn.textContent = "Confirmar baneo";
    }
  }

  validarFormularioBaneo();
  if (window.lucide) lucide.createIcons();
  document.getElementById("manage-user-modal").classList.add("open");
}

function closeManageUserModal() {
  document.getElementById("manage-user-modal").classList.remove("open");
}

// Envia la accion de banear o desbanear por AJAX (fetch) sin recargar la pagina
function handleUserActionConfirm() {
  if (!currentUserId) return;
  const reasonInput = document.getElementById("ban-reason-input");
  let motivoText = reasonInput ? reasonInput.value.trim() : "";

  if (!currentUserBaneado) {
    if (currentActionType === "temporal") {
      const untilDate = document.getElementById("ban-until-date")?.value;
      motivoText =
        "[Baneo temporal hasta:" +
        (untilDate || "no especificado") +
        "] " +
        motivoText;
    } else if (currentActionType === "permanente") {
      motivoText = "[Permanente] " + motivoText;
    }
  }

  const actionUrl = currentUserBaneado
    ? "/usuarios/" + currentUserId + "/desbanear"
    : "/usuarios/" + currentUserId + "/banear";

  const formData = new FormData();
  formData.append(currentUserBaneado ? "nota" : "motivo", motivoText);

  fetch(actionUrl, {
    method: "POST",
    body: formData,
  })
    .then(async (res) => {
      const data = await res.json().catch(() => ({}));
      if (!res.ok) {
        throw new Error(data.message || "Error en la solicitud");
      }
      return data;
    })
    .then((data) => {
      closeManageUserModal();

      if (data.accion === "baneado") {
        Swal.fire({
          icon: "warning",
          title: "Usuario baneado",
          text:
            "La cuenta de " +
            data.nombre +
            " ha sido restringida correctamente.",
          confirmButtonColor: "#2c5b27",
        }).then(() => {
          location.reload();
        });
      } else {
        Swal.fire({
          icon: "success",
          title: "Cuenta reactivada",
          text: "La cuenta de " + data.nombre + " vuelve a estar activa.",
          confirmButtonColor: "#2c5b27",
        }).then(() => {
          location.reload();
        });
      }
    })
    .catch((err) => {
      closeManageUserModal();
      Swal.fire({
        icon: "error",
        title: "No permitido",
        text: err.message || "Ocurrió un problema al procesar la solicitud.",
        confirmButtonColor: "#2c5b27",
      });
    });
}

// Validación en tiempo real
function validarFormularioBaneo() {
  const reasonInput = document.getElementById("ban-reason-input");
  const charCount = document.getElementById("ban-char-count");
  const untilDate = document.getElementById("ban-until-date");
  const confirmBtn = document.getElementById("confirm-action-btn");
  const alertBanner = document.getElementById("ban-alert-banner");
  const alertText = document.getElementById("ban-alert-text");

  if (!reasonInput || !confirmBtn) return;

  const texto = reasonInput.value.trim();
  const largo = reasonInput.value.length;

  // Actualizar contador 0/300
  if (charCount) {
    charCount.textContent = largo + "/300";
    charCount.style.color = largo >= 280 ? "#dc2626" : "#9aa29a";
  }

  // Para desbanear siempre permite confirmar
  if (currentUserBaneado) {
    confirmBtn.disabled = false;
    if (alertBanner) alertBanner.classList.add("hidden");
    return;
  }

  let error = "";
  if (texto.length < 10) {
    error =
      "Tienes que escribir un motivo de al menos 10 caracteres (" +
      texto.length +
      "/10).";
  } else if (
    currentActionType === "temporal" &&
    (!untilDate || !untilDate.value)
  ) {
    error = "Selecciona la fecha en la que el baneo temporal terminará";
  }

  if (error) {
    confirmBtn.disabled = true;
    reasonInput.style.borderColor = texto.length > 0 ? "#dc2626" : "";

    if (alertBanner && alertText) {
      alertText.textContent = error;
      alertBanner.classList.remove("hidden");
    }
  } else {
    confirmBtn.disabled = false;
    reasonInput.style.borderColor = "#2c5b27";

    if (alertBanner) alertBanner.classList.add("hidden");
  }
}

function toggleUsersFilters() {
  const panel = document.getElementById("users-multifilter-panel");
  const btn = document.getElementById("users-filter-toggle");
  if (!panel) return;
  const open = !panel.classList.contains("open");
  panel.classList.toggle("open", open);
  if (btn) btn.setAttribute("aria-expanded", String(open));
}

function renderUsersFilterChips() {
  const chipsEl = document.getElementById("users-filter-chips");
  const dotEl = document.getElementById("users-filter-dot");
  const form = document.getElementById("users-multifilter-form");
  const baneadoEl = document.getElementById("users-filter-baneado");
  const desdeEl = document.getElementById("users-filter-desde");
  const hastaEl = document.getElementById("users-filter-hasta");
  if (!chipsEl || !form) return;

  const fmtDate = (s) =>
    new Date(s + "T12:00:00")
      .toLocaleDateString("es-CO", { day: "numeric", month: "short", year: "numeric" })
      .replace(/\./g, "");

  const activos = [];
  if (baneadoEl && baneadoEl.value && baneadoEl.value !== "todos") {
    const label = baneadoEl.value === "activo" ? "Activa" : "Baneada";
    activos.push({ key: "baneado", label: "Estado: " + label });
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
      if (k === "baneado" && baneadoEl) baneadoEl.value = "todos";
      if (k === "desde" && desdeEl) desdeEl.value = "";
      if (k === "hasta" && hastaEl) hastaEl.value = "";
      form.submit();
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

document.addEventListener("DOMContentLoaded", () => {
  const reasonInput = document.getElementById("ban-reason-input");
  const untilDate = document.getElementById("ban-until-date");
  const desdeEl = document.getElementById("users-filter-desde");
  const hastaEl = document.getElementById("users-filter-hasta");

  if (reasonInput)
    reasonInput.addEventListener("input", validarFormularioBaneo);
  if (untilDate) untilDate.addEventListener("change", validarFormularioBaneo);
  if (desdeEl && hastaEl) {
    desdeEl.addEventListener("change", () => {
      hastaEl.min = desdeEl.value || "";
      if (hastaEl.value && desdeEl.value && hastaEl.value < desdeEl.value) {
        hastaEl.value = desdeEl.value;
      }
    });
  }
  renderUsersFilterChips();

  // Tooltip flotante: usa la posición horizontal de cada columna (offsetLeft) y el borde
  // superior de su barra rellena (offsetTop) para mostrar el total mensual al pasar el cursor
  const usersChart = document.getElementById("users-activity-chart");
  const usersTooltip = document.getElementById("users-chart-tooltip");
  if (usersChart && usersTooltip) {
    usersChart.querySelectorAll(".activity-bar-col[data-mes]").forEach((col) => {
      col.addEventListener("mouseenter", () => {
        const n = Number(col.dataset.cantidad || 0);
        usersTooltip.innerHTML = `<div style="color:#4b5563;font-weight:500">${col.dataset.mes}</div><div style="color:#111827"><strong>${n} ${n === 1 ? "registro" : "registros"}</strong></div>`;
        usersTooltip.style.left = `${col.offsetLeft + col.offsetWidth / 2}px`;
        const fill = col.querySelector(".activity-bar-fill");
        const barTop = fill ? fill.offsetTop : 0;
        usersTooltip.style.top = `${Math.max(4, barTop - 8)}px`;
        usersTooltip.style.display = "block";
      });
      col.addEventListener("mouseleave", () => {
        usersTooltip.style.display = "none";
      });
    });
  }
});
