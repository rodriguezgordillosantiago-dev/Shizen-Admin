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
