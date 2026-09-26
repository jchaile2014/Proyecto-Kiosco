package org.kiosco.caja;

/**
 * Se publica justo antes de borrar un movimiento de la caja, dentro de la misma transacción,
 * para que otros módulos deshagan lo que dependía de él (por ejemplo, devolver el stock de
 * una venta) sin que la caja tenga que conocerlos.
 */
public record MovimientoCajaEliminado(Long movimientoId) {
}
