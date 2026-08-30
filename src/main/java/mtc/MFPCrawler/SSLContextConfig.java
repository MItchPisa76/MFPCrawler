package mtc.MFPCrawler;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class SSLContextConfig {

    public static SSLContext configureSSL(String p12Path, String p12Password, String caPemPath) throws Exception {
        char[] password = p12Password.toCharArray();

        // 1. CONFIGURA IL KEYMANAGER (La tua chiave privata key.pem + cert.pem convertiti in P12)
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(p12Path)) {
            keyStore.load(fis, password);
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, password);

        // 2. CONFIGURA IL TRUSTMANAGER (Risolve l'errore PKIX leggendo il certificato ca.pem / cert.pem)
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null); // Inizializza un truststore vuoto

        try (FileInputStream fis = new FileInputStream(caPemPath)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate caCert = (X509Certificate) cf.generateCertificate(fis);
            // Aggiunge il certificato fidato al truststore temporaneo
            trustStore.setCertificateEntry("server-ca", caCert);
        }
        
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        // 3. INIZIALIZZA L'SSLCONTEXT
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new java.security.SecureRandom());

        return sslContext;
    }
}