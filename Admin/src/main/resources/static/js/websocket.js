/**
 * Servicio global de WebSocket (SockJS + STOMP)
 */
let stompClient = null;
let suscripcionesActivas = {};

// Inicia la conexión con el servidor
function iniciarWebSocket(onConectadoCallback) {
  const socket = new SockJS("/ws");
  stompClient = Stomp.over(socket);
  stompClient.debug = null; // Silenciar logs excesivos

  stompClient.connect(
    {},
    function (frame) {
      console.log("WebSocket conectado exitosamente.");
      if (onConectadoCallback) {
        onConectadoCallback();
      }
    },
    function (error) {
      console.error("Error en WebSocket. Reintentando en 4s...", error);
      setTimeout(() => iniciarWebSocket(onConectadoCallback), 4000);
    },
  );
}

// Suscribirse al canal en vivo de un ticket (ej. /topic/ticket/1)
function suscribirseACanal(canal, onMensajeCallback) {
  if (!stompClient || !stompClient.connected) {
    console.warn("WebSocket aún no está conectado.");
    return null;
  }

  // Cancelar suscripción previa si ya existía
  if (suscripcionesActivas[canal]) {
    suscripcionesActivas[canal].unsubscribe();
  }

  const sub = stompClient.subscribe(canal, function (mensajePayload) {
    const datos = JSON.parse(mensajePayload.body);
    if (onMensajeCallback) {
      onMensajeCallback(datos);
    }
  });

  suscripcionesActivas[canal] = sub;
  return sub;
}

// Cancelar la escucha
function desuscribirseDeCanal(canal) {
  if (suscripcionesActivas[canal]) {
    suscripcionesActivas[canal].unsubscribe();
    delete suscripcionesActivas[canal];
  }
}

// Enviar un mensaje por WebSocket hacia Spring
function enviarPorWebSocket(destino, objetoDatos) {
  if (stompClient && stompClient.connected) {
    stompClient.send(destino, {}, JSON.stringify(objetoDatos));
    return true;
  } else {
    console.error("No se pudo enviar: WebSocket desconectado.");
    return false;
  }
}
