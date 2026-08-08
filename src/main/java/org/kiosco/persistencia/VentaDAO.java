package org.kiosco.persistencia;

import jakarta.persistence.EntityManager;
import org.kiosco.logica.Venta;
import java.util.List;

public class VentaDAO {

    public void guardar(Venta v) {
        EntityManager em = Conexion.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(v);
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public List<Venta> listarTodas() {
        EntityManager em = Conexion.getEntityManager();
        try {
            return em.createQuery("SELECT v FROM Venta v", Venta.class).getResultList();
        } finally {
            em.close();
        }
    }
}
