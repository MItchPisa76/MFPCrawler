package mtc.MFPCrawler;

import java.awt.Color;
import java.awt.GridLayout;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.snmp4j.smi.Null;
import org.snmp4j.smi.Variable;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

class TupleString {
	String s1, s2;

	public TupleString(String ss1, String ss2) {
		s1 = ss1;
		s2 = ss2;
	}

}

class TupleMaintenanceType {
	String s1, s2;

	public TupleMaintenanceType(String ss1, String ss2) {
		s1 = ss1;
		s2 = ss2;
	}

	public void addToPanel(JPanel jp) {

	}
}

public class Resolver {

	static JLabel statusLabel = new JLabel();
	static JLabel optsrcLabel = new JLabel();
	static JLabel lastTimeLabel = new JLabel();
	static JPanel statusPanel;

	static public Hashtable<Integer, TupleString> ht = new Hashtable<Integer, TupleString>();
	static public Hashtable<String, TupleString> rt = new Hashtable<String, TupleString>();

	static public Hashtable<String, Hashtable<String, String>> remapper = new Hashtable<String, Hashtable<String, String>>();
	static public ConcurrentHashMap<String, List<OID_DATA_BASE>> local = new ConcurrentHashMap<String, List<OID_DATA_BASE>>();
	static {
		local.put("1.3.6.1.4.1.236.11.5.1DEPR",
				List.of(new OID_DATA("1.3.6.1.4.1.236.11.5.1.1.1.1.0", "secSysModelName", "Modello", 18, false),
						new OID_DATA("1.3.6.1.4.1.236.11.5.1.1.1.4.0", "secSysSerial", "Seriale", 19, false),
						new OID_DATA("1.3.6.1.4.1.236.11.5.1.1.1.2.0", "secSysFWVersion", "System Version", 17, false),
						new OID_DATA("1.3.6.1.4.1.236.11.5.1.1.1.25643.0", "secSysPuppa", "PUPPA")
				// new OID_DATA("1.3.6.1.4.1.236.11.5.1.1.1.2.0", "secSysFWVersion", "System
				// Version", 17, false)

				));
		local.put("1.3.6.1.4.1.236.11.5.1DEPR:consumabili",
				List.of(new OID_TONER_DATA("1.3.6.1.2.1.43.11.1.1.5.1", "prtMarkerSuppliesType", "Tipo Materiale"),
						new OID_TONER_DATA("1.3.6.1.2.1.43.11.1.1.6.1", "prtMarkerSuppliesDescription", "Nome"),
						new OID_TONER_DATA("1.3.6.1.2.1.43.11.1.1.7.1", "prtMarkerSuppliesSupplyUnit", "Unita"),
						new OID_TONER_DATA("1.3.6.1.2.1.43.11.1.1.8.1", "prtMarkerSuppliesMaxCapacity", "Max"),
						new OID_TONER_DATA("1.3.6.1.2.1.43.11.1.1.9.1", "prtMarkerSuppliesLevel", "Lev")

				));
		local.put("***:consumabili",
				List.of(new OID_TONER_DATA("1.3.6.1.2.1.43.11.1.1.5.1", "prtMarkerSuppliesType", "Tipo Materiale"),
						new OID_TONER_DATA("1.3.6.1.2.1.43.11.1.1.6.1", "prtMarkerSuppliesDescription",
								"Descrizione Materiale"),
						new OID_TONER_DATA("1.3.6.1.2.1.43.11.1.1.7.1", "prtMarkerSuppliesSupplyUnit", "Unita"),
						new OID_TONER_DATA("1.3.6.1.2.1.43.11.1.1.8.1", "prtMarkerSuppliesMaxCapacity", "Max"),
						new OID_TONER_DATA("1.3.6.1.2.1.43.11.1.1.9.1", "prtMarkerSuppliesLevel", "Lev")

				));

		ht.put(0, new TupleString("other", "Errore mr.getResolvedUnits();"));
		ht.put(3, new TupleString("tenThousandthsOfInches",
				"Decimi\ndi millesimo di pollice (usato per misurare fogli/spessori)"));
		ht.put(4, new TupleString("micrometers", "Micrometri"));
		ht.put(7, new TupleString("impressions", "Numero di impressioni/ pagine stampate"));
		ht.put(8, new TupleString("sheets", "Numero di fogli"));
		ht.put(11, new TupleString("percent", "Percentuale (0 - 100%) (Valore più comune per i toner)"));
		ht.put(12, new TupleString("tenthsOfGrams", "Decimi di grammo (peso del toner o inchiostro residuo)"));
		ht.put(13, new TupleString("hundredthsOfFluidOunces", "Centesimi di oncia fluida"));
		ht.put(14, new TupleString("tenthsOfMilliliters", "Decimi di millilitro (volume d'inchiostro)"));
		ht.put(15, new TupleString("feet", "Piedi (per materiali a rotolo)"));
		ht.put(16, new TupleString("meters", "Metri (per materiali a rotolo)"));
		ht.put(19, new TupleString("items", "Unità singole discrete (es. numero di fermagli per cucitrice)"));

	}

