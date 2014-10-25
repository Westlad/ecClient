/**
 * 
 */
package com.duncanwestland.ec.client.live;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.ProxySelector;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.logging.Logger;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.ProxySelectorRoutePlanner;
import org.apache.http.message.BasicHeader;
import org.bouncycastle.util.encoders.Base64;

import com.google.gdata.util.AuthenticationException;

/**
 * @author Duncan Westland
 * @version 1.0.0 
 * 
 * This class handles the authentication of a user and the uploading of biodata 
 * to a J2EE compliant server
 *
 */
/**
 * @author duncan
 *
 */
public class Uploader {
	Logger log = Logger.getLogger(Uploader.class.getName());
	/**
	 * Name of Keystore that contains certificates that are trusted to make an https
	 * connection to the server
	 */
	private String httpsCerts;
	/**
	 * https certificates Keystore password
	 */
	private String httpsCertsPassword;
	/**
	 * The URL of the Servlet responsible for processing an upload of chip check data
	 */
	private URL serviceUrl;
	/**
	 * Header that contains authentication data (base64 encoded username and password).
	 */
	private Header authHeader;
    /**
     * The email address of the client, used to authenticate the upload
     */
    private String username;
    /**
     * The client's password
     */
    private String password;
    /**
     * The URL of the proxy server ( only set if a proxy is used by the client)
     */
    private String proxyHost;
    /**
     * The port of the proxy server (if a proxy is used by the client)
     */
    private String proxyPort;

	public String getHttpsCertsPassword() {
		return httpsCertsPassword;
	}
	public void setHttpsCertsPassword(String httpsCertsPassword) {
		this.httpsCertsPassword = httpsCertsPassword;
	}
	public String getHttpsCerts() {
		return httpsCerts;
	}
	public void setHttpsCerts(String httpsCerts) {
		this.httpsCerts = httpsCerts;
	}
	public String getProxyHost() {
		return proxyHost;
	}
	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}
	public String getProxyPort() {
		return proxyPort;
	}
	public void setProxyPort(String proxyPort) {
		this.proxyPort = proxyPort;
	}
	public URL getServiceUrl() {
		return serviceUrl;
	}
	public void setServiceUrl(String serviceUrl) throws MalformedURLException {
		this.serviceUrl = new URL(serviceUrl);
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	
	/**
	 * Uploads the bytes in dataPackage.
	 * For this to work, the relevant fields in this class must have been set and
	 * authentication has to have completed successfully, otherwise an error is thrown.
	 * @param dataPackage byte array to be uploaded
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws AuthenticationException
	 * @throws KeyStoreException 
	 * @throws CertificateException 
	 * @throws NoSuchAlgorithmException 
	 * @throws UnrecoverableKeyException 
	 * @throws KeyManagementException 
	 */
	public void upload(byte[] dataPackage) throws ClientProtocolException, IOException, AuthenticationException, KeyStoreException, NoSuchAlgorithmException, CertificateException, KeyManagementException, UnrecoverableKeyException {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		//load the trust source for https comms and set up https
        KeyStore trustStore  = KeyStore.getInstance(KeyStore.getDefaultType());        
        InputStream instream = this.getClass().getResourceAsStream(httpsCerts); 
        try {
            trustStore.load(instream, httpsCertsPassword.toCharArray());
        } finally {
            instream.close();
        }
        
        SSLSocketFactory socketFactory = new SSLSocketFactory(trustStore);
        int port = serviceUrl.getPort();
        if (port==-1) port = serviceUrl.getDefaultPort();
        @SuppressWarnings("deprecation")
		Scheme sch = new Scheme("https", socketFactory, port);
        httpclient.getConnectionManager().getSchemeRegistry().register(sch);
       
		//proxy settings
        System.setProperty("http.proxyHost", proxyHost);
        System.setProperty("http.proxyPort", proxyPort);
        System.setProperty("https.proxyHost", proxyHost);
        System.setProperty("https.proxyPort", proxyPort);
        log.info("https.proxyHost = "+System.getProperty("https.proxyHost"));
        log.info("https.proxyPort = "+System.getProperty("https.proxyPort"));
        
		ProxySelectorRoutePlanner routePlanner = new ProxySelectorRoutePlanner(
		        httpclient.getConnectionManager().getSchemeRegistry(),
		        ProxySelector.getDefault());  
		httpclient.setRoutePlanner(routePlanner);
		//
		HttpPost post = new HttpPost(serviceUrl.toString());
		// set authcookie returned by login service
		if (authHeader != null) {post.addHeader(authHeader);}
   	    // set request entity body
        ByteArrayEntity entity = new ByteArrayEntity(dataPackage); 
        post.setEntity(entity);
        log.info("attempting upload to " + serviceUrl.toString());
		HttpResponse resp = httpclient.execute(post);
   	    // process response
   	    log.info("response from upload request = " + resp);
   	    String status = resp.getStatusLine().toString();
   	    int responseCode = resp.getStatusLine().getStatusCode();
   	    if ( responseCode == 401 || responseCode == 403) throw new AuthenticationException(status);
   	    if (responseCode != 200) throw new IOException(status);
   	    httpclient.getConnectionManager().shutdown();  
	}
	/**
	 * Authenticates to a J2EE server
	 * This is a wrapper for the actual authentication method, it just prepares a header
	 * that contains the authentication cookie
	 * @throws IOException 
	 * @throws CertificateException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyStoreException 
	 */
	public void authenticate() {
		log.info("authenticating with username " + username);
		this.authHeader = authenticateJ2ee();
	}
	
	
	/**
	 * Method for authenticating against. J2EE server
	 * The authentication uses the BASIC method and creates an authorisation header that
	 * can be used to authenticate an http request..
	 * @return the authentication header, that should be added to http requests that
	 * require authorisation
	 * @throws KeyStoreException 
	 * @throws IOException 
	 * @throws CertificateException 
	 * @throws NoSuchAlgorithmException 
	 */
	private Header authenticateJ2ee() {
		String encoding = new String(Base64.encode((username+":"+password).getBytes()));
		Header authHeader = new BasicHeader("Authorization", "Basic " + encoding);
		return authHeader;
	}
}
