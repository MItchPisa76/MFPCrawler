package mtc.MFPCrawler;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class Options {

	enum OPTIONSSOURCE {
		base, file, server, modif
	}

	public static String theme = "dark";
	public static Integer fontSize = 16;
	public static Boolean autoSave = true;
	public static String primaryserver = "localhost";
	public static String primarysPort = "3081";

	private transient static OPTIONSSOURCE _OptionsSource = OPTIONSSOURCE.base;

	public static void setOptionSource(OPTIONSSOURCE s) {
		_OptionsSource = s;
		Resolver.optsrcLabel.setText(s.name());
	}

	public static String token = "PLEASE";

	public static Integer timeoutMs = 10000;
	public static String community = "public";
	public static String lastIPQuery = "";
	public static String language = "it";

	public static String defaultFileName = "default.json";
	public static Integer resolveRespawn = 10000;
	public static Integer resolveOptionsRespawn = 1000;

	private Options() {
	}

	public transient static final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

	private transient static HttpClient _client = null;

	public static HttpClient getHttpClient() {
		if (_client != null)
			return _client;
		try {
			KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			trustStore.load(null, null); // Inizializza un truststore vuoto in memoria

			try (InputStream is = MFPCrawler.class.getClassLoader().getResourceAsStream("cert.pem")) {
				if (is == null) {
					throw new IllegalArgumentException(
							"Errore: Il file cert.pem non è stato trovato in src/main/resources!");
				}

				CertificateFactory cf = CertificateFactory.getInstance("X.509");
				X509Certificate caCert = (X509Certificate) cf.generateCertificate(is);

				// Registra il certificato del tuo server come attendibile
				trustStore.setCertificateEntry("mio-server-custom", caCert);
			}

			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(trustStore);

			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, tmf.getTrustManagers(), new java.security.SecureRandom());

			// 3. CREA L'HTTPCLIENT INIETTANDO LA CONFIGURAZIONE SSL
			_client = HttpClient.newBuilder().sslContext(sslContext).build();
		} catch (NoSuchAlgorithmException e1) {
			Resolver.onError(e1.getMessage(), Color.red);
			e1.printStackTrace();
		} catch (KeyManagementException e1) {
			Resolver.onError(e1.getMessage(), Color.red);
			e1.printStackTrace();
		} catch (KeyStoreException e1) {
			Resolver.onError(e1.getMessage(), Color.red);
			e1.printStackTrace();
		} catch (CertificateException e1) {
			Resolver.onError(e1.getMessage(), Color.red);
			e1.printStackTrace();
		} catch (IOException e1) {
			Resolver.onError(e1.getMessage(), Color.red);
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return _client;
	}

	public static String getResolveServer() {
		return "https://" + primaryserver + ":" + primarysPort;
	}

	public static void readServerResponse(String json) {
		try {
			System.out.println("Contenuto: " + json);

			Map<String, String> jmap = mapper.readValue(json, new TypeReference<Map<String, String>>() {
			});
			String ntoken = jmap.get("token");
			if (!Options.token.equals(ntoken)) {

				System.out.println("renew:" + Options.token + ">" + ntoken);
				Options.token = ntoken;
			}

			jmap = mapper.readValue(jmap.get("options"), new TypeReference<Map<String, String>>() {
			});

			// Iterazione su tutte le coppie di stringhe
			for (Map.Entry<String, String> entry : jmap.entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue();
				System.out.println(key + " = " + value);
				for (Field field : Options.class.getDeclaredFields()) {
					if (field.getName().equals(entry.getKey())) {
						if (entry.getValue() != null) {
							try {
								setStaticFieldValue(field, entry.getValue());
								System.out.println("  Aggiorno:" + entry.getKey() + ">" + entry.getValue());
							} catch (IllegalAccessException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

						} else {
							System.out.println("     ***SKIP:" + entry.getKey());
						}

						break;
					}

				}
			}

		} catch (JsonMappingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (JsonProcessingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public static void sendToServer() {
		try {
			if (!Resolver.isOffLine) {

				setOptionSource(_OptionsSource.server);
				Map<String, Object> map = null;
				try {
					map = getMap();
				} catch (IllegalArgumentException | IllegalAccessException e) {
					Resolver.onError(e.getMessage(), Color.red);

					e.printStackTrace();
					return;
				}
				String localData = mapper.writeValueAsString(MFPCrawler.crawlerWindow.localData);
				map.put("localData", localData);
				String jsonPayload = mapper.writeValueAsString(map);

				HttpClient client = getHttpClient();

				if (client == null)
					return;
				String str = Options.getResolveServer() + "/options";
				HttpRequest request = HttpRequest.newBuilder().uri(URI.create(str))
						.header("Content-Type", "application/json").header("token", token)
						.POST(BodyPublishers.ofString(jsonPayload)).build();

				System.out.println("Invio richiesta protetta in corso..." + str);
				System.out.println(">" + jsonPayload);
				HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

				// 5. STAMPA IL RISULTATO
				System.out.println("Risposta ricevuta! Codice di stato: " + response.statusCode());
				if (response.statusCode() == 200) {
					readServerResponse(response.body());
					respawnSendOptions = false;
				} else {
					Resolver.onError(response.body() + ":" + response.statusCode(), Color.orange);
					System.err.println("Impossibile recuperare le opzioni: " + response.statusCode());
				}
			}
		} catch (InterruptedException | JsonProcessingException e) {
			Resolver.onError(e.getMessage(), Color.red);
			e.printStackTrace();
		} catch (IOException e1) {
			Resolver.onError("Errore server", Color.red);
			e1.printStackTrace();
		}
	}

	public static void getFromServer() {
		try {
			if (!Resolver.isOffLine) {

				String url = Options.getResolveServer() + "/options";

				HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
						.header("Content-Type", "application/json").header("token", token).GET().build();

				System.out.println("Invio richiesta get protetta in corso..." + url);

				HttpClient client = getHttpClient();
				if (client == null)
					return;
				HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

				// 5. STAMPA IL RISULTATO
				System.out.println("Risposta ricevuta! Codice di stato: " + response.statusCode());
				if (response.statusCode() == 200) {
					readServerResponse(response.body());

					setOptionSource(_OptionsSource.server);
				} else {
					Resolver.onError(response.body() + ":" + response.statusCode(), Color.orange);
					System.err.println("Impossibile recuperare le opzioni: " + response.statusCode());
				}

			}
		} catch (InterruptedException | JsonProcessingException e) {
			Resolver.onError(e.getMessage(), Color.red);
			e.printStackTrace();
		} catch (IOException e1) {
			Resolver.onError("Errore server", Color.red);
			e1.printStackTrace();
		}
	}

	/**
	 * Salva su file JSON tutti i campi statici presenti nella classe Options.
	 * 
	 * @throws Exception
	 */

	public static void save() {
		try {
			sendToServer();
			setOptionSource(_OptionsSource.file);
			File defaultFile = ConfigManager.getConfigFile(defaultFileName);
			save(defaultFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void setStaticFieldValue(Field field, String value) throws IllegalAccessException {
		Class<?> type = field.getType();

		if (type == String.class) {
			field.set(null, value);
		} else if (type == int.class || type == Integer.class) {
			field.set(null, Integer.parseInt(value));
		} else if (type == boolean.class || type == Boolean.class) {
			field.set(null, Boolean.parseBoolean(value));
		} else if (type == double.class || type == Double.class) {
			field.set(null, Double.parseDouble(value));
		} else if (type == long.class || type == Long.class) {
			field.set(null, Long.parseLong(value));
		} else if (type == float.class || type == Float.class) {
			field.set(null, Float.parseFloat(value));
		} else {
			// Per altri tipi di oggetti
			field.set(null, value);
		}
	}

	public static void save(File file) throws IOException, IllegalAccessException {
		mapper.writeValue(file, getMap());
	}

	public static Map<String, Object> getMap() throws IllegalArgumentException, IllegalAccessException {
		Map<String, Object> map = new HashMap<>();

		for (Field field : Options.class.getDeclaredFields()) {
			int modifiers = field.getModifiers();

			// Considera solo i campi statici non-finali e non-sintetici
			if (!Modifier.isTransient(modifiers) && Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers)
					&& !field.isSynthetic()) {
				// field.setAccessible(true);
				map.put(field.getName(), field.get(null));
			}
		}
		return map;
	}

	/**
	 * Legge il file JSON e assegna i valori ai campi statici corrispondenti nella
	 * classe Options.
	 */

	public static void load() {
		try {
			if (!_OptionsSource.equals(_OptionsSource.server)) {
				File defaultFile = ConfigManager.getConfigFile(defaultFileName);
				load(defaultFile);
			}
			getFromServer();
		} catch (IllegalAccessException | IOException e) {
			// TODO Auto-generated catch block
			setOptionSource(_OptionsSource.base);

			e.printStackTrace();
		}
	}

	public static void load(File file) throws IOException, IllegalAccessException {
		if (!file.exists()) {
			File defaultFile = ConfigManager.getConfigFile(defaultFileName);
			save(defaultFile);
			return;
		}
		setOptionSource(_OptionsSource.file);

		Map<String, Object> map = mapper.readValue(file, new TypeReference<Map<String, Object>>() {
		});

		for (Field field : Options.class.getDeclaredFields()) {
			try {
				int modifiers = field.getModifiers();

				if (Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers) && !field.isSynthetic()) {
					if (map.containsKey(field.getName())) {
						field.setAccessible(true);
						Object value = map.get(field.getName());

						setFieldValue(field, value);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static void setFieldValue(Field field, Object value) throws IllegalAccessException {
		if (value == null) {
			field.set(null, null);
			return;
		}

		Class<?> type = field.getType();

		// Conversione sicura dei numeri da JSON (Jackson usa Integer/Double di default)
		if (type == int.class || type == Integer.class) {
			field.set(null, ((Number) value).intValue());
		} else if (type == double.class || type == Double.class) {
			field.set(null, ((Number) value).doubleValue());
		} else if (type == float.class || type == Float.class) {
			field.set(null, ((Number) value).floatValue());
		} else if (type == long.class || type == Long.class) {
			field.set(null, ((Number) value).longValue());
		} else if (type == boolean.class || type == Boolean.class) {
			field.set(null, value);
		} else {
			field.set(null, value);
		}
	}

	public static boolean respawnSendOptions = false;

	private static class OptionsWorker extends CrawlerWorkerBase {

		static class RemoteCommands extends Object {
			public RemoteCommands() {

			}

			public String forceResend = "false";
			public String findNewHosts = "once";
			public String refreshoids = "once";
			public String queryoids = "false";
			public String queryoidsAction = "";
			public String queryoidsSerial = "";
			public String queryoidsIPv4 = "";

		}

		InetAddress address;

		@Override
		protected Void doInBackground() {// throws Exception {
			try {
				address = InetAddress.getByName(Options.primaryserver);
				appendLog("Options avviato:" + Options.primaryserver);

				while (!isCancelled()) {
					Thread.sleep(Options.resolveOptionsRespawn);
					if (respawnSendOptions) {
						sendToServer();
						continue;
					}

					try {
						if (!address.isReachable(Options.timeoutMs)) {
							if (Resolver.offLine())
								continue;
						} else if (Resolver.onLine())
							continue;

						HttpClient client = Options.getHttpClient();
						String url = Options.getResolveServer() + "/hello";

						Builder builder = HttpRequest.newBuilder().uri(URI.create(url))
								.header("Content-Type", "application/json").header("token", Options.token);

						HttpRequest request = builder.GET().build();

						HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

						if ((response.statusCode() == 200)) {
							Map<String, String> jmap = mapper.readValue(response.body(),
									new TypeReference<Map<String, String>>() {
									});
							boolean updateRemote = false;
							RemoteCommands rc;
							if ((jmap.get("crawler") != null) && (jmap.get("crawler") != "")) {
								rc = mapper.readValue(jmap.get("crawler"), RemoteCommands.class);
								// cmds = mapper.readValue(crawlerJson, RemoteCommands.class); {
								// });
								// RemoteCommands cmds = jmap.get("forceResend");
								// if ((cmds != null) && (cmds.equals("true")))

								if (rc.forceResend.equals("true"))
									respawnSendOptions = true;
								if (rc.forceResend.equals("once")) {
									respawnSendOptions = true;
									rc.forceResend = "false";
									updateRemote = true;
								}

								if (rc.findNewHosts.equals("true"))
									MFPCrawler.crawlerWindow.startCrawler();
								if (rc.findNewHosts.equals("once")) {
									MFPCrawler.crawlerWindow.startCrawler();
									rc.findNewHosts = "false";
									updateRemote = true;
								}
								if (rc.refreshoids.equals("true")) {
									Resolver.local.clear();
								}
								if (rc.refreshoids.equals("once")) {
									Resolver.local.clear();
									rc.refreshoids = "false";
									updateRemote = true;
								}

								if (rc.queryoidsAction.equals("true"))
									;
								if (rc.queryoidsAction.equals("once")) {

									rc.queryoidsAction = "false";
									SNMPHost h = MFPCrawler.crawlerWindow.knownhosts.get(rc.queryoidsIPv4);
									if (h != null) {
										h.sendSNMPQuery(rc.queryoids);
									}
									updateRemote = true;
								}
							} else {
								rc = new RemoteCommands();
								updateRemote = true;
							}
							// cmds = jmap.get("findNewHosts");
							// if ((cmds != null) && (cmds.equals("true"))) {

							// }
							if (updateRemote) {
								String jsonPayload = mapper.writeValueAsString(rc);

								String str = Options.getResolveServer() + "/hello";
								request = builder.POST(BodyPublishers.ofString(jsonPayload)).build();

								appendLog("Invio aggiornamento options..." + str);
								System.out.println(">" + jsonPayload);

								client.send(request, HttpResponse.BodyHandlers.ofString());
							}

						} else {
							appendLog(response.body());
						}
					} catch (IOException | InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
			stopCrawler();
			return null;
		}

		@Override
		protected void process(List<String> chunks) {
			for (String msg : chunks) {
				appendLog(msg);
			}
		}

		@Override
		protected void done() {

			stopCrawler();
		}

		@Override
		public void stopCrawler() {
			appendLog("ERRORE options stopped");
		}

	}

	public synchronized static void startCrawler() {

		OptionsWorker crawler = new OptionsWorker();
		crawler.execute();
	}

}