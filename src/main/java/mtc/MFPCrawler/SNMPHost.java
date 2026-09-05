package mtc.MFPCrawler;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.Null;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

public class SNMPHost {
	String ipV4address;
	ConcurrentHashMap<String, Variable> values = new ConcurrentHashMap<String, Variable>();

	JPanel jp;
	JTabbedPane jtp;

	public SNMPHost(String a, PDU pdu) {
		ipV4address = a;
		updateValues(pdu);
		GridBagLayout gbl = new GridBagLayout();
		jp = new JPanel(gbl);

	}

	public void updateValues(PDU pdu) {
		for (VariableBinding aa : pdu.getVariableBindings()) {
			String oo = aa.getOid().toString();
			OID_DATA_BASE oid = OID_DATA.OIDS.get(oo);
			if (oid != null) {
				values.put(oid.OID, aa.getVariable());
				appendLog("   -->:" + oid.OID + ":" + oid.pretty + ":" + aa.getVariable().toString());
			} else {
				values.put(oo, aa.getVariable());
				appendLog("   ->:" + oo + ":" + aa.getVariable().toString());
			}

		}
	}

	public void updateMaintenance(PDU pdu) {
		for (VariableBinding aa : pdu.getVariableBindings()) {
			try {
				String oid = aa.getOid().toString();
				String ind = oid.substring(oid.lastIndexOf(".") + 1);
				Integer ii = Integer.parseInt(ind);
				MaintenanceRowOIDS mr = lm.get(ii);
				if (mr == null) {
					mr = new MaintenanceRowOIDS(ii);
					lm.put(ii, mr);
				}

				mr.registerOID(oid.substring(0, oid.lastIndexOf(".")), aa.getVariable());
				values.put(oid, aa.getVariable());
			} catch (Exception e) {
				e.printStackTrace();
				// TODO: handle exception
			}
			// appendLog(" M>:" + oid + ":" + aa.getVariable().toString());

		}
		lastUpdatedMantained = new Date();
	}

	public void updateAlert(PDU pdu) {
		for (VariableBinding aa : pdu.getVariableBindings()) {
			try {
				String oid = aa.getOid().toString();
				String ind = oid.substring(oid.lastIndexOf(".") + 1);
				Integer ii = Integer.parseInt(ind);
				AlertRowOIDS al = la.get(ii);
				if (al == null) {
					al = new AlertRowOIDS(ii);
					la.put(ii, al);
				}

				al.registerOID(oid.substring(0, oid.lastIndexOf(".")), aa.getVariable());
				values.put(oid, aa.getVariable());
			} catch (Exception e) {
				e.printStackTrace();
				// TODO: handle exception
			}
			// appendLog(" M>:" + oid + ":" + aa.getVariable().toString());

		}
		lastUpdatedMantained = new Date();
	}

	public Date lastUpdatedMantained;
	Hashtable<Integer, MaintenanceRowOIDS> lm = new Hashtable<Integer, MaintenanceRowOIDS>();
	Hashtable<Integer, AlertRowOIDS> la = new Hashtable<Integer, AlertRowOIDS>();
	// Hashtable<Integer, CounterRowOIDS> ca = new Hashtable<Integer,
	// CounterRowOIDS>();

	class AlertRowOIDS {
		int index = -1;
		public String description;
		public String severity;
		public String time;

		AlertRowOIDS(int i) {
			index = i;
		}

		enum prtMarkerAlert {
			prtAlertDescription("description"), prtAlertTime("time"), prtAlertSeverityLevel("severity");

			String OID;
			private String field;

			prtMarkerAlert(String f) {
				OID = OID_DATA.getByName(name()).OID;
				field = f;
			}

			public Field getField() {
				try {
					return AlertRowOIDS.class.getField(this.field);
				} catch (NoSuchFieldException e) {
					throw new RuntimeException("Campo non trovato in MaintenanceRowOIDS: " + field, e);
				}
			}
		}

