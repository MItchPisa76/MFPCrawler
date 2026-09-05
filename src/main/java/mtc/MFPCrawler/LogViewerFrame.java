package mtc.MFPCrawler;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.RandomAccessFile;

public class LogViewerFrame extends JFrame {

    private final JTextArea logArea;

    public LogViewerFrame(File logFile) {
        super("Log Viewer - " + logFile.getName());

        // Setup dell'interfaccia
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBackground(Color.BLACK);
        logArea.setForeground(Color.GREEN);

        JScrollPane scrollPane = new JScrollPane(logArea);
        add(scrollPane, BorderLayout.CENTER);

        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Avvio del thread in background per il monitoraggio del file
        startLogTailer(logFile);
    }

    private void startLogTailer(File file) {
        Thread tailThread = new Thread(() -> {
            try (RandomAccessFile reader = new RandomAccessFile(file, "r")) {
                long filePointer = file.length();
                reader.seek(filePointer);

                while (!Thread.currentThread().isInterrupted()) {
                    long fileLength = file.length();

                    // Gestione del reset/rotazione del file
                    if (fileLength < filePointer) {
                        filePointer = 0;
                        reader.seek(filePointer);
                    }

                    if (fileLength > filePointer) {
                        reader.seek(filePointer);
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String currentLine = line; // Variabile finale per la lambda
                            
                            // Aggiorna l'interfaccia nel thread grafico di Swing
                            SwingUtilities.invokeLater(() -> {
                                logArea.append(currentLine + "\n");
                                // Auto-scroll fino all'ultima riga aggiunta
                                logArea.setCaretPosition(logArea.getDocument().getLength());
                            });
                        }
                        filePointer = reader.getFilePointer();
                    }

                    Thread.sleep(1000); // Pausa di 1 secondo prima del controllo successivo
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> 
                    logArea.append("\n[ERRORE] Impossibile leggere il file: " + e.getMessage())
                );
            }
        });

        tailThread.setDaemon(true); // Si chiude automaticamente alla chiusura dell'app
        tailThread.start();
    }

  
}