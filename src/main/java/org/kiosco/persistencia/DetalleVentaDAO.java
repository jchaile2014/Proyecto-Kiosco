package org.kiosco.persistencia;

import jakarta.persistence.EntityManager;
import org.kiosco.logica.DetalleVenta;
import java.util.List;

public class DetalleVentaDAO {

    public void guardar(DetalleVenta d) {
        EntityManager em = Conexion.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(d);
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public List<DetalleVenta> listarPorVenta(int codigoVenta) {
        EntityManager em = Conexion.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT d FROM DetalleVenta d WHERE d.venta.codigo = :codigo",
                            DetalleVenta.class)
                    .setParameter("codigo", codigoVenta)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}
