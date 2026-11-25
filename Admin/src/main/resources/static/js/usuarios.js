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

let currentUserId = null;
let currentUserBaneado = false;

function openManageUserModal(btn) {
  currentUserId = btn.dataset.id;
  currentUserBaneado = btn.dataset.baneado === "true";

  document.getElementById("modal-user-name").textContent = btn.dataset.nombre;
  document.getElementById("modal-user-type").textContent =
    (btn.dataset.rol || "Usuario") + " · USR-" + btn.dataset.id;
  document.getElementById("modal-user-email").textContent = btn.dataset.email;
  document.getElementById("modal-user-status").textContent = currentUserBaneado
    ? "Baneado"
    : "Activo";

  const confirmBtn = document.getElementById("confirm-action-btn");
  confirmBtn.disabled = false;
  confirmBtn.textContent = currentUserBaneado
    ? "Desbanear cuenta"
    : "Confirmar baneo";

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

  // Si es temporal o permanente, armamos el texto del motivo
  if (currentActionType === "temporal") {
    const untilDate = document.getElementById("ban-until-date")?.value;
    motivoText =
      "[Baneo temporal hasta:" +
      (untilDate || "no especificado") +
      "] " +
      motivoText;
  } else if (currentActionType === "permanente") {
    motivoText = "[Permanente]" + motivoText;
  }

  // Ruta hacia el Controller de Spring Boot
  const actionUrl = currentUserBaneado
    ? "/usuarios/" + currentUserId + "/desbanear"
    : "/usuarios/" + currentUserId + "/banear";

  // Preparamos los datos del formulario en segundo plano
  const formData = new FormData();
  formData.append("motivo", motivoText);

  // Peticion fetch (igual a como se hace en shizenhome)
  fetch(actionUrl, {
    method: "POST",
    body: formData,
  })
    .then((res) => {
      if (!res.ok) throw new Error("Error en la solicitud");
      return res.json();
    })
    .then((data) => {
      // 1. Cerramos el modal de gestion
      closeManageUserModal();

      // 2. Disparamos SweetAlert2 directamente en JS sin esperar recarga
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
          // Recargamos solo al presionar OK para refrescar la tabla con el nuevo estado
          location.reload();
        });
      } else {
        Swal.fire({
          icon: "success",
          title: "Usuario reactivado",
          text: "La cuenta de " + data.nombre + " ha sido desbaneada.",
          confirmButtonColor: "#2c5b27",
        }).then(() => {
          location.reload();
        });
      }
    })
    .catch((err) => {
      console.error("Error al procesar accion:", err);
      Swal.fire({
        icon: "error",
        title: "Error",
        text: "Ocurrió un problema al procesar la solicitud.",
        confirmButtonColor: "#2c5b27",
      });
    });
} // <-- cierra handleUserActionConfirm

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

document.addEventListener("DOMContentLoaded", () => {
  const reasonInput = document.getElementById("ban-reason-input");
  const untilDate = document.getElementById("ban-until-date");

  if (reasonInput)
    reasonInput.addEventListener("input", validarFormularioBaneo);
  if (untilDate) untilDate.addEventListener("change", validarFormularioBaneo);
});
