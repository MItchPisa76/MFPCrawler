## 📥 Storico Versioni / Downloads

keytool -list -keystore "$JAVA_HOME/lib/security/cacerts" -storepass changeit See whether the server's CA or root CA is present.
//keytool -importcert -alias nodejs-server -file cert.pem -keystore "$JAVA_HOME/lib/security/cacerts" -storepass changeit -noprompt




openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -sha256 -days 365 -nodes \
  -subj '/CN=192.168.2.210' \
  -addext "subjectAltName=IP:192.168.2.210"
  
| Versione | Data Rilascio | Installer Windows (.exe) |
| :--- | :--- | :--- |
<!-- DOWNLOAD_TABLE_MARKER -->
| **v1.0.0-manual** | 2026-09-05 14:00 | [⚡ Scarica MFPCrawler-1.0.0.exe](https://github.com/MitchPisa76/MFPCrawler/releases/download/v1.0.0-manual/MFPCrawler-1.0.0.exe) |

