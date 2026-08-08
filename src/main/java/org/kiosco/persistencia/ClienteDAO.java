package org.kiosco.persistencia;

import jakarta.persistence.EntityManager;
import org.kiosco.logica.Cliente;
import java.util.List;

public class ClienteDAO {

    public void guardar(Cliente c) {
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

    public List<Cliente> listarTodos() {
        EntityManager em = Conexion.getEntityManager();
        try {
            return em.createQuery("SELECT c FROM Cliente c WHERE c.activo = true", Cliente.class).getResultList();
        } finally {
            em.close();
        }
    }

    public Cliente buscarPorCodigo(int codigo) {
        EntityManager em = Conexion.getEntityManager();
        try {
            return em.find(Cliente.class, codigo);
        } finally {
            em.close();
        }
    }

    public void actualizar(Cliente c) {
        EntityManager em = Conexion.getEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(c);
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
            Cliente c = em.find(Cliente.class, codigo);
            if (c != null) {
                em.getTransaction().begin();
                c.setActivo(false);
                em.merge(c);
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
