package org.kiosco.persistencia;

import jakarta.persistence.EntityManager;
import org.kiosco.logica.DetalleCompra;
import java.util.List;

public class DetalleCompraDAO {

    public void guardar(DetalleCompra d) {
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

    public List<DetalleCompra> listarPorCompra(int codigoCompra) {
        EntityManager em = Conexion.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT d FROM DetalleCompra d WHERE d.compra.codigo = :codigo",
                            DetalleCompra.class)
                    .setParameter("codigo", codigoCompra)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}
