package org.gradle.wrapper;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;

public final class GradleWrapperMain {
    private GradleWrapperMain() {}

    public static void main(String[] args) throws Exception {
        Path root = findeProjektWurzel();
        Properties eigenschaften = new Properties();
        Path datei = root.resolve("gradle/wrapper/gradle-wrapper.properties");
        try (Reader reader = Files.newBufferedReader(datei, StandardCharsets.UTF_8)) {
            eigenschaften.load(reader);
        }
        String url = erforderlich(eigenschaften, "distributionUrl");
        String sha = erforderlich(eigenschaften, "distributionSha256Sum").toLowerCase(Locale.ROOT);
        String name = url.substring(url.lastIndexOf('/') + 1);
        String ordnerName = name.replace("-bin.zip", "").replace("-all.zip", "");
        Path basis = Path.of(System.getProperty("user.home"), ".gradle", "wrapper", "dists", "mathematik-atlas", sha.substring(0, 16));
        Path installation = basis.resolve(ordnerName);
        Path gradle = installation.resolve("bin").resolve(istWindows() ? "gradle.bat" : "gradle");

        if (!Files.isRegularFile(gradle)) {
            Files.createDirectories(basis);
            Path zip = basis.resolve(name);
            if (!Files.isRegularFile(zip) || !sha256(zip).equals(sha)) {
                Files.deleteIfExists(zip);
                ladeHerunter(url, zip);
            }
            String tatsächlich = sha256(zip);
            if (!tatsächlich.equals(sha)) {
                Files.deleteIfExists(zip);
                throw new IOException("Gradle-Prüfsumme stimmt nicht. Erwartet " + sha + ", erhalten " + tatsächlich);
            }
            entpacke(zip, basis);
        }
        if (!Files.isRegularFile(gradle)) throw new IOException("Gradle-Startdatei nicht gefunden: " + gradle);
        if (!istWindows()) gradle.toFile().setExecutable(true);

        List<String> befehl = new ArrayList<>();
        befehl.add(gradle.toString());
        befehl.addAll(Arrays.asList(args));
        ProcessBuilder prozess = new ProcessBuilder(befehl).directory(root.toFile()).inheritIO();
        int code = prozess.start().waitFor();
        System.exit(code);
    }

    private static Path findeProjektWurzel() {
        Path aktuell = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (aktuell != null) {
            if (Files.isRegularFile(aktuell.resolve("settings.gradle.kts"))) return aktuell;
            aktuell = aktuell.getParent();
        }
        throw new IllegalStateException("settings.gradle.kts nicht gefunden");
    }

    private static String erforderlich(Properties p, String schlüssel) {
        String wert = p.getProperty(schlüssel);
        if (wert == null || wert.isBlank()) throw new IllegalArgumentException("Fehlende Eigenschaft: " + schlüssel);
        return wert.replace("\\:", ":");
    }

    private static void ladeHerunter(String url, Path ziel) throws Exception {
        System.out.println("Gradle wird einmalig geladen: " + url);
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        Path tmp = ziel.resolveSibling(ziel.getFileName() + ".teil");
        HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(tmp));
        if (response.statusCode() / 100 != 2) {
            Files.deleteIfExists(tmp);
            throw new IOException("Download fehlgeschlagen, HTTP " + response.statusCode());
        }
        Files.move(tmp, ziel, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void entpacke(Path zip, Path ziel) throws IOException {
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry eintrag;
            while ((eintrag = in.getNextEntry()) != null) {
                Path ausgabe = ziel.resolve(eintrag.getName()).normalize();
                if (!ausgabe.startsWith(ziel)) throw new IOException("Unsicherer ZIP-Eintrag: " + eintrag.getName());
                if (eintrag.isDirectory()) Files.createDirectories(ausgabe);
                else {
                    Files.createDirectories(ausgabe.getParent());
                    Files.copy(in, ausgabe, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static String sha256(Path datei) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(datei)) {
            byte[] puffer = new byte[64 * 1024];
            int n;
            while ((n = in.read(puffer)) >= 0) digest.update(puffer, 0, n);
        }
        StringBuilder text = new StringBuilder();
        for (byte b : digest.digest()) text.append(String.format("%02x", b));
        return text.toString();
    }

    private static boolean istWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }
}
