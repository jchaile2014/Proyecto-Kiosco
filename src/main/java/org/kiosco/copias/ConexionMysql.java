package org.kiosco.copias;

import org.kiosco.comun.DatoInvalidoException;

import java.net.URI;

/** Host, puerto y base de una URL JDBC de MySQL, para pasárselos a mysqldump. */
record ConexionMysql(String host, int puerto, String base) {

    static ConexionMysql desde(String urlJdbc) {
        if (urlJdbc == null || !urlJdbc.startsWith("jdbc:mysql://")) {
            throw new DatoInvalidoException("Las copias de seguridad necesitan una base MySQL.");
        }
        URI uri = URI.create(urlJdbc.substring("jdbc:".length()));
        String base = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");
        if (uri.getHost() == null || base.isBlank()) {
            throw new DatoInvalidoException("No entiendo la dirección de la base: " + urlJdbc);
        }
        return new ConexionMysql(uri.getHost(), uri.getPort() == -1 ? 3306 : uri.getPort(), base);
    }
}
