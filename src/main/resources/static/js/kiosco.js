// Cada pedido de HTMX lleva el token CSRF que la página recibió del servidor.
document.addEventListener('htmx:configRequest', (evento) => {
    const header = document.querySelector('meta[name="csrf-header"]');
    const token = document.querySelector('meta[name="csrf-token"]');
    if (header && token) {
        evento.detail.headers[header.content] = token.content;
    }
});

// Si algo no se pudo guardar, avisarlo en vez de no hacer nada.
function mostrarErrorDeConexion() {
    const aviso = document.getElementById('error-conexion');
    aviso.classList.add('show');
    setTimeout(() => aviso.classList.remove('show'), 6000);
}
document.addEventListener('htmx:responseError', mostrarErrorDeConexion);
document.addEventListener('htmx:sendError', mostrarErrorDeConexion);

// Selector de fecha del encabezado: salta al día elegido.
document.addEventListener('change', (evento) => {
    const campo = evento.target.closest('[data-ir-a-fecha]');
    if (campo && campo.value) {
        window.location = '/?fecha=' + campo.value;
    }
});
