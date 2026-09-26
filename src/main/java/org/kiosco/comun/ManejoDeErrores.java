package org.kiosco.comun;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Si una acción no se puede hacer (por ejemplo, cancelar dos veces el mismo pedido porque
 * la página quedó vieja), se vuelve a la pantalla actual con el motivo, en vez de mostrar
 * una página de error.
 */
@ControllerAdvice
class ManejoDeErrores {

    @ExceptionHandler(DatoInvalidoException.class)
    ResponseEntity<Void> datoInvalido(DatoInvalidoException e, HttpServletRequest request, HttpServletResponse response) {
        boolean htmx = request.getHeader("HX-Request") != null;
        String origen = htmx ? request.getHeader("HX-Current-URL") : request.getHeader("Referer");
        String destino = volverA(origen);

        RequestContextUtils.getOutputFlashMap(request).put("errorGeneral", e.getMessage());
        RequestContextUtils.saveOutputFlashMap(destino, request, response);

        return htmx
                ? ResponseEntity.noContent().header("HX-Redirect", destino).build()
                : ResponseEntity.status(303).header("Location", destino).build();
    }

    /** Solo la ruta y la consulta de la página de origen: nunca redirige a otro sitio. */
    private static String volverA(String origen) {
        if (origen == null) {
            return "/";
        }
        var uri = UriComponentsBuilder.fromUriString(origen).build();
        // "//otro-sitio" sería una dirección a otro dominio: se deja una sola barra
        String ruta = "/" + (uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/+", ""));
        return uri.getQuery() == null ? ruta : ruta + "?" + uri.getQuery();
    }
}
