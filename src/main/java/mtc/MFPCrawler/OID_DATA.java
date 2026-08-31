package mtc.MFPCrawler;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;

class OID_TONER_DATA extends OID_DATA_BASE {

	public OID_TONER_DATA(String o, String n, String p) {
		super(o, n, p);
		// TODO Auto-generated constructor stub
	}

	public OID_TONER_DATA(Map<String, String> e) {
		super(e);
		// TODO Auto-generated constructor stub
	}

};

class OID_ALERT_DATA extends OID_DATA_BASE {

	public OID_ALERT_DATA(String o, String n, String p) {
		super(o, n, p);
		// TODO Auto-generated constructor stub
	}

	public OID_ALERT_DATA(Map<String, String> e) {
		super(e);
	}

};

public class OID_DATA extends OID_DATA_BASE {

	public final static OID_DATA MTC_OFFLINE = new OID_DATA("mtc.offline", "MTC_OFFLINE", "OFFLINE") {
		@Override
		public int getPriority() {
			// TODO Auto-generated method stub
			return 20;
		}

		@Override
		public JComponent getPanel(SNMPHost host, JPanel jp) {
			Object v = host.values.get(OID);
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets.right = 6;
			gbc.anchor = GridBagConstraints.NORTHWEST;

			gbc.weightx = 0.0;
			gbc.gridwidth = 1;
			gbc.fill = GridBagConstraints.NONE;
			jp.add(title, gbc);

			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			JLabel jl = new JLabel(v.toString());
			if (v.toString().startsWith(SNMPHost.ONLINESTR)) {
				jl.setBackground(Color.green);
			} else {
				jl.setBackground(Color.orange);
			}
			jl.setOpaque(true);
			jp.add(jl, gbc);
			return null;
		};
	};
	public final static OID_DATA OID_MAC_ADDRESS = new OID_DATA("1.3.6.1.2.1.2.2.1.6.1", "SYS_MAC_ADDR", "MAC_Address");
	public final static OID_DATA OID_SYS_DESCR = new OID_DATA("1.3.6.1.2.1.1.1.0", "OID_SYS_DESCR", "Description") {
		@Override
		public int getPriority() {
			// TODO Auto-generated method stub
			return -220;
		}

		@Override
		public JComponent getPanel(SNMPHost host, JPanel jp) {
			Object v = host.values.get(OID);
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets.right = 6;
			gbc.anchor = GridBagConstraints.NORTHWEST;

			gbc.weightx = 0.0;
			gbc.gridwidth = 1;
			gbc.fill = GridBagConstraints.NONE;
			jp.add(title, gbc);

			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			JTextArea labelLikeArea = new JTextArea(v.toString());
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
			jp.add(labelLikeArea, gbc);
			return null;
		};
	};
	public final static OID_DATA OID_SYS_NAME = new OID_DATA("1.3.6.1.2.1.1.5.0", "OID_SYS_NAME", "Host Name");
	public final static OID_DATA OID_SYS_SERIAL = new OID_DATA("1.3.6.1.2.1.43.5.1.1.17.1", "sysSerial", "Serial");

	public final static OID_DATA OID_SYS_OBJECT_ID = new OID_DATA("1.3.6.1.2.1.1.2.0", "sysObjectID", "Vendor Name");
//	public final static OID_DATA OID_SYS_SERIAL = new OID_DATA("1.3.6.1.2.1.43.5.1.1.17.1", "sysSerial", "Serial");
	// private static final String OID_SYS_SERIAL = "1.3.6.1.2.1.43.5.1.1.17.1";//
	// "1.3.6.1.2.1.43.5.1.1.17"; // serial

	static {
		for (Field field : OID_DATA.class.getDeclaredFields()) {
			if (field.getType() == OID_DATA.class) {

				try {
					OID_DATA d = (OID_DATA) field.get(null);
					OIDS.put(d.OID, d);
					System.out.println(field.getType().getSimpleName() + " " + field.getName() + " " + d.OID);
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

	protected OID_DATA(Map<String, String> entry) {
		super(entry);
	}

	protected OID_DATA(String o, String n, String p, int i, boolean f) {
		super(o, n, p, i, f);
	}

	protected OID_DATA(String o, String n, String p) {
		super(o, n, p);
	}
}

class OID_DATA_BASE {
	public static final ConcurrentHashMap<String, OID_DATA_BASE> OIDS = new ConcurrentHashMap<String, OID_DATA_BASE>();

	public static OID_DATA_BASE getByName(String str) {
		for (OID_DATA_BASE o : OIDS.values()) {
			if (o.name.equals(str)) {
				return o;
			}
		}
		return null;
	}

	public static OID_DATA_BASE getByOID(String str) {
		while (str.length() > 3) {
			for (OID_DATA_BASE o : OIDS.values()) {
				if (o.OID.equals(str)) {
					return o;
				}
			}
			str = str.substring(0,str.lastIndexOf("."));
		}
		return null;
	}

	String OID;
	String pretty = null;
	String name = null;

	private int priority = -1;
	private boolean isForced = false;

	public int getPriority() {
		return priority;
	};

	protected OID_DATA_BASE(Map<String, String> entry) {
		OID = entry.get("oid");
		pretty = entry.get("descrizione");
		name = entry.get("short");
		String prio = entry.get("priority");
		String m = entry.get("mode");
		priority = Integer.parseInt(prio);
		isForced = m.equals("1");
		title = new JLabel(pretty);
	}

	protected OID_DATA_BASE(String o, String n, String p) {
		OID = o;
		name = n;
		pretty = p;
		title = new JLabel(p);
	}

	protected OID_DATA_BASE(String o, String n, String p, int i, boolean f) {
		OID = o;
		name = n;
		pretty = p;
		title = new JLabel(p);
		priority = i;
		isForced = f;
	}

	JPanel jp = new JPanel();
	GridBagLayout gbl = new GridBagLayout();
	JLabel title;

	public VariableBinding getVariableBinding() {
		return new VariableBinding(new OID(OID));
	}

	public JComponent getPanel(SNMPHost host, JPanel jp) {
		Object v = host.values.get(OID);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets.right = 6;
		gbc.anchor = GridBagConstraints.WEST;

		// jp.removeAll();
		// jp.setLayout(gbl);
		gbc.weightx = 0.0;
		gbc.gridwidth = 1;
		gbc.fill = GridBagConstraints.NONE;
		jp.add(title, gbc);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		JLabel jl = new JLabel(v.toString());

		jp.add(jl, gbc);
		return null;
	}

	public boolean isForced() {
		return isForced;
	}
}