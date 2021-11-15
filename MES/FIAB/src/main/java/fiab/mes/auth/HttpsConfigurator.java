package fiab.mes.auth;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
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

import com.google.gson.Gson;

import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectionContext;
import akka.http.javadsl.HttpsConnectionContext;

public class HttpsConfigurator {
	private static Gson gson = new Gson();
	private static Credentials cred;
	public static HttpsConnectionContext useHttps(ActorSystem system) {
		HttpsConnectionContext https = null;
	    try {
			cred = gson.fromJson(new FileReader("keystore.json"), Credentials.class);
				
		    final KeyStore ks = KeyStore.getInstance("PKCS12");
		    ks.load(new FileInputStream("keystore.jks"), cred.getPassword());
	
		    final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
		    keyManagerFactory.init(ks, cred.getPassword());
	
		    final TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
		    tmf.init(ks);
	
		    final SSLContext sslContext = SSLContext.getInstance("TLS");
		    sslContext.init(keyManagerFactory.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
	
		    https = ConnectionContext.https(sslContext);

	    } catch (NoSuchAlgorithmException | KeyManagementException e) {
	    	system.log().error("Exception while configuring HTTPS.", e);
	    } catch (CertificateException | KeyStoreException | UnrecoverableKeyException | IOException e) {
	    	system.log().error("Exception while ", e);
	    } catch(Exception e) {
	    	e.printStackTrace();
	    }

	    return https;
	}
	
	public static class Credentials {
		private char[] password;
		private Credentials(char[] password) {
			this.password = password;
		}
		private char[] getPassword() {
			return password;
		}
	}
}
