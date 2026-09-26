package org.kiosco.copias;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

/**
 * Copias de seguridad de la base con mysqldump, comprimidas, una por día en
 * {@code ~/.kiosco/copias}. Se guardan las últimas 30.
 *
 * <p>La contraseña se le pasa a mysqldump en un archivo temporal que solo puede leer el
 * usuario y se borra al terminar: nunca aparece en la lista de procesos.
 */
@Service
public class CopiaDeSeguridadService {

    private static final Logger log = LoggerFactory.getLogger(CopiaDeSeguridadService.class);
    private static final Pattern NOMBRE = Pattern.compile("kiosco-(\\d{4}-\\d{2}-\\d{2})\\.sql\\.gz");
    private static final long MINUTOS_MAXIMOS = 5;
    /** Las copias tienen todos los datos del negocio: solo las puede leer el usuario. */
    private static final String SOLO_EL_USUARIO = "rw-------";

    private final String url;
    private final String usuario;
    private final String contrasenia;
    private final Path carpeta;
    private final String mysqldump;
    private final int cantidadAGuardar;
    private final Clock clock;

    private volatile Resultado ultimoResultado;

    public CopiaDeSeguridadService(@Value("${spring.datasource.url}") String url,
                                   @Value("${spring.datasource.username:}") String usuario,
                                   @Value("${spring.datasource.password:}") String contrasenia,
                                   @Value("${kiosco.copias.carpeta:${user.home}/.kiosco/copias}") Path carpeta,
                                   @Value("${kiosco.copias.mysqldump:mysqldump}") String mysqldump,
                                   @Value("${kiosco.copias.cantidad:30}") int cantidadAGuardar,
                                   Clock clock) {
        this.url = url;
        this.usuario = usuario;
        this.contrasenia = contrasenia;
        this.carpeta = carpeta;
        this.mysqldump = mysqldump;
        this.cantidadAGuardar = cantidadAGuardar;
        this.clock = clock;
    }

    public record Copia(Path archivo, LocalDate fecha, long bytes) {

        /** Para las pantallas: Thymeleaf no deja leer propiedades de un Path. */
        public String nombreArchivo() {
            return archivo.getFileName().toString();
        }
    }

    /** Cómo salió la última copia desde que arrancó la app (null si todavía no se hizo ninguna). */
    public record Resultado(LocalDateTime cuando, boolean ok, String mensaje) {
    }

    /** En modo demo (base en memoria) no hay nada que copiar. */
    public boolean disponible() {
        return url != null && url.startsWith("jdbc:mysql:");
    }

    public Path carpeta() {
        return carpeta;
    }

    public Resultado ultimoResultado() {
        return ultimoResultado;
    }

    public boolean hayCopiaDeHoy() {
        return Files.exists(archivoDelDia(LocalDate.now(clock)));
    }

    /** Las copias guardadas, la más nueva primero. */
    public List<Copia> copias() {
        if (!Files.isDirectory(carpeta)) {
            return List.of();
        }
        List<Copia> copias = new ArrayList<>();
        try (Stream<Path> archivos = Files.list(carpeta)) {
            for (Path archivo : archivos.toList()) {
                Matcher m = NOMBRE.matcher(archivo.getFileName().toString());
                if (m.matches()) {
                    copias.add(new Copia(archivo, LocalDate.parse(m.group(1)), Files.size(archivo)));
                }
            }
        } catch (IOException e) {
            log.warn("No se pudo leer la carpeta de copias {}", carpeta, e);
        }
        copias.sort(Comparator.comparing(Copia::fecha).reversed());
        return copias;
    }

    /** Hace la copia de hoy (si ya había una, la reemplaza con la más reciente). */
    public synchronized Copia hacerCopia() {
        if (!disponible()) {
            throw new CopiaFallidaException("En modo demo no se hacen copias: la base es de prueba y se borra al cerrar.");
        }
        try {
            Copia copia = volcarYComprimir(ConexionMysql.desde(url));
            ultimoResultado = new Resultado(LocalDateTime.now(clock), true, "Copia guardada en " + copia.archivo());
            log.info("Copia de seguridad guardada en {}", copia.archivo());
            return copia;
        } catch (CopiaFallidaException e) {
            ultimoResultado = new Resultado(LocalDateTime.now(clock), false, e.getMessage());
            log.error("Falló la copia de seguridad: {}", e.getMessage());
            throw e;
        }
    }

    private Copia volcarYComprimir(ConexionMysql conexion) {
        Path credenciales = null;
        Path volcado = carpeta.resolve(".kiosco-en-curso.sql");
        try {
            Files.createDirectories(carpeta, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
            credenciales = Files.createTempFile(carpeta, ".credenciales-", ".cnf",
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString(SOLO_EL_USUARIO)));
            Files.writeString(credenciales, "[client]\nuser=\"" + escapar(usuario) + "\"\npassword=\""
                    + escapar(contrasenia) + "\"\n", StandardCharsets.UTF_8);

            Process proceso = new ProcessBuilder(
                    mysqldump,
                    "--defaults-extra-file=" + credenciales,   // tiene que ser la primera opción
                    "--host=" + conexion.host(),
                    "--port=" + conexion.puerto(),
                    "--single-transaction",                   // copia consistente sin frenar la app
                    "--no-tablespaces",                       // no pide permisos de administrador
                    "--result-file=" + volcado,
                    conexion.base())
                    .redirectErrorStream(true)
                    .start();
            String salida = new String(proceso.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
            if (!proceso.waitFor(MINUTOS_MAXIMOS, TimeUnit.MINUTES)) {
                proceso.destroyForcibly();
                throw new CopiaFallidaException("La copia tardó demasiado y se canceló.");
            }
            if (proceso.exitValue() != 0) {
                throw new CopiaFallidaException(salida.isEmpty()
                        ? "mysqldump terminó con el error " + proceso.exitValue() + "." : salida);
            }

            Path destino = archivoDelDia(LocalDate.now(clock));
            comprimir(volcado, destino);
            borrarLasViejas();
            return new Copia(destino, LocalDate.now(clock), Files.size(destino));
        } catch (IOException e) {
            throw new CopiaFallidaException("No se pudo hacer la copia: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CopiaFallidaException("La copia se interrumpió.");
        } finally {
            borrarSinFallar(credenciales);
            borrarSinFallar(volcado);
        }
    }

    private void borrarLasViejas() {
        List<Copia> copias = copias();
        copias.stream().skip(cantidadAGuardar).forEach(c -> borrarSinFallar(c.archivo()));
    }

    private Path archivoDelDia(LocalDate dia) {
        return carpeta.resolve("kiosco-" + dia + ".sql.gz");
    }

    private static void comprimir(Path origen, Path destino) throws IOException {
        Path temporal = destino.resolveSibling(destino.getFileName() + ".tmp");
        try (InputStream entrada = Files.newInputStream(origen);
             OutputStream salida = new GZIPOutputStream(Files.newOutputStream(temporal))) {
            entrada.transferTo(salida);
        }
        Files.setPosixFilePermissions(temporal, PosixFilePermissions.fromString(SOLO_EL_USUARIO));
        // Se renombra al final: nunca queda una copia a medio escribir con el nombre bueno
        Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
    }

    /** Para valores entre comillas en el archivo de opciones de MySQL. */
    private static String escapar(String valor) {
        return valor == null ? "" : valor.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void borrarSinFallar(Path archivo) {
        if (archivo == null) {
            return;
        }
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException e) {
            log.warn("No se pudo borrar {}", archivo, e);
        }
    }
}
