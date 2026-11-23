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
  document.getElementById("modal-user-status").textContent =
    currentUserBaneado ? "Baneado" : "Activo";

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

function handleUserActionConfirm() {
  if (!currentUserId) return;
  const form = document.getElementById("action-user-form");
  const reasonInput = document.getElementById("ban-reason-input");
  const motivoInput = document.getElementById("form-motivo-input");

  let motivoText = reasonInput ? reasonInput.value.trim() : "";

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
  motivoInput.value = motivoText;

  const actionUrl = currentUserBaneado
    ? "/usuarios/" + currentUserId + "/desbanear"
    : "/usuarios/" + currentUserId + "/banear";

  form.action = actionUrl;
  form.submit();
}