	public static boolean isOffLine = false;
	private static TitledBorder b = new TitledBorder("Connection");

	static synchronized boolean onError(String msg, Color c) {
		appendLog("Error:" + msg);
		statusLabel.setText("Error:" + msg);
		statusLabel.setForeground(c);
		isOffLine = false;
		return true;
	}

	static synchronized boolean handShaking() {

		appendLog("  handShacking:" + address);
		statusLabel.setText("Stato: handShacking");
		statusLabel.setForeground(new Color(20, 207, 3));	
		return true;
	}
	
	static synchronized boolean offLine() {
		if (isOffLine)
			return false;
		appendLog(" server OffLine:" + address);
		statusLabel.setText("Stato: OffLine");
		statusLabel.setForeground(Color.pink);
		isOffLine = true;
		return true;
	}

	static synchronized boolean onLine() {
		if (!isOffLine)
			return false;
		appendLog(" server ONLine:" + address);
		statusLabel.setText("Stato: ONLine");
		statusLabel.setForeground(new Color(40, 207, 69));
		isOffLine = false;
		return true;
	}

	public static JPanel updateJPanel() {
		if (statusPanel == null) {
			statusPanel = new JPanel(new GridLayout(-1, 1));
		}
		statusPanel.removeAll();
		optsrcLabel.setFont(optsrcLabel.getFont().deriveFont(5));
		statusPanel.setBorder(b);
		statusPanel.add(statusLabel);
		statusPanel.add(lastTimeLabel);
		statusPanel.add(optsrcLabel);
		statusPanel.updateUI();
		return statusPanel;
	}

	static void resolveLocal(SNMPHost host) {
		resolveRemote(host);
		String vendor = host.values.get(OID_DATA.OID_SYS_OBJECT_ID.OID).toString();
		updateOIDSLocal(host, vendor);
		updateOIDSLocal(host, vendor + ":consumabili");
		updateOIDSLocal(host, vendor + ":alert");

		host.typeRemapper.clear();
		host.typeRemapper.put("15", "11");

		MFPCrawler.crawlerWindow.appendLog("DONE:");
	}

	static InetAddress address;
	static Thread resolverThread;

	private static class ResolverWorker extends CrawlerWorkerBase {
		// public Hashtable<SNMPHost, Date> lastUpdatesMantain = new Hashtable();
		// public Vector<SNMPHost> toUpdateMantain = new Vector<>();

