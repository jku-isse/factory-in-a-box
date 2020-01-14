package fiab.mes.auth;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectionContext;
import akka.http.javadsl.HttpsConnectionContext;

public class HttpsConfigurator {

	public static HttpsConnectionContext useHttps(ActorSystem system) {
		HttpsConnectionContext https = null;
	    try {
	      // initialise the keystore
	      // !!! never put passwords into code !!!
	      final char[] password = new char[]{'p', 'a', 's', 's', 'w', 'o', 'r', 'd'};

//	      final KeyStore ks = KeyStore.getInstance("PKCS12");
//	      final InputStream keystore = SimpleServerApp.class.getClassLoader().getResourceAsStream("./keystore.jks");
//	      if (keystore == null) {
//	        throw new RuntimeException("Keystore required!");
//	      }
//	      ks.load(keystore, password);
	      
	      final KeyStore ks = KeyStore.getInstance("PKCS12");
	      ks.load(new FileInputStream("keystore.jks"), password);

	      final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
	      keyManagerFactory.init(ks, password);

	      final TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
	      tmf.init(ks);

	      final SSLContext sslContext = SSLContext.getInstance("TLS");
	      sslContext.init(keyManagerFactory.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());

	      https = ConnectionContext.https(sslContext);

	    } catch (NoSuchAlgorithmException | KeyManagementException e) {
	      system.log().error("Exception while configuring HTTPS.", e);
	    } catch (CertificateException | KeyStoreException | UnrecoverableKeyException | IOException e) {
	      system.log().error("Exception while ", e);
	    }

	    return https;
	}
}
