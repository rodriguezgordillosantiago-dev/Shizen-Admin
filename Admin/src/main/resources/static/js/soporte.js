let ticketActualId = null;
let canalActual = null;

// Inicialización de la pantalla
document.addEventListener("DOMContentLoaded", () => {
  if (window.lucide) {
    lucide.createIcons();
  }
  // Conectar el socket al entrar a la pantalla
  iniciarWebSocket(() => {
    console.log("Servicio de chat en vivo listo para usar.");
  });
});

// Modales
function openNewTicketModal() {
  document.getElementById("new-ticket-modal").classList.add("open");
}

function closeNewTicketModal() {
  document.getElementById("new-ticket-modal").classList.remove("open");
}

// Al hacer clic en un ticket de la lista
// Al hacer clic en un ticket de la lista
function seleccionarTicket(item) {
  document
    .querySelectorAll(".support-ticket-item")
    .forEach((el) => el.classList.remove("active"));
  item.classList.add("active");

  ticketActualId = item.dataset.id;
  const idPedido = item.dataset.pedido;
  const estado = (item.dataset.estado || "").toLowerCase();

  // 1. Actualizar títulos
  document.getElementById("support-chat-title").textContent =
    item.dataset.asunto;
  document.getElementById("support-chat-subtitle").textContent =
    "#TCK-" +
    item.dataset.id +
    " · Estado: " +
    item.dataset.estado +
    " · " +
    item.dataset.fecha;

  // 2. ¿Tiene pedido asociado para rastrear?
  const orderLink = document.getElementById("support-order-link");
  const orderText = document.getElementById("support-order-text");
  if (idPedido && idPedido !== "null" && idPedido !== "") {
    orderLink.style.display = "inline-flex";
    orderLink.href = "/pedidos?id=" + idPedido; // Redirige a la pantalla de pedidos
    orderText.textContent = "Rastrear pedido #" + idPedido;
  } else {
    orderLink.style.display = "none";
  }

  // 3. ¿El ticket ya está cerrado o sigue abierto?
  const btnCerrar = document.getElementById("btn-cerrar-ticket");
  const badgeCerrado = document.getElementById("badge-ticket-cerrado");
  const formCerrar = document.getElementById("form-cerrar-ticket");
  const chatInput = document.getElementById("support-chat-input");
  const chatSendBtn = document.querySelector(".support-chat-send-btn");

  if (estado === "cerrado") {
    btnCerrar.style.display = "none";
    badgeCerrado.style.display = "inline-block";
    // Deshabilitar chat porque ya se resolvió
    chatInput.disabled = true;
    chatInput.placeholder = "Este ticket ha sido resuelto y cerrado.";
    if (chatSendBtn) chatSendBtn.disabled = true;
  } else {
    btnCerrar.style.display = "inline-flex";
    badgeCerrado.style.display = "none";
    formCerrar.action = "/soporte/" + ticketActualId + "/cerrar";
    // Habilitar chat
    chatInput.disabled = false;
    chatInput.placeholder = "Escribe tu respuesta para el usuario...";
    if (chatSendBtn) chatSendBtn.disabled = false;
  }

  // Renderizar iconos de Lucide nuevos
  if (window.lucide) {
    lucide.createIcons();
  }

  // 4. Cargar historial anterior por HTTP
  cargarHistorial(ticketActualId, item.dataset.descripcion);

  // 5. Conectarse a la frecuencia de este ticket vía WebSocket
  if (canalActual) {
    desuscribirseDeCanal(canalActual);
  }
  canalActual = "/topic/ticket/" + ticketActualId;
  suscribirseACanal(canalActual, (nuevoMensaje) => {
    dibujarBurbujaMensaje(nuevoMensaje);
  });
}

// Carga mensajes históricos desde la base de datos
function cargarHistorial(ticketId, descripcionInicial) {
  const msgContainer = document.getElementById("support-chat-messages");

  let html = `
    <div style="background: #F4F7F2; padding: 14px; border-radius: 12px; margin-bottom: 16px; border: 1px solid #E7EBE3;">
      <p style="font-size: 11px; font-weight: 600; color: #556153; margin-bottom: 4px;">Solicitud inicial del cliente:</p>
      <p style="font-size: 13px; color: #1B2E1D; line-height: 1.4;">${descripcionInicial || ""}</p>
    </div>
  `;

  fetch(`/soporte/${ticketId}/mensajes`)
    .then((res) => res.json())
    .then((mensajes) => {
      msgContainer.innerHTML = html;
      mensajes.forEach((m) => dibujarBurbujaMensaje(m));
      msgContainer.scrollTop = msgContainer.scrollHeight;
    })
    .catch((err) => {
      console.error("Error cargando historial:", err);
      msgContainer.innerHTML = html;
    });
}

// Pinta una burbuja en el contenedor de mensajes
function dibujarBurbujaMensaje(m) {
  const msgContainer = document.getElementById("support-chat-messages");
  const esAgente = ADMIN_ID != null && m.idRemitente === ADMIN_ID;
  const clase = esAgente ? "agent" : "customer";
  const fecha = m.fechaEnvio
    ? new Date(m.fechaEnvio).toLocaleTimeString([], {
        hour: "2-digit",
        minute: "2-digit",
      })
    : "";

  const bubble = document.createElement("div");
  bubble.className = `chat-bubble ${clase}`;
  bubble.innerHTML = `
    <div>${m.contenido}</div>
    <div class="chat-bubble-time">${fecha}</div>
  `;

  msgContainer.appendChild(bubble);
  msgContainer.scrollTop = msgContainer.scrollHeight;
}

// Enviar mensaje al pulsar Enviar o Enter
function sendSupportChatMessage(event) {
  event.preventDefault();

  if (!ticketActualId) {
    alert("Por favor selecciona un ticket primero");
    return;
  }

  const input = document.getElementById("support-chat-input");
  const texto = input.value.trim();
  if (!texto) return;

  // Enviamos al canal usando el servicio de WebSocket
  const enviado = enviarPorWebSocket("/app/chat/" + ticketActualId, {
    contenido: texto,
  });

  if (enviado) {
    input.value = ""; // Limpiamos la caja de texto
  } else {
    alert("Conectando al servidor... Intenta nuevamente en un momento.");
  }
}
