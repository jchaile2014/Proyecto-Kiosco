package org.kiosco.persistencia;

import jakarta.persistence.EntityManager;
import org.kiosco.logica.Repartidor;
import java.util.List;

public class RepartidorDAO {

    public void guardar(Repartidor r) {
        EntityManager em = Conexion.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(r);
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public List<Repartidor> listarTodos() {
        EntityManager em = Conexion.getEntityManager();
        try {
            return em.createQuery("SELECT r FROM Repartidor r WHERE r.activo = true", Repartidor.class).getResultList();
        } finally {
            em.close();
        }
    }

    public Repartidor buscarPorCodigo(int codigo) {
        EntityManager em = Conexion.getEntityManager();
        try {
            return em.find(Repartidor.class, codigo);
        } finally {
            em.close();
        }
    }

    public void actualizar(Repartidor r) {
        EntityManager em = Conexion.getEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(r);
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public void eliminar(int codigo) {
        EntityManager em = Conexion.getEntityManager();
        try {
            Repartidor r = em.find(Repartidor.class, codigo);
            if (r != null) {
                em.getTransaction().begin();
                r.setActivo(false);
                em.merge(r);
                em.getTransaction().commit();
            }
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}
