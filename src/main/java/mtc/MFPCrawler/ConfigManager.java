package mtc.MFPCrawler;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class ConfigManager {

	private static final String APP_FOLDER_NAME = "MFPCrawler";


	/**
	 * Determina la cartella dati specifica per l'OS in uso.
	 */
	public static Path getAppDataDirectory() {
		String os = System.getProperty("os.name").toLowerCase();
		String userHome = System.getProperty("user.home");

		if (os.contains("win")) {
			// Windows: %APPDATA%\MFPCrawler
			String appData = System.getenv("APPDATA");
			return (appData != null) ? Paths.get(appData, APP_FOLDER_NAME)
					: Paths.get(userHome, "AppData", "Roaming", APP_FOLDER_NAME);
		} else if (os.contains("mac")) {
			// macOS: ~/Library/Application Support/MFPCrawler
			return Paths.get(userHome, "Library", "Application Support", APP_FOLDER_NAME);
		} else {
			// Linux e Unix-like: ~/.config/MFPCrawler
			String xdgConfig = System.getenv("XDG_CONFIG_HOME");
			return (xdgConfig != null) ? Paths.get(xdgConfig, APP_FOLDER_NAME)
					: Paths.get(userHome, ".config", APP_FOLDER_NAME);
		}
	}

	/**
	 * Inizializza ed ottiene il file di configurazione modificabile
	 * dall'applicazione.
	 */
	public static File getConfigFile(String filename) {
		try {
			Path appDir = getAppDataDirectory();

			// Crea la directory se non esiste (es. ~/.config/MFPCrawler)
			if (Files.notExists(appDir)) {
				Files.createDirectories(appDir);
			}

			Path configFile = appDir.resolve(filename);

			// Se il file di configurazione non esiste, ne estrae una copia di default dal
			// JAR
			if (Files.notExists(configFile)) {
				try (InputStream defaultConfigStream = ConfigManager.class
						.getResourceAsStream("/" + filename)) {
					if (defaultConfigStream != null) {
						Files.copy(defaultConfigStream, configFile, StandardCopyOption.REPLACE_EXISTING);
					} else {
						// Se non c'è un file di default nelle risorse, crea un file vuoto
						Files.createFile(configFile);
					}
				}
			}

			return configFile.toFile();

		} catch (Exception e) {
			e.printStackTrace();
			// Fallback sulla directory corrente in caso di eccezioni critiche di I/O
			return new File(filename);
		}

	}
}