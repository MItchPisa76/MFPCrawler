package mtc.MFPCrawler;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigLoader {

    /**
     * Restituisce il percorso assoluto della directory di installazione dell'applicazione.
     */
    public static Path getAppInstallationDir() {
        try {
            // Ottiene la posizione del file JAR/classe in esecuzione
            File jarFile = new File(ConfigLoader.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());

            // Se l'app è in un JAR (target/app/app.jar), restituisce la cartella padre
            if (jarFile.isFile()) {
                return jarFile.getParentFile().toPath();
            } else {
                // In ambiente di sviluppo (IDE), restituisce la root del progetto
                return jarFile.toPath();
            }
        } catch (URISyntaxException e) {
            // Fallback sulla directory corrente se fallisce la reflection
            return Paths.get(".").toAbsolutePath().normalize();
        }
    }

    /**
     * Recupera il file di configurazione situato nella root di installazione.
     */
    public static File getConfigFile(String fileName) {
        Path configPath = getAppInstallationDir().resolve(fileName);
        return configPath.toFile();
    }
}