package org.kiosco.persistencia;

import jakarta.persistence.EntityManager;
import org.kiosco.logica.Producto;
import java.util.List;

public class ProductoDAO {

    public void guardar(Producto p) {
        EntityManager em = Conexion.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(p);
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public List<Producto> listarTodos() {
        EntityManager em = Conexion.getEntityManager();
        try {
            return em.createQuery("SELECT p FROM Producto p WHERE p.activo = true", Producto.class).getResultList();
        } finally {
            em.close();
        }
    }

    public Producto buscarPorCodigo(int codigo) {
        EntityManager em = Conexion.getEntityManager();
        try {
            return em.find(Producto.class, codigo);
        } finally {
            em.close();
        }
    }

    public void actualizar(Producto p) {
        EntityManager em = Conexion.getEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(p);
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
            Producto p = em.find(Producto.class, codigo);
            if (p != null) {
                em.getTransaction().begin();
                p.setActivo(false);
                em.merge(p);
                em.getTransaction().commit();
            }
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public void actualizarStock(int codigoProducto, int cantidad) {
        EntityManager em = Conexion.getEntityManager();
        try {
            Producto p = em.find(Producto.class, codigoProducto);
            if (p != null) {
                em.getTransaction().begin();
                p.setStock(p.getStock() + cantidad);
                em.merge(p);
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
