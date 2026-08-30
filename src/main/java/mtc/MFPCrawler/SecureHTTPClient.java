package mtc.MFPCrawler;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class SecureHTTPClient {

    // Configura l'SSLContext leggendo i file dal JAR
    private static SSLContext createSSLContext(String p12Resource, String p12Password, String caPemResource) throws Exception {
        char[] password = p12Password.toCharArray();

        // 1. Carica il KeyStore (.p12) per mTLS
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = SecureHTTPClient.class.getClassLoader().getResourceAsStream(p12Resource)) {
            if (is == null) {
                throw new IllegalArgumentException("File P12 non trovato nel JAR: " + p12Resource);
            }
            keyStore.load(is, password);
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, password);

        // 2. Carica il TrustStore (.pem) per risolvere l'errore PKIX
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null); 
        try (InputStream is = SecureHTTPClient.class.getClassLoader().getResourceAsStream(caPemResource)) {
            if (is == null) {
                throw new IllegalArgumentException("File CA PEM non trovato nel JAR: " + caPemResource);
            }
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate caCert = (X509Certificate) cf.generateCertificate(is);
            trustStore.setCertificateEntry("server-ca", caCert);
        }
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        // 3. Inizializza l'SSLContext con entrambi i manager
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new java.security.SecureRandom());
        return sslContext;
    }

    public static void main(String[] args) {
        try {
            // 1. Crea l'SSLContext personalizzato
            SSLContext sslContext = createSSLContext(
                "keystore.p12",    // File sotto src/main/resources
                "la_tua_password", // La password impostata con OpenSSL
                "ca.pem"           // File sotto src/main/resources
            );

            // 2. Costruisci l'HttpClient nativo iniettando l'SSLContext
            HttpClient client = HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .build();

            // 3. Prepara la richiesta verso l'endpoint protetto
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://il-tuo-server-protetto.com"))
                    .GET()
                    .build();

            // 4. Invia la richiesta
            System.out.println("Invio della richiesta sicura...");
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // 5. Gestisci la risposta
            System.out.println("Codice di stato: " + response.statusCode());
            System.out.println("Corpo della risposta: " + response.body());

        } catch (Exception e) {
            System.err.println("Errore durante la configurazione SSL o la chiamata HTTP:");
            e.printStackTrace();
        }
    }
}
