package org.kiosco.persistencia;

import jakarta.persistence.EntityManager;
import org.kiosco.logica.Fiado;
import java.util.List;

public class FiadoDAO {

    public void registrar(Fiado f) {
        EntityManager em = Conexion.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(f);
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public List<Fiado> listarPorCliente(int codigoCliente) {
        EntityManager em = Conexion.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT f FROM Fiado f WHERE f.cliente.codigo = :codigo",
                            Fiado.class)
                    .setParameter("codigo", codigoCliente)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public double calcularTotalPorCliente(int codigoCliente) {
        EntityManager em = Conexion.getEntityManager();
        try {
            Double total = em.createQuery(
                            "SELECT SUM(f.monto) FROM Fiado f WHERE f.cliente.codigo = :codigo",
                            Double.class)
                    .setParameter("codigo", codigoCliente)
                    .getSingleResult();
            return total != null ? total : 0.0;
        } finally {
            em.close();
        }
    }
}
