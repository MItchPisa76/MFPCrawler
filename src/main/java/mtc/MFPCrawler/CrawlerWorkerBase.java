package mtc.MFPCrawler;

import java.util.List;

import javax.swing.SwingWorker;

/**
 * Applicazione MFPCrawler con interfaccia Swing ed esecuzione in background
 * integrata nella barra di sistema (System Tray).
 */
/**
 * Classe interna per la gestione asincrona delle scansioni del crawler
 * (SwingWorker). Sostituisci il ciclo doInBackground con la tua reale logica di
 * crawling SNMP/HTTP per MFP.
 */

public abstract class CrawlerWorkerBase extends SwingWorker<Void, String> {

	

	@Override
	protected Void doInBackground() throws Exception {
		int scanCycle = 1;
		while (!isCancelled()) {
			publish("Inizio ciclo di scansione #" + scanCycle);

			// SIMULAZIONE LOGICA DI CRAWLING MFP (es. scansione IP stampanti, SNMP polling)
			Thread.sleep(30);

			if (isCancelled())
				break;
			publish("Completato ciclo #" + scanCycle + " - 0 errori riscontrati.");
			scanCycle++;

			// Intervallo tra le scansioni
			Thread.sleep(4);
		}
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
		appendLog("Ciclo di crawling arrestato.");
		stopCrawler();
	}

	protected final void appendLog(String string) {
		MFPCrawler.crawlerWindow.appendLog(string);
	}

	public abstract void stopCrawler();
}