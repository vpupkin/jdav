package com.dropbox.client;
 
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Map;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.esxx.js.protocol.GAEConnectionManager; 
import org.json.simple.parser.ParseException;


public class TrustedAuthenticator extends Authenticator {

    public String api_host = null;
    public int port = 80;

    /** This is set by Dropbox to indicate what version of the API you are using. */
    final static public int API_VERSION=0;


    /**
     * Takes a Map of configuration values (similar to what's loaded by loadConfig) and configures
     * a for accessing the Dropbox service.
     *
     * You can preconfigure an access token by setting access_token_key and access_token_secret in
     * the config map.
     */
    @SuppressWarnings("unchecked")
    public TrustedAuthenticator(Map config)  throws IOException, OAuthException, OAuthCommunicationException 
    {
        super();
        this.config = config;
        this.consumer_key = (String)config.get("consumer_key");
        this.consumer_secret = (String)config.get("consumer_secret");
        this.consumer = new DefaultOAuthConsumer(this.consumer_key, this.consumer_secret);

        this.api_host = (String)config.get("server");
        this.port = ((Long)config.get("port")).intValue();

        if (config.get("access_token_key") != null) {
            assert config.get("access_token_secret") != null : "You must give the access_token_secret as well.";
            consumer.setTokenWithSecret((String)config.get("access_token_key"), (String)config.get("access_token_secret"));
        }
    
    }

    
    @SuppressWarnings("unchecked")
    public boolean retrieveTrustedAccessToken(String user_name, String user_password) 
    throws OAuthCommunicationException, OAuthMessageSignerException, IOException, ParseException, OAuthExpectationFailedException, DropboxException
    {
        assert this.config != null : "Config was not set.";
        assert user_name != null : "Must set a user name to create a token for.";
        assert user_password != null : "Must set a user_password to create a token for.";

        Object[] params = { "email", user_name, "password", user_password };
        
        OAuthConsumer cons = new CommonsHttpOAuthConsumer(this.consumer_key, this.consumer_secret);
        
        String target = RESTUtility.buildFullURL(RESTUtility.secureProtocol(), api_host, port, RESTUtility.buildURL("/token", API_VERSION, params));

        HttpGet req = new HttpGet(target);
        cons.sign(req);
        
        HttpClient client = //RESTUtility.getClient();
        					makeHTTPClient();
        
        
        try {
            HttpResponse response = client.execute(req);
            
            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode == 401 || responseCode == 404) {
                // Invalid user or password
                return false;
            } else if (responseCode != 200) {
                throw new DropboxException(response.getStatusLine().toString());
            }
            
            Map creds = (Map)RESTUtility.parseAsJSON(response);
            
            this.consumer.setTokenWithSecret((String)creds.get("token"), (String)creds.get("secret"));
        } catch (UnknownHostException uhe) {
            throw new DropboxException(uhe);
        }

        return true;
    }


 
	/**
	 * @author vipup
	 * @param httpClient
	 */
	private static void setupProxy(HttpClient httpClient) {
		String schemes[] = {"https", "http", "ftp"};
		for (String scheme : schemes) {
			String proxHostTmp = System.getProperty(scheme + ".proxyHost");// System.getProperties();
			String proxyPortTmp = System.getProperty(scheme + ".proxyPort");// System.setProperty("http.proxyHost","localhost");
			if (("" + proxHostTmp + proxyPortTmp).indexOf("null") == -1) {
				org.apache.http.HttpHost proxyTmp = new org.apache.http.HttpHost(
						proxHostTmp, Integer.parseInt(proxyPortTmp), scheme);
				httpClient.getParams().setParameter(
						ConnRoutePNames.DEFAULT_PROXY, proxyTmp);
			}
		}
	}
	
	public static boolean isGAE() {
		return !(System.getProperty("com.google.appengine.runtime.version")==null);
	}
	
	public static HttpClient makeHTTPClient() {
		HttpParams parmsTmp = new BasicHttpParams();

		org.apache.http.conn.ClientConnectionManager cmTmp = null;
		// ?new RrdGraphCmd():new RrdSvgCmd();
		if (! isGAE()) {
			SchemeRegistry sregTmp = new SchemeRegistry();
			PlainSocketFactory socketFactory = PlainSocketFactory
					.getSocketFactory();
			sregTmp.register(new Scheme("http", socketFactory, 80));
			SSLSocketFactory socketFactory2 = SSLSocketFactory
					.getSocketFactory();
			sregTmp.register(new Scheme("https", socketFactory2, 443));
			cmTmp = new ThreadSafeClientConnManager(parmsTmp, sregTmp);
		} else {
			cmTmp = new GAEConnectionManager();
		}
		HttpClient cliTmp = new DefaultHttpClient(cmTmp, parmsTmp);
		setupProxy(cliTmp);
		return cliTmp;
	}

    
    
}
