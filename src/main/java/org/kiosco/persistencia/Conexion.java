package org.kiosco.persistencia;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.Map;

public class Conexion {

    private static EntityManagerFactory factory;

    public static EntityManager getEntityManager() {
        if (factory == null) {
            factory = Persistence.createEntityManagerFactory("kioscoPU", buildEnvOverrides());
        }
        return factory.createEntityManager();
    }

    /**
     * Permite configurar la conexión real vía variables de entorno (DB_URL, DB_USER,
     * DB_PASSWORD) sin tocar persistence.xml, para no versionar credenciales.
     */
    private static Map<String, String> buildEnvOverrides() {
        Map<String, String> overrides = new HashMap<>();
        putIfPresent(overrides, "jakarta.persistence.jdbc.url", System.getenv("DB_URL"));
        putIfPresent(overrides, "jakarta.persistence.jdbc.user", System.getenv("DB_USER"));
        putIfPresent(overrides, "jakarta.persistence.jdbc.password", System.getenv("DB_PASSWORD"));
        return overrides;
    }

    private static void putIfPresent(Map<String, String> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }
}