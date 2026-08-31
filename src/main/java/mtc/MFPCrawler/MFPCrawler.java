package mtc.MFPCrawler;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

//Import di SNMP4J (Assicurati di aver aggiunto la dipendenza nel progetto)
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OctetString;
import org.snmp4j.transport.DefaultUdpTransportMapping;

public class MFPCrawler extends JFrame {

	private JTextArea logArea;
	private JButton startButton;
	private JButton stopButton;
	private JLabel statusLabel;

	JTextField txtIpPattern;

	private JTabbedPane jtp;
	private JPanel outJP;
	private GridBagConstraints gbc = new GridBagConstraints();

	private SystemTray systemTray;
	private TrayIcon trayIcon;
	private CrawlerWorker crawlerWorker;
	private boolean isRunning = false;

	public MFPCrawler() {
		super("MFP Crawler");
		initUI();
		initSystemTray();
	}

	public static class LocalData {
		public String localIF;
		public String localIPv4;
		public String localHostName;
		public String localSubnet;
	}
	
	public LocalData localData = new LocalData();

	public int[] scanner_start = { 192, 168, 2, 1 };
	public int[] scanner_stop = { 192, 168, 2, 254 };
	// JTextField jtf_subnetPrefix;
	// JTextField jtf_subnetFrom;
	// JTextField jtf_subnetTo;

	// Utility per convertire la notazione CIDR (es. /24) in formato IP
	// (255.255.255.0)
	private static String convertiPrefissoInSubnet(short prefix) {
		int mask = 0xffffffff << (32 - prefix);
		int value = mask;
		byte[] bytes = new byte[] { (byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value };
		try {
			return InetAddress.getByAddress(bytes).getHostAddress();
		} catch (Exception e) {
			return "N/D";
		}
	}

	JPanel netPanel;

	// --- FILTRO PER IL JTEXTFIELD ---
	// Permette la digitazione solo di cifre, punti, asterischi e trattini
	private class IpPatternFilter extends DocumentFilter {
		private final Pattern permesso = Pattern.compile("^[0-9\\.\\*\\-]*$");

		JLabel status;

		IpPatternFilter(JLabel sta) {
			super();
			status = sta;
		}

		@Override
		public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
			// TODO Auto-generated method stub
			super.remove(fb, offset, length);

			eseguiParsing(status);
		}

		@Override
		public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
				throws BadLocationException {
			if (string.contains("\n"))
				eseguiParsing(status);
			if (permesso.matcher(string).matches()) {
				super.insertString(fb, offset, string, attr);
				eseguiParsing(status);
			}
		}

		@Override
		public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
				throws BadLocationException {
			if (text.contains("\n"))
				eseguiParsing(status);
			if (permesso.matcher(text).matches()) {
				super.replace(fb, offset, length, text, attrs);
				eseguiParsing(status);
			}
		}
	}

	private void eseguiParsing(JLabel status) {
		String input = txtIpPattern.getText().trim();

		if (input.equals("")) {
			String str = localData.localIPv4.substring(0,  localData.localIPv4.lastIndexOf(".")) + ".*";
			txtIpPattern.setText(str);
		}
		// Validazione formale tramite espressione regolare adattata per range e
		// asterischi
		String regexPart = "(\\d{1,3}|\\*|\\d{1,3}-\\d{1,3})";
		String fullRegex = "^" + regexPart + "\\." + regexPart + "\\." + regexPart + "\\." + regexPart + "$";

		if (!Pattern.matches(fullRegex, input)) {
			status.setText("192.168.1.1\n- 192.168.1.*\n- 192.168.1.1-50");
			status.setForeground(Color.red);
			// "Errore", JOptionPane.ERROR_MESSAGE);)
			// JOptionPane.showMessageDialog(this,
			// "Formato IP non valido!\nUsa formati come:\n- 192.168.1.1\n- 192.168.1.*\n-
			// 192.168.1.1-50",
			// "Errore", JOptionPane.ERROR_MESSAGE);
			return;
		}

		String[] arr = input.split("\\.");
		for (int i = 0; i < 4; i++) {
			String s = arr[i];
			if (s.equals("*")) {
				scanner_start[i] = 1;
				scanner_stop[i] = 254;
				continue;
			}
			if (s.contains("-")) {
				String[] arrr = s.split("-");
				scanner_start[i] = Integer.parseInt(arrr[0]);
				scanner_stop[i] = Integer.parseInt(arrr[1]);
				continue;
			}
			scanner_start[i] = Integer.parseInt(s);
			scanner_stop[i] = Integer.parseInt(s);
		}

		// Se il formato è corretto, puoi procedere a espandere gli IP (logica per il
		// tuo scanner)
		// JOptionPane.showMessageDialog(this, "Formato valido! Pronto per il parsing
		// del range:\n" + input, "Successo",
		// JOptionPane.INFORMATION_MESSAGE);
		status.setText("OK");
		status.setForeground(jtp.getForeground());
	}

