package mtc.MFPCrawler;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;

import mtc.MFPCrawler.SNMPHost.MaintenanceRowOIDS;

public enum PrtMarkerSuppliesType {
	OTHER(1, "other", "Altro", true), UNKNOWN(2, "unknown", "Sconosciuto", false), TONER(3, "toner", "Toner", true),
	WASTE_TONER(4, "wasteToner", "Vaschetta Toner Esausto", false), INK(5, "ink", "Inchiostro", true),
	INK_CARTRIDGE(6, "inkCartridge", "Cartuccia Inchiostro", false),
	INK_RIBBON(7, "inkRibbon", "Nastro Inchiostrato", false), WASTE_INK(8, "wasteInk", "Recupero Inchiostro", false),
	OPC(9, "opc", "Tamburo (Drum)", false), DEVELOPER(10, "developer", "Sviluppatore", false),
	FUSER(11, "fuser", "Gruppo Fusore", false), CORONA_WIRE(12, "coronaWire", "Filo Corona", false),
	FUSER_OIL(13, "fuserOil", "Olio Fusore", true), COATED_PAPER(14, "coatedPaper", "Carta Patinata", false),
	MELT_WAX(15, "meltWax", "Cera Solida", true), WASTE_WAX(16, "wasteWax", "Recupero Cera", true),
	FUSER_CLEANER(17, "fuserCleaner", "Pulitore Fusore", false), BELT(18, "belt", "Cinghia Trasferimento", false),
	FUSER_OILER(19, "fuserOiler", "Applicatore Olio", false), SUB_ITEM(20, "subItem", "Sotto-componente", false),
	TRANSFER_UNIT(21, "transferUnit", "Unità Trasferimento", false), STAPLES(22, "staples", "Punti Metallici", true);

	private final int code;
	private final String mibName;
	private final String description;
	private final boolean isReceptacle; // true se il contenitore si riempie anziché svuotarsi

	private static final Map<Integer, PrtMarkerSuppliesType> BY_CODE = new HashMap<>();

	static {
		for (PrtMarkerSuppliesType type : values()) {
			BY_CODE.put(type.code, type);
		}
	}

	PrtMarkerSuppliesType(int code, String mibName, String description, boolean isReceptacle) {
		this.code = code;
		this.mibName = mibName;
		this.description = description;
		this.isReceptacle = isReceptacle;
	}

	public int getCode() {
		return code;
	}

	public String getMibName() {
		return mibName;
	}

	public String getDescription() {
		return description;
	}

	public boolean isReceptacle() {
		return isReceptacle;
	}

	public JLabel getLabel(MaintenanceRowOIDS mr) {
		JLabel jl;
		if (isReceptacle()) {
			jl = new JLabel("0 < " + mr.getResolvedValue() + " < ");
		} else {
			jl = new JLabel("0 >" + mr.getResolvedValue() + " > ");
		}
		return jl;
	}

	/**
	 * Risolve l'enum partendo dal codice numerico SNMP (O(1)).
	 */
	public static PrtMarkerSuppliesType fromCode(int code) {
		return BY_CODE.getOrDefault(code, UNKNOWN);
	}

	/**
	 * Risolve l'enum partendo dalla stringa MIB (es. "wasteToner").
	 */
	public static PrtMarkerSuppliesType fromMibName(String mibName) {
		if (mibName == null)
			return UNKNOWN;
		for (PrtMarkerSuppliesType type : values()) {
			if (type.mibName.equalsIgnoreCase(mibName.trim())) {
				return type;
			}
		}
		return UNKNOWN;
	}
}