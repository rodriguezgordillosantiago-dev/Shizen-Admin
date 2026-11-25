let showPass = false;

function toggleStandalonePassword() {
  showPass = !showPass;
  const input = document.getElementById("standalone-password");
  const icon = document.getElementById("standalone-eye-icon");
  if (input && icon) {
    input.type = showPass ? "text" : "password";
    icon.setAttribute("data-lucide", showPass ? "eye-off" : "eye");
    lucide.createIcons();
  }
}

function handleStandaloneLogin(e) {
  return true;
}

document.addEventListener("DOMContentLoaded", () => {
  if (window.lucide) {
    lucide.createIcons();
  }
});

/// Validaciones en tiempo real///

// Constante para validar formato de correo electrónico
const EMAIL_REGEX = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

function validarFormularioLogin() {
  const emailInput = document.getElementById("standalone-email");
  const passwordInput = document.getElementById("standalone-password");
  const errorBox = document.getElementById("standalone-error");
  const submitBtn = document.getElementById("standalone-btn");
  const serverError = document.getElementById("standalone-server-error");

  if (!emailInput || !passwordInput || !submitBtn) return;

  const emailVal = emailInput.value.trim();
  const passVal = passwordInput.value;

  // Si el usuario vuelve a escribir, el error viejo del servidor desaparece
  if (serverError) {
    serverError.classList.add("hidden");
  }

  let errorMensaje = "";
  let emailValido = false;
  let passValido = false;

  // 1. Validar correo
  if (emailVal.length === 0) {
    emailInput.style.borderColor = ""; // neutral si está vacío
  } else if (!EMAIL_REGEX.test(emailVal)) {
    emailInput.style.borderColor = "#dc2626"; // rojo si no es email válido
    errorMensaje =
      "Ingresa un correo electrónico válido (ej: admin@shizen.com).";
  } else {
    emailInput.style.borderColor = "#2c5b27"; // verde Shizen si es correcto
    emailValido = true;
  }

  // 2. Validar contraseña
  if (passVal.length === 0) {
    passwordInput.style.borderColor = ""; // neutral si está vacío
  } else if (passVal.length < 6) {
    passwordInput.style.borderColor = "#dc2626"; // rojo si tiene menos de 6 caracteres
    if (!errorMensaje) {
      errorMensaje = "La contraseña debe tener al menos 8 caracteres.";
    }
  } else {
    passwordInput.style.borderColor = "#2c5b27"; // verde Shizen si cumple longitud
    passValido = true;
  }

  // 3. Mostrar u ocultar caja de error y habilitar/deshabilitar botón
  if (errorMensaje) {
    errorBox.textContent = errorMensaje;
    errorBox.classList.remove("hidden");
  } else {
    errorBox.classList.add("hidden");
  }

  // El botón de ingreso solo se activa si los 2 campos son válidos
  submitBtn.disabled = !(emailValido && passValido);
}

// Escuchamos el evento 'input' para validar cada tecla que el usuario presione
document.addEventListener("DOMContentLoaded", () => {
  const emailInput = document.getElementById("standalone-email");
  const passwordInput = document.getElementById("standalone-password");

  if (emailInput) {
    emailInput.addEventListener("input", validarFormularioLogin);
  }
  if (passwordInput) {
    passwordInput.addEventListener("input", validarFormularioLogin);
  }
});