		public void registerOID(String oid, Variable v) {
			for (prtMarkerAlert p : prtMarkerAlert.values()) {
				if (p.OID.equals(oid)) {
					try {
						p.getField().set(this, "" + v.toString());
						appendLog("    A[" + index + "->" + oid + ":" + p.getField().getName() + "=" + v.toString());
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}

		String getResolvedName() {
			return description;
		}

		String getResolvedSeverity() {
			return severity;
		}

		String getResolvedTime() {
			return time;
		}
	}

	public Hashtable<String, String> typeRemapper = new Hashtable<String, String>();

	class MaintenanceRowOIDS {
		int index = -1;
		public String name;
		public String units;
		public String unitsS1;
		public String unitsS2;
		public String value;
		public String max;
		public String typeI;
		public String type;

		MaintenanceRowOIDS(int i) {
			index = i;
		}

		public enum prtMarkerSupplies {
			prtMarkerSuppliesDescription("name"), prtMarkerSuppliesLevel("value"), prtMarkerSuppliesSupplyUnit("units"),
			prtMarkerSuppliesMaxCapacity("max"), prtMarkerSuppliesType("typeI"),;

			String OID;
			private String field;

			prtMarkerSupplies(String f) {
				OID = OID_DATA.getByName(name()).OID;
				field = f;
			}

			public Field getField() {
				try {
					return MaintenanceRowOIDS.class.getField(this.field);
				} catch (NoSuchFieldException e) {
					throw new RuntimeException("Campo non trovato in MaintenanceRowOIDS: " + field, e);
				}
			}
		}

		public void registerOID(String oid, Variable v) {

			for (prtMarkerSupplies p : prtMarkerSupplies.values()) {
				if (p.OID.equals(oid)) {
					try {
						p.getField().set(this, "" + v.toString());
						appendLog("    M[" + index + "->" + oid + ":" + p.getField().getName() + "=" + v.toString());
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			if ((units != null) && (unitsS1 == null)) {
				Integer ii = units == null ? 0 : Integer.parseInt(units);
				TupleString ts = Resolver.ht.getOrDefault(ii, null);
				unitsS1 = ts.s1;
				unitsS2 = ts.s2;
			}
			if ((typeI != null) && (type == null)) {
				String rem = typeRemapper.get(typeI);
				if (rem != null) {
					typeI = rem;
				}
				type = PrtMarkerSuppliesType.fromCode(Integer.parseInt(typeI)).getDescription();
			}
		}

		String getResolvedName() {
			return name;
		}

		String getResolvedUnitsS1() {
			return unitsS1;
		}

		String getResolvedUnitsS2() {
			return unitsS2;
		}

		String getResolvedValue() {
			return value;
		}

		String getResolvedMax() {
			return max;
		}

		PrtMarkerSuppliesType getResolvedType() {
			return PrtMarkerSuppliesType.fromCode(Integer.parseInt(typeI));
		}
	}

	public JPanel getPanel() {
		Variable v = values.get(OID_DATA.OID_SYS_SERIAL.OID);
		List<String> sortOids = new ArrayList<String>(values.keySet());
		sortOids.sort(new Comparator<String>() {
			public int compare(String s2, String s1) {
				OID_DATA_BASE o1 = OID_DATA.OIDS.get(s1);
				OID_DATA_BASE o2 = OID_DATA.OIDS.get(s2);
				if (o1 == o2)
					return 0;
				if (o1 == null)
					return -1;
				if (o2 == null)
					return 1;
				return o1.getPriority() - o2.getPriority();
			};
		});

		if (v == null) {
			jp.setBorder(new TitledBorder(ipV4address));
		} else {
			OID_DATA_BASE o = OID_DATA.getByName("secSysModelName");
			Variable vv = values.get(o != null ? o.OID : "");
			TitledBorder tb = new TitledBorder(v.toString() + " " + (vv != null ? vv.toString() : ""));
			Variable var = values.get(OID_DATA.MTC_OFFLINE.OID);
			if (var != null) {
				if (var.toString().startsWith(ONLINESTR)) {
					tb.setTitleColor(Color.green.darker().darker());
				} else {
					tb.setTitleColor(Color.orange);
				}
			}
			jp.setBorder(tb);
		}

		GridBagConstraints gbc = new GridBagConstraints();
		jp.removeAll();
		if (jtp == null) {
			jtp = new JTabbedPane();
			GridBagLayout gbl = new GridBagLayout();
			JPanel jp = new JPanel(gbl);
			jtp.add("Info", jp);
			gbl = new GridBagLayout();
			jp = new JPanel(gbl);
			jtp.add("Consumabili", jp);
			gbl = new GridBagLayout();
			jp = new JPanel(gbl);
			jtp.add("Alert", jp);
			jp = new JPanel(gbl);
			jtp.add("Counters", jp);
		}

		jp.add(jtp, gbc);

		JPanel jp_description = (JPanel) jtp.getComponent(0);
		jp_description.removeAll();

		gbc = new GridBagConstraints();
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.WEST;

		for (String oid : sortOids) {

			OID_DATA_BASE o = OID_DATA.OIDS.get(oid);
			if (!(o instanceof OID_DATA))
				continue;
			JComponent jc = o.getPanel(this, jp_description);
			if (jc != null)
				jp_description.add(jc, gbc);
		}

		JPanel jp_consumabili = (JPanel) jtp.getComponent(1);
		jp_consumabili.removeAll();

		Collection<MaintenanceRowOIDS> mroid = lm.values();
		List<MaintenanceRowOIDS> sortLL = new ArrayList<MaintenanceRowOIDS>(mroid);

		sortLL.sort(new Comparator<MaintenanceRowOIDS>() {
			@Override
			public int compare(MaintenanceRowOIDS o1, MaintenanceRowOIDS o2) {
				int typecode1 = o1.getResolvedType().getCode();
				int typecode2 = o2.getResolvedType().getCode();

				if (typecode1 == 3)
					return -1;
				if (typecode2 == 3)
					return 1;
				if (typecode1 == 11)
					return -1;
				if (typecode2 == 11)
					return 1;
				return 0;
			}
		});

		gbc = new GridBagConstraints();
		gbc.insets.right = 6;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.weighty = 0;
		for (MaintenanceRowOIDS mr : sortLL) {
			gbc.weightx = 0.0;
			gbc.gridwidth = 1;
			gbc.fill = GridBagConstraints.NONE;
			JLabel jl = new JLabel(mr.getResolvedName());
			jl.setToolTipText(mr.getResolvedType().getDescription());
			jp_consumabili.add(jl, gbc);

			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 1.0;
			jl = mr.getResolvedType().getLabel(mr);

			jp_consumabili.add(jl, gbc);
			gbc.weightx = 0.0;
			gbc.fill = GridBagConstraints.NONE;
			jl = new JLabel(mr.getResolvedMax());
			jp_consumabili.add(jl, gbc);
			gbc.weightx = 0.0;
			gbc.fill = GridBagConstraints.NONE;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			String str = mr.getResolvedUnitsS1();
			String str2 = mr.getResolvedUnitsS2();
			// Integer ii = str == null ? 0 : Integer.parseInt(str);
			// TupleString ts = Resolver.ht.getOrDefault(ii, null);
			jl = new JLabel(str);
			jl.setToolTipText(str2);

			jp_consumabili.add(jl, gbc);
		}
		gbc.weighty = 1;
		jp_consumabili.add(new JPanel(), gbc);
		JPanel jp_alert = (JPanel) jtp.getComponent(2);
		jp_alert.removeAll();
		Collection<AlertRowOIDS> aloid = la.values();
		List<AlertRowOIDS> sortAlert = new ArrayList<AlertRowOIDS>(aloid);
		gbc = new GridBagConstraints();
		gbc.insets.right = 6;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.weighty = 1;
		for (AlertRowOIDS al : sortAlert) {
			gbc.weightx = 0.0;
			gbc.gridwidth = 1;
			gbc.fill = GridBagConstraints.NONE;
			JLabel jl = new JLabel(al.getResolvedSeverity());

			jp_alert.add(jl, gbc);

			gbc.fill = GridBagConstraints.NONE;

			jl = new JLabel(al.getResolvedTime());
			jp_alert.add(jl, gbc);
			gbc.weightx = 1.0;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			JTextArea labelLikeArea = new JTextArea(al.getResolvedName());
			labelLikeArea.setLineWrap(true);
			labelLikeArea.setWrapStyleWord(true); // Spezza
													// sulle
													// parole,
													// non
													// a
													// metà
													// parola

			// 2. Rendi la JTextArea identica a una JLabel
			labelLikeArea.setEditable(false);
			labelLikeArea.setFocusable(false);
			labelLikeArea.setOpaque(false); // Sfondo
											// trasparente
			labelLikeArea.setFont(UIManager.getFont("Label.font")); // Stesso font delle label di sistema
			labelLikeArea.setBorder(null);

			jp_alert.add(labelLikeArea, gbc);
		}

		JPanel jp_result = (JPanel) jtp.getComponent(3);
		jp_result.removeAll();

		jp.repaint();
		return jp;
	}

	static final String ONLINESTR = "ONLINE:";
	static final String OFFLINESTR = "OFFLINE:";

	protected void appendLog(String string) {
		// TODO Auto-generated method stub
		MFPCrawler.crawlerWindow.appendLog(string);
	}

	public synchronized void sendSNMPQuery(String OIDreq) {

		InetAddress address;
		try {
			address = InetAddress.getByName(ipV4address);
			if (!address.isReachable(Options.timeoutMs)) {

				return;
			}

			appendLog("sendSNMPQuery to:" + address + " OID:" + OIDreq);
			TransportMapping<?> transport = new DefaultUdpTransportMapping();
			Snmp snmp = new Snmp(transport);
			transport.listen();

			CommunityTarget<Address> target = new CommunityTarget<>();
			target.setCommunity(new OctetString(Options.community));
			target.setAddress(GenericAddress.parse("udp:" + ipV4address + "/161"));
			target.setRetries(1);
			target.setTimeout(Options.timeoutMs);
			target.setVersion(SnmpConstants.version2c);

			PDU pdu = new PDU();

			pdu.add(new VariableBinding(new OID(OIDreq)));
			pdu.setType(PDU.GETBULK);
			pdu.setNonRepeaters(0); // 0 = Tutti gli OID nella lista sono tabellari
			pdu.setMaxRepetitions(5000);

			ResponseEvent<Address> response = snmp.send(pdu, target);

			if (response == null || response.getResponse() == null) {

			} else {
				Map<String, String> map = new HashMap<String, String>();
				for (VariableBinding aa : response.getResponse().getVariableBindings()) {
					String oo = aa.getOid().toString();
					map.put(oo, aa.toValueString());
					System.out.println(oo + ">" + aa.toValueString());
				}

				String json = Resolver.mapper.writeValueAsString(map);
				HttpClient client = Options.getHttpClient();
				String url = Options.getResolveServer() + "/snmpquery";

				Builder builder = HttpRequest.newBuilder().uri(URI.create(url))
						.header("Content-Type", "application/json").header("token", Options.token).header("OID", OIDreq)
						.header("ipv4", ipV4address);

				HttpRequest request = builder.POST(BodyPublishers.ofString(json)).build();

				HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
				if (resp.statusCode() == 200) {
					appendLog("    <:" + address + " OID:" + OIDreq + "");
				} else {
					Resolver.onError(resp.body() + ":" + resp.statusCode(), Color.orange);
					System.err.println("    error:" + resp.body() + ":" + resp.statusCode());
				}
			}

		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
	}

	public synchronized Void update() {
		InetAddress address;
		try {
			address = InetAddress.getByName(ipV4address);
			if (!address.isReachable(Options.timeoutMs)) {
				appendLog(" Host offLine:" + address);
				String str = values.get(OID_DATA.MTC_OFFLINE.OID).toString();// , new
																				// OctetString("ONLINE:"+format));
				if (str.startsWith(ONLINESTR)) {
					str = str.replace(ONLINESTR, OFFLINESTR);
				}
				values.put(OID_DATA.MTC_OFFLINE.OID, new OctetString(str));

				return null;
			}
			Locale loc = Locale.getDefault();
			LocalDateTime now = LocalDateTime.now();

			// Formatter in Italiano
			DateTimeFormatter itaFormatter = DateTimeFormatter.ofPattern("EEE d MMMM yyyy, HH:mm", loc);
			final String format = now.format(itaFormatter).toString();
			values.put(OID_DATA.MTC_OFFLINE.OID, new OctetString(ONLINESTR + format));

			appendLog("Richiesta info:" + address);
			TransportMapping<?> transport = new DefaultUdpTransportMapping();
			Snmp snmp = new Snmp(transport);
			transport.listen();

			CommunityTarget<Address> target = new CommunityTarget<>();
			target.setCommunity(new OctetString(Options.community));
			target.setAddress(GenericAddress.parse("udp:" + ipV4address + "/161"));
			target.setRetries(1);
			target.setTimeout(Options.timeoutMs);
			target.setVersion(SnmpConstants.version2c);

			PDU pdu = new PDU();
			for (String oid : values.keySet()) {
				OID_DATA_BASE o = OID_DATA_BASE.OIDS.get(oid);
				if ((o instanceof OID_DATA) && (o.isForced() || (values.get(o.OID) == Null.noSuchInstance))) {
					pdu.add(o.getVariableBinding());
				}

			}
			pdu.setType(PDU.GET);

			ResponseEvent<Address> response = snmp.send(pdu, target);

			if (response == null || response.getResponse() == null) {

			} else
				updateValues(response.getResponse());

			pdu.clear();
			for (String oid : values.keySet()) {
				try {
					OID_DATA_BASE o = OID_DATA_BASE.OIDS.get(oid);
					if ((o instanceof OID_TONER_DATA) && (o.isForced() || (values.get(o.OID) == Null.noSuchInstance))) {
						pdu.add(o.getVariableBinding());
					}

				} catch (Exception e) {
					e.printStackTrace();
					// TODO: handle exception
				}
			}
			pdu.setType(PDU.GETBULK);
			pdu.setNonRepeaters(0); // 0 = Tutti gli OID nella lista sono tabellari
			pdu.setMaxRepetitions(20);
			response = snmp.send(pdu, target);

			if (response == null || response.getResponse() == null) {

			} else {
				updateMaintenance(response.getResponse());
				// updateAlert(response.getResponse());
			}

			pdu = new PDU();
			for (String oid : values.keySet()) {
				try {
					OID_DATA_BASE o = OID_DATA_BASE.OIDS.get(oid);
					if ((o instanceof OID_ALERT_DATA) && (o.isForced() || (values.get(o.OID) == Null.noSuchInstance))) {
						pdu.add(o.getVariableBinding());
					}

				} catch (Exception e) {
					e.printStackTrace();
					// TODO: handle exception
				}
			}
			pdu.setType(PDU.GETBULK);
			pdu.setNonRepeaters(0); // 0 = Tutti gli OID nella lista sono tabellari
			pdu.setMaxRepetitions(20);
			response = snmp.send(pdu, target);

			if (response == null || response.getResponse() == null) {

			} else {
				updateAlert(response.getResponse());
			}
			Variable serial = values.get(OID_DATA.OID_SYS_SERIAL.OID);
			if (serial != null) {

				HttpClient client = Options.getHttpClient();
				String url = Options.getResolveServer() + "/mfp";
				HashMap<String, Object> map = new HashMap<String, Object>();
				HashMap<String, String> mapI = new HashMap<String, String>();
				HashMap<String, String> mapM = new HashMap<String, String>();
				HashMap<String, String> mapA = new HashMap<String, String>();
				for (String v : values.keySet()) {
					OID_DATA_BASE o = OID_DATA_BASE.getByOID(v);
					if (o instanceof OID_DATA)
						mapI.put(v, values.get(v).toString());
					if (o instanceof OID_TONER_DATA)
						mapM.put(v, values.get(v).toString());
					if (o instanceof OID_ALERT_DATA)
						mapA.put(v, values.get(v).toString());
				}

				map.put("info", mapI);
				map.put("maintenace", lm);
				map.put("alerts", la);
				map.put("ipv4", ipV4address);

				String jsonPayload = Options.mapper.writeValueAsString(map);
				Builder builder = HttpRequest.newBuilder().uri(URI.create(url))
						.header("Content-Type", "application/json").header("token", Options.token)
						.header("serial", serial.toString());

				HttpRequest request = builder.POST(BodyPublishers.ofString(jsonPayload)).build();

				client.send(request, HttpResponse.BodyHandlers.ofString());
			}
			// appendLog(" ->:" + response.getResponse());
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		getPanel();

		return null;

	}
}