	private JPanel getLocalNetworkPanel() {
		try {
			// Ottiene l'elenco di tutte le interfacce di rete attive sul computer
			Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();

			for (NetworkInterface netIf : Collections.list(nets)) {
				// Salta le interfacce disattivate o quelle di loopback (127.0.0.1)
				if (!netIf.isUp() || netIf.isLoopback()) {
					continue;
				}

				// System.out.println("Interfaccia: " + netIf.getDisplayName() + " [" +
				// netIf.getName() + "]");

				// Ottiene tutti gli indirizzi IP associati a questa specifica interfaccia
				List<InterfaceAddress> inetAddresses = netIf.getInterfaceAddresses();
				for (InterfaceAddress ifAddress : inetAddresses) {
					InetAddress inetAddr = ifAddress.getAddress();

					// Filtra per mostrare solo IPv4 (più leggibile per la subnet)
					if (inetAddr instanceof Inet4Address) {
						 localData.localIF = netIf.getDisplayName();
						 localData.localHostName = inetAddr.getHostName();
						 localData.localIPv4 = inetAddr.getHostAddress();
						short prefixLength = ifAddress.getNetworkPrefixLength();
						 localData.localSubnet = convertiPrefissoInSubnet(prefixLength) + " (/" + prefixLength + ")";

						System.out.println("Interfaccia: " +  localData.localIF);
						System.out.println("  IP locale:   " +  localData.localIPv4 + "   " +  localData.localHostName);

						// Calcola la Subnet Mask dalla lunghezza del prefisso (es. /24 ->
						// 255.255.255.0)

					}
					// System.out.println(" -> Indirizzo IP: " + inetAddress.getHostAddress() +
					// ":"+inetAddress.);
				}
			}
		} catch (SocketException e) {
			System.err.println("Errore nel recupero delle informazioni di rete: " + e.getMessage());
		}

		if (netPanel == null)
			netPanel = new JPanel(new GridBagLayout());
		netPanel.removeAll();
		// Aggiunge un po' di spazio vuoto (padding) attorno al pannello
		netPanel.setBorder(new TitledBorder( localData.localIF));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(6, 10, 6, 10); // Spazio tra i singoli elementi
		gbc.anchor = GridBagConstraints.WEST; // Allinea il testo a sinistra

		// --- Configurazione Font ---
		Font fontLabel = new Font("Arial", Font.BOLD, 12);
		Font fontValue = new Font("Arial", Font.PLAIN, 12);
		Font fontMsg = new Font("Arial", Font.PLAIN, 8);

		// ================= RIGA 1 =================
		// Hostname
		gbc.gridx = 0;
		gbc.gridy = 0;
		JLabel lblHost = new JLabel("Hostname:");
		lblHost.setFont(fontLabel);
		netPanel.add(lblHost, gbc);

		gbc.gridx = 1;
		JLabel txtHost = new JLabel( localData.localHostName);
		txtHost.setFont(fontValue);
		netPanel.add(txtHost, gbc);

		// IP Locale
		gbc.gridx = 2;
		JLabel lblIp = new JLabel("IP Locale:");
		lblIp.setFont(fontLabel);
		netPanel.add(lblIp, gbc);

		gbc.gridx = 3;

		JLabel txtIp = new JLabel( localData.localIPv4);
		txtIp.setFont(fontValue);
		netPanel.add(txtIp, gbc);

		// ================= RIGA 2 =================
		// Subnet Mask
		gbc.gridx = 0;
		gbc.gridy = 1;
		JLabel lblSubnet = new JLabel("Subnet Mask:");
		lblSubnet.setFont(fontLabel);
		netPanel.add(lblSubnet, gbc);

		gbc.gridx = 1;
		JLabel txtSubnet = new JLabel( localData.localSubnet);
		txtSubnet.setFont(fontValue);
		netPanel.add(txtSubnet, gbc);

		// Interfaccia
		gbc.gridx = 2;
		JPanel jp = new JPanel(new GridLayout(2, 2));
		jp.setBorder(new EmptyBorder(2, 2, 2, 2));
		JLabel lblInterface = new JLabel("Scanner:");
		lblInterface.setFont(fontLabel);
		jp.add(lblInterface);

		txtIpPattern = new JTextField(18);
		txtIpPattern.setFont(new Font("Monospaced", Font.PLAIN, 13));
		txtIpPattern.setText(Options.lastIPQuery);

		// btnVerifica = new JButton("Valida e Genera IP");
		// btnVerifica.addActionListener(e -> eseguiParsing());

		txtIpPattern.setFont(fontValue);

		jp.add(txtIpPattern);

		JLabel verifica = new JLabel("verifica:");
		verifica.setFont(fontMsg);
		jp.add(verifica);
		eseguiParsing(verifica);

		// 2. Applica il filtro per accettare solo numeri, punti, asterischi e trattini
		((AbstractDocument) txtIpPattern.getDocument()).setDocumentFilter(new IpPatternFilter(verifica));
		txtIpPattern.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {

				super.keyReleased(e);
				if (e.getKeyChar() == '\n') {
					eseguiParsing(verifica);
					startCrawler();
				}

			}
		});

		JPanel jpp = new JPanel();
		startButton.setFont(fontMsg);
		jpp.add(startButton);

		stopButton.setFont(fontMsg);
		jpp.add(stopButton);
		jp.add(jpp);
		// JLabel txtInterface = new JLabel(localIF);

		gbc.gridx = 3;

		gbc.gridwidth = 2;
		netPanel.add(jp, gbc);

		gbc.gridwidth = 1;

		return netPanel;
	}

	private void initUI() {
		// Intercettiamo la chiusura della finestra per nasconderla nella tray invece di
		// chiuderla
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setSize(650, 420);
		setLocationRelativeTo(null);
		setLayout(new BorderLayout(10, 10));

		// --- Pannello Superiore (Stato) ---
		JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
		topPanel.setBorder(BorderFactory.createEtchedBorder());
		statusLabel = new JLabel("Stato: Inattivo");
		statusLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
		topPanel.add(statusLabel);
		add(topPanel, BorderLayout.NORTH);

		jtp = new JTabbedPane();
		// --- Area Centrale (Console / Log Operazioni) ---

		logArea = new JTextArea();

		logArea.setEditable(false);
		logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
		JScrollPane scrollPane = new JScrollPane(logArea);
		scrollPane.setBorder(BorderFactory.createTitledBorder("Log Attività Crawler"));

		jtp.add(scrollPane, "LOG");
		add(jtp, BorderLayout.CENTER);

		outJP = new JPanel(new GridBagLayout());
		JScrollPane scrollPaneOut = new JScrollPane(outJP);
		jtp.add(scrollPaneOut, "MFPCrawler");
		// --- Pannello Inferiore (Controlli) ---
		JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

		// jtf_subnetPrefix = new JTextField(CrawlerWorker.subnetPrefix);
		// jtf_subnetFrom = new JTextField(CrawlerWorker.subnetInterval_from);
		// jtf_subnetTo = new JTextField(CrawlerWorker.subnetInterval_to);

		startButton = new JButton("Avvia Crawler");
		stopButton = new JButton("Interrompi");
		JButton hideButton = new JButton("Riduci a Icona (Tray)");

		stopButton.setEnabled(false);

		startButton.addActionListener(e -> startCrawler());
		stopButton.addActionListener(e -> stopCrawler());
		hideButton.addActionListener(e -> minimizeToTray());

		bottomPanel.add(Resolver.updateJPanel());
		bottomPanel.add(getLocalNetworkPanel());

		// bottomPanel.add(jtf_subnetPrefix);
		// bottomPanel.add(jtf_subnetFrom);
		// bottomPanel.add(jtf_subnetTo);
		// bottomPanel.add(startButton);
		// bottomPanel.add(stopButton);
		bottomPanel.add(hideButton);
		add(bottomPanel, BorderLayout.SOUTH);

		// Gestore per intercettare la riduzione a icona e il pulsante di chiusura della
		// finestra (X)
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowIconified(WindowEvent e) {
				minimizeToTray();
			}

			@Override
			public void windowClosing(WindowEvent e) {
				minimizeToTray();
			}
		});
	}

	private void initSystemTray() {
		if (!SystemTray.isSupported()) {
			appendLog("ATTENZIONE: SystemTray non supportato su questo sistema.");
			return;
		}

		systemTray = SystemTray.getSystemTray();
		Image iconImage = createFallbackIcon();

		// Popup Menu al tasto destro sull'icona della Tray
		PopupMenu popup = new PopupMenu();

		MenuItem openItem = new MenuItem("Apri Finestra MFPCrawler");
		openItem.addActionListener(e -> restoreFromTray());

		MenuItem startTrayItem = new MenuItem("Avvia Crawler");
		startTrayItem.addActionListener(e -> startCrawler());

		MenuItem stopTrayItem = new MenuItem("Interrompi Crawler");
		stopTrayItem.addActionListener(e -> stopCrawler());

		MenuItem exitItem = new MenuItem("Esci Definitivamente");
		exitItem.addActionListener(e -> shutdown());

		popup.add(openItem);
		popup.addSeparator();
		popup.add(startTrayItem);
		popup.add(stopTrayItem);
		popup.addSeparator();
		popup.add(exitItem);

		trayIcon = new TrayIcon(iconImage, "MFP Crawler", popup);
		trayIcon.setImageAutoSize(true);

		// Il doppio clic sull'icona della barra orologio ripristina la finestra
		trayIcon.addActionListener(e -> restoreFromTray());

		try {
			systemTray.add(trayIcon);
		} catch (AWTException e) {
			appendLog("Impossibile registrare la TrayIcon: " + e.getMessage());
		}
	}

	/**
	 * Nasconde la finestra principale e mostra una notifica a comparsa.
	 */
	private void minimizeToTray() {
		if (SystemTray.isSupported() && trayIcon != null) {
			setVisible(false);
			trayIcon.displayMessage("MFP Crawler", "L'applicazione è attiva in background nella barra orologio.",
					TrayIcon.MessageType.INFO);
		} else {
			setState(Frame.ICONIFIED);
		}
	}

	/**
	 * Ripristina la finestra principale portandola in primo piano.
	 */
	private void restoreFromTray() {
		setVisible(true);
		setState(Frame.NORMAL);
		toFront();
		requestFocus();
	}

	/**
	 * Avvia il thread di crawling in background.
	 */
	public synchronized void startCrawler() {
		if (isRunning)
			return;
		Options.lastIPQuery = txtIpPattern.getText();
		isRunning = true;
		statusLabel.setText("Stato: In esecuzione...");
		statusLabel.setForeground(new Color(40, 167, 69));
		startButton.setEnabled(false);
		stopButton.setEnabled(true);
		appendLog("Servizio MFPCrawler avviato.");

		crawlerWorker = new CrawlerWorker();
		crawlerWorker.execute();
	}

	/**
	 * Richiede l'arresto del thread del crawler.
	 */
	private synchronized void stopCrawler() {
		if (!isRunning)
			return;

		appendLog("Richiesta di arresto in corso...");
		if (crawlerWorker != null && !crawlerWorker.isDone()) {
			crawlerWorker.cancel(true);
		}
		isRunning = false;
		statusLabel.setText("Stato: Inattivo");
		statusLabel.setForeground(Color.BLACK);
		startButton.setEnabled(true);
		stopButton.setEnabled(false);
	}

	public Hashtable<String, SNMPHost> knownhosts = new Hashtable();

	/*
	 * public class Base_OID_SYS { private static final String OID_SYS_NAME =
	 * "1.3.6.1.2.1.1.5.0"; // sysName private static final String OID_SYS_DESCR =
	 * "1.3.6.1.2.1.1.1.0"; // sysDescr private static final List<OID_DATA>
	 * _baseList = new ArrayList();
	 * 
	 * protected class OID_DATA { String OID; String pretty = null; String name =
	 * null; String value = null; boolean ro = true;
	 * 
	 * public OID_DATA(String o) { OID = o; }
	 * 
	 * public String getOID() { return OID; }
	 * 
	 * };
	 * 
	 * public List<OID_DATA> getQueryList() { return _baseList; } }
	 * 
	 * public class MFP_BASE_OID extends Base_OID_SYS { private static final String
	 * OID_SYS_SERIAL = "1.3.6.1.2.1.43.5.1.1.17.1";// "1.3.6.1.2.1.43.5.1.1.17"; //
	 * serial }
	 */

	private class CrawlerWorker extends CrawlerWorkerBase {
		// static String subnetPrefix = "192.168.2";
		// static String subnetInterval_from = "148";
		// static String subnetInterval_to = "160";
		private Boolean registerNOSNMP = true;

		// Utility per convertire la notazione CIDR (es. /24) in formato IP
		// (255.255.255.0)
		private static String convertiPrefissoInSubnet(short prefix) {
			int mask = 0xffffffff << (32 - prefix);
			int value = mask;
			byte[] bytes = new byte[] { (byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8),
					(byte) value };
			try {
				return InetAddress.getByAddress(bytes).getHostAddress();
			} catch (Exception e) {
				return "N/D";
			}
		}

		@Override
		protected Void doInBackground() throws Exception {
			// Pool di thread per scansionare la rete velocemente in parallelo
			ExecutorService executor = Executors.newFixedThreadPool(30);
			// final String subnetPrefix = jtf_subnetPrefix.getText();
			// final String subnetFrom = jtf_subnetFrom.getText();
			// final String subnetTo = jtf_subnetTo.getText();

			for (int i0 = scanner_start[0]; i0 <= scanner_stop[0]; i0++)
				for (int i1 = scanner_start[1]; i1 <= scanner_stop[1]; i1++)
					for (int i2 = scanner_start[2]; i2 <= scanner_stop[2]; i2++)
						for (int i3 = scanner_start[3]; i3 <= scanner_stop[3]; i3++) {

							if (isCancelled())
								break;

							String ip = i0 + "." + i1 + "." + i2 + "." + i3;
							executor.submit(() -> {
								if (isCancelled())
									return;
								InetAddress address;
								try {
									address = InetAddress.getByName(ip);
									if (!address.isReachable(Options.timeoutMs)) {
										return;
									}
									appendLog("Trovato Host:" + address);
									TransportMapping<?> transport = new DefaultUdpTransportMapping();
									Snmp snmp = new Snmp(transport);
									transport.listen();

									CommunityTarget<Address> target = new CommunityTarget<>();
									target.setCommunity(new OctetString(Options.community));
									target.setAddress(GenericAddress.parse("udp:" + ip + "/161"));
									target.setRetries(1);
									target.setTimeout(Options.timeoutMs);
									target.setVersion(SnmpConstants.version2c);

									PDU pdu = new PDU();
									pdu.add(OID_DATA.OID_SYS_DESCR.getVariableBinding());
									pdu.add(OID_DATA.OID_SYS_NAME.getVariableBinding());
									pdu.add(OID_DATA.OID_MAC_ADDRESS.getVariableBinding());
									pdu.add(OID_DATA.OID_SYS_OBJECT_ID.getVariableBinding());
									pdu.add(OID_DATA.OID_SYS_SERIAL.getVariableBinding());
								
									pdu.setType(PDU.GET);
								

									ResponseEvent<Address> response = snmp.send(pdu, target);
									snmp.close();
									if (response == null || response.getResponse() == null) {
										return;
									}
									appendLog("   conferma snmp:" + address);
									if (knownhosts.contains(ip))
										return;

									System.out.println("   ->:" + response.getResponse());
									knownhosts.put(ip, new SNMPHost(ip, response.getResponse()));

									Resolver.resolverThread.interrupt();
								} catch (UnknownHostException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
									return;
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
									return;
								}

								// checkSnmpDevice(ip);
							});
						}

			executor.shutdown();
			executor.awaitTermination(60, TimeUnit.SECONDS);
			outJP.removeAll();
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.fill = GridBagConstraints.BOTH;
			
			for (SNMPHost h : knownhosts.values()) {
				outJP.add(h.getPanel(), gbc);
			}
			outJP.updateUI();
			outJP.repaint();
			Options.save();
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
			appendLog("Scansione della rete completata.");
			stopCrawler();
		}

		@Override
		public void stopCrawler() {
			MFPCrawler.this.stopCrawler();
		}

	}

	/**
	 * Chiusura definitiva del programma e pulizia risorse Tray.
	 */
	private void shutdown() {
		stopCrawler();
		if (systemTray != null && trayIcon != null) {
			systemTray.remove(trayIcon);
		}
		dispose();
		System.exit(0);
	}

	/**
	 * Aggiunge un messaggio al log con timestamp in modo thread-safe.
	 */
	public void appendLog(String message) {
		String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
		SwingUtilities.invokeLater(() -> {
			logArea.append("[" + timestamp + "] " + message + "\n");
			logArea.setCaretPosition(logArea.getDocument().getLength());
		});
	}

	/**
	 * Genera un'icona grafica dinamicamente se non è presente un file immagine
	 * esterno.
	 */
	private Image createFallbackIcon() {
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = image.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(new Color(0, 122, 255));
		g2.fillOval(0, 0, 16, 16);
		g2.setColor(Color.WHITE);
		g2.setFont(new Font("SansSerif", Font.BOLD, 10));
		g2.drawString("M", 3, 12);
		g2.dispose();
		return image;
	}

	static MFPCrawler crawlerWindow = null;

	public static void main(String[] args) {
		// Applica lo stile grafico nativo del sistema operativo
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ignored) {
		}
		Options.load();
		SwingUtilities.invokeLater(() -> {
			crawlerWindow = new MFPCrawler();
			crawlerWindow.setVisible(true);
			Options.startCrawler();
			Resolver.startCrawler();
			
			crawlerWindow.startCrawler();
		});
	}
}