		@Override
		protected Void doInBackground() {// throws Exception {
			try {
				address = InetAddress.getByName(Options.primaryserver);
				appendLog("Resolver avviato:" + Options.primaryserver);

				while (!isCancelled()) {
					try {
						resolverThread = Thread.currentThread();
						Thread.sleep(Options.resolveRespawn);
					} catch (InterruptedException e) {
						// e.printStackTrace();
					}

					try {
						if (!address.isReachable(Options.timeoutMs)) {
							if (offLine())
								continue;
						} else if (onLine())
							continue;

					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					Locale loc = Locale.getDefault();
					LocalDateTime now = LocalDateTime.now();
					// Formatter in Italiano
					DateTimeFormatter itaFormatter = DateTimeFormatter.ofPattern("EEE d MMMM yyyy, HH:mm", loc);
					final String format = now.format(itaFormatter).toString();
					lastTimeLabel.setText(format);
					// toUpdateMantain.clear();
					for (String h : MFPCrawler.crawlerWindow.knownhosts.keySet()) {
						SNMPHost host = MFPCrawler.crawlerWindow.knownhosts.get(h);
						resolveLocal(host);
						host.update();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			stopCrawler();
			return null;
		}

		@Override
		protected void done() {
			appendLog("Scambio dati completato.");
			stopCrawler();
		}

		@Override
		public void stopCrawler() {
			Resolver.stopCrawler();
		}

	}

	static ObjectMapper mapper = new ObjectMapper();
	static ResolverWorker resolverWorker;

	public synchronized static void startCrawler() {

		statusLabel.setText("Resolver: In esecuzione...");
		statusLabel.setForeground(new Color(40, 167, 69));

		resolverWorker = new ResolverWorker();
		resolverWorker.execute();
	}

	private static synchronized void stopCrawler() {

		appendLog("Resolver arresto in corso...");
		if (resolverWorker != null && !resolverWorker.isDone()) {
			resolverWorker.cancel(true);
		}

		statusLabel.setText("Stato: Inattivo");
		statusLabel.setForeground(Color.gray);

	}

	/**
	 * Aggiunge un messaggio al log con timestamp in modo thread-safe.
	 */
	public static void appendLog(String message) {
		if (MFPCrawler.crawlerWindow != null)
			MFPCrawler.crawlerWindow.appendLog(message);
	}

	public static <T extends OID_DATA_BASE> void updateOIDSLocal(SNMPHost host, String vendor) {
		List<OID_DATA_BASE> l = local.get(vendor);
		if (l == null) {
			appendLog("Error:unknown vendor " + vendor);
			return;
		}
		// MFPCrawler.crawlerWindow.appendLog(" resolve vendor " + vendor);
		for (OID_DATA_BASE o : l) {
			OID_DATA_BASE.OIDS.put(o.OID, o);
			if (!host.values.containsKey(o.OID)) {
				host.values.put(o.OID, Null.noSuchInstance);
				MFPCrawler.crawlerWindow.appendLog("   request:" + o.pretty);
				continue;
			}

			Variable v = host.values.get(o);
			if (v == Null.noSuchObject) {
				MFPCrawler.crawlerWindow.appendLog("   error:" + o.pretty);
			} else
				MFPCrawler.crawlerWindow.appendLog("   hasValue:" + o.pretty + (o.isForced() ? "[forced]" : ""));
		}
	}

	public static <T extends OID_DATA_BASE> void resolveOIDSRemote(String vendor,
			Function<Map<String, String>, T> factory) {
		String url = Options.getResolveServer() + "/oids/vendor/" + vendor;
		List<OID_DATA_BASE> l = local.get(vendor);
		if (l == null) {
			local.put(vendor, l = new ArrayList<OID_DATA_BASE>());
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
					.header("Content-Type", "application/json").header("token", Options.token).GET().build();

			HttpClient client = Options.getHttpClient();

			try {

				System.out.println("Invio resolveOIDSRemote get in corso..." + url);
				HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

				if (response.statusCode() == 200) {
					String json = response.body();

					ArrayList<Map<String, String>> jmap = mapper.readValue(json,
							new TypeReference<ArrayList<Map<String, String>>>() {
							});

					for (Map<String, String> entry : jmap) {
						OID_DATA_BASE o = factory.apply(entry);
						l.add(o);
					}
					appendLog("register:" + url);
				} else {
					appendLog("Error:" + url + ">" + response.body());
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	static void resolveRemote(SNMPHost host) {
		Variable v = host.values.get(OID_DATA.OID_SYS_OBJECT_ID.OID);
		if (v == null)
			return;
		String vendor = v.toString();

		HttpClient client = Options.getHttpClient();

		if (client == null) {
			appendLog("  errore http client non avviato:");
			return;
		}

		resolveOIDSRemote(vendor, OID_DATA::new);
		resolveOIDSRemote(vendor + ":consumabili", OID_TONER_DATA::new);
		resolveOIDSRemote(vendor + ":alert", OID_ALERT_DATA::new);

	}
}
