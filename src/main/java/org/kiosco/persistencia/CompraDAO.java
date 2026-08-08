package org.kiosco.persistencia;

import jakarta.persistence.EntityManager;
import org.kiosco.logica.Compra;
import java.util.List;

public class CompraDAO {

    public void guardar(Compra c) {
        EntityManager em = Conexion.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(c);
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public List<Compra> listarTodas() {
        EntityManager em = Conexion.getEntityManager();
        try {
            return em.createQuery("SELECT c FROM Compra c", Compra.class).getResultList();
        } finally {
            em.close();
        }
    }

    public List<String> listarProveedores() {
        EntityManager em = Conexion.getEntityManager();
        try {
            return em.createQuery("SELECT DISTINCT c.proveedor FROM Compra c", String.class).getResultList();
        } finally {
            em.close();
        }
    }
}
