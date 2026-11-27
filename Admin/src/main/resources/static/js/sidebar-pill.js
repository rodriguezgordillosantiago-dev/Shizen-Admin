/* ======================================================== */
/* SHIZEN ADMIN - CONTROLADOR DE LA BARRA LATERAL PÍLDORA                   */
/* ======================================================== */
document.addEventListener("DOMContentLoaded", () => {
  const side = document.getElementById("pill-sidebar");
  const toggle = document.getElementById("pill-side-toggle");
  const searchInput = document.getElementById("pill-nav-search");
  const navList = document.getElementById("pill-nav-list");

  if (!side || !toggle) return;

  // Restaurar estado guardado (expandido o colapsado)
  const isCollapsed = localStorage.getItem("shizen_admin_side_collapsed") === "true";
  if (isCollapsed) {
    side.classList.add("collapsed");
    toggle.setAttribute("aria-expanded", "false");
  }

  // Alternar al hacer clic en el botón
  toggle.addEventListener("click", () => {
    const collapsed = side.classList.toggle("collapsed");
    toggle.setAttribute("aria-expanded", String(!collapsed));
    toggle.setAttribute("aria-label", collapsed ? "Expandir menú" : "Contraer menú");
    localStorage.setItem("shizen_admin_side_collapsed", String(collapsed));
  });

  // Filtro en tiempo real al escribir en el buscador
  if (searchInput && navList) {
    searchInput.addEventListener("input", () => {
      const q = searchInput.value.toLowerCase().trim();
      const items = navList.querySelectorAll("li");
      items.forEach((li) => {
        const text = li.textContent.toLowerCase();
        li.style.display = text.includes(q) ? "" : "none";
      });
    });
  }
});
