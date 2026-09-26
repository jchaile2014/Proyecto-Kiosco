package org.kiosco.copias;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CopiaDeSeguridadServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final Clock DIA_26 = Clock.fixed(LocalDate.of(2026, 9, 26).atTime(23, 55).atZone(ZONA).toInstant(), ZONA);

    @TempDir
    Path carpeta;

    @TempDir
    Path herramientas;

    @Test
    void guardaElVolcadoComprimidoConLaFechaYNoDejaLaContraseniaTirada() throws IOException {
        CopiaDeSeguridadService copias = servicio("mysqldump-falso.sh", 30);

        CopiaDeSeguridadService.Copia copia = copias.hacerCopia();

        assertThat(copia.archivo().getFileName()).hasToString("kiosco-2026-09-26.sql.gz");
        assertThat(descomprimir(copia.archivo())).isEqualTo("-- volcado de prueba de kiosco\n");
        try (Stream<Path> archivos = Files.list(carpeta)) {
            assertThat(archivos.map(p -> p.getFileName().toString()))
                    .as("ni credenciales ni volcados a medio hacer").containsExactly("kiosco-2026-09-26.sql.gz");
        }
        assertThat(Files.getPosixFilePermissions(copia.archivo()))
                .as("tiene los datos del negocio").isEqualTo(PosixFilePermissions.fromString("rw-------"));
        assertThat(copias.hayCopiaDeHoy()).isTrue();
        assertThat(copias.ultimoResultado().ok()).isTrue();
    }

    @Test
    void guardaSoloLasUltimasCopias() throws IOException {
        for (String dia : new String[]{"2026-09-20", "2026-09-24", "2026-09-25"}) {
            Files.writeString(carpeta.resolve("kiosco-" + dia + ".sql.gz"), "vieja");
        }
        CopiaDeSeguridadService copias = servicio("mysqldump-falso.sh", 2);

        copias.hacerCopia();

        assertThat(copias.copias()).extracting(c -> c.fecha().toString()).containsExactly("2026-09-26", "2026-09-25");
    }

    @Test
    void siMysqldumpFallaSeGuardaElMotivoYNoQuedaNingunArchivo() throws IOException {
        CopiaDeSeguridadService copias = servicio("mysqldump-que-falla.sh", 30);

        assertThatThrownBy(copias::hacerCopia)
                .isInstanceOf(CopiaFallidaException.class)
                .hasMessageContaining("Access denied");
        assertThat(copias.ultimoResultado().ok()).isFalse();
        try (Stream<Path> archivos = Files.list(carpeta)) {
            assertThat(archivos).isEmpty();
        }
    }

    @Test
    void siNoEstaMysqldumpInstaladoSeExplica() {
        CopiaDeSeguridadService copias = new CopiaDeSeguridadService("jdbc:mysql://localhost:3306/kiosco", "kiosco",
                "x", carpeta, "/no/existe/mysqldump", 30, DIA_26);

        assertThatThrownBy(copias::hacerCopia)
                .isInstanceOf(CopiaFallidaException.class)
                .hasMessageContaining("No se pudo hacer la copia");
    }

    @Test
    void enModoDemoNoHayCopias() {
        CopiaDeSeguridadService copias = new CopiaDeSeguridadService("jdbc:h2:mem:kiosco-demo", "sa", "",
                carpeta, "mysqldump", 30, DIA_26);

        assertThat(copias.disponible()).isFalse();
        assertThatThrownBy(copias::hacerCopia).hasMessageContaining("modo demo");
    }

    @Test
    void entiendeLaDireccionDeLaBase() {
        assertThat(ConexionMysql.desde("jdbc:mysql://localhost:3306/kiosco?serverTimezone=UTC"))
                .isEqualTo(new ConexionMysql("localhost", 3306, "kiosco"));
        assertThat(ConexionMysql.desde("jdbc:mysql://127.0.0.1/kiosco"))
                .isEqualTo(new ConexionMysql("127.0.0.1", 3306, "kiosco"));
    }

    private CopiaDeSeguridadService servicio(String script, int cantidad) throws IOException {
        Path ejecutable = herramientas.resolve(script);
        try (InputStream origen = getClass().getResourceAsStream("/copias/" + script)) {
            Files.copy(origen, ejecutable);
        }
        assertThat(ejecutable.toFile().setExecutable(true)).isTrue();
        // La contraseña tiene una comilla para probar que se escapa bien en el archivo de opciones
        return new CopiaDeSeguridadService("jdbc:mysql://localhost:3306/kiosco", "kiosco", "se\"creto",
                carpeta, ejecutable.toString(), cantidad, DIA_26);
    }

    private static String descomprimir(Path archivo) throws IOException {
        try (InputStream entrada = new GZIPInputStream(Files.newInputStream(archivo))) {
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
