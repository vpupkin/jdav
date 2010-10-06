/*
 * Copyright (c) 2009 Evenflow, Inc.
 * 
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package com.dropbox.client;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import java.util.Map;
import com.dropbox.client.Authenticator;
import java.io.IOException;
import java.net.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;


/**
 * Unit test for simple App.
 */
public class AuthenticatorTest 
    extends TestCase
{

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AuthenticatorTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AuthenticatorTest.class );
    }

    public void test_loadConfig() throws Exception
    {
        Map config = Authenticator.loadConfig("config/testing.json");
        assert null != config.get("consumer_key");
        assert null != config.get("consumer_secret");
        assert null != config.get("request_token_url");
        assert null != config.get("access_token_url");
        assert null != config.get("authorization_url");
    }

    public void test_Authenticator() throws Exception 
    {
        Map config = Authenticator.loadConfig("config/testing.json");
        Authenticator auth = new Authenticator(config);
        assert auth.consumer_key == (String)config.get("consumer_key");
        assert auth.consumer_secret == (String)config.get("consumer_secret");
        assert auth.request_token_url == (String)config.get("request_token_url");
        assert auth.access_token_url == (String)config.get("access_token_url");
        assert auth.authorization_url == (String)config.get("authorization_url");
    }

    public void test_retrieveRequestToken() throws Exception 
    {
        Map config = Authenticator.loadConfig("config/testing.json");
        Authenticator auth = new Authenticator(config);
        String url = auth.retrieveRequestToken("somecallback");
        assert url != null : "Failed to get a authorization url.";
        assert auth.getTokenKey() != null : "Failed to set the access token key.";
        assert auth.getTokenSecret() != null : "Failed to set the access token secret.";
    }

    public void test_retrieveAccessToken() throws Exception 
    {
        Map config = Authenticator.loadConfig("config/testing.json");
        Authenticator auth = new Authenticator(config);
        String url = auth.retrieveRequestToken(null);
        String req_key = auth.getTokenKey();
        String req_secret = auth.getTokenSecret();

        Util.authorizeForm(url, (String)config.get("testing_user"), (String)config.get("testing_password"));
        auth.retrieveAccessToken("");

        assert auth.getTokenKey() != null : "Failed to set the access token key.";
        assert auth.getTokenSecret() != null : "Failed to set the access token secret.";
        assert auth.getTokenKey() != req_key : "Key should change.";
        assert auth.getTokenSecret() != req_secret : "Secret should change.";
    }


    public void test_sign() throws Exception 
    {
        Map config = Authenticator.loadConfig("config/testing.json");
        Authenticator auth = new Authenticator(config);
        String url = auth.retrieveRequestToken(null);
        Util.authorizeForm(url, (String)config.get("testing_user"), (String)config.get("testing_password"));
        auth.retrieveAccessToken("");

        URL account_url = new URL("http://" + (String)config.get("server") + "/0/files/sandbox/");
        HttpURLConnection request = auth.sign(account_url);
        request.connect();

        assert request.getResponseCode() == 200 : "Wrong status code when using access token: " + request.getResponseCode();

        // try against raw request
        request = (HttpURLConnection)account_url.openConnection();
        auth.sign(request);
        request.connect();
        assert request.getResponseCode() == 200 : "Wrong status code when usiing access token.";

        // try with commons httpclient
        HttpClient client = //new DefaultHttpClient();
        	TrustedAuthenticator. makeHTTPClient();
        HttpGet get_req = new HttpGet("http://" + (String)config.get("server") + "/0/files/sandbox/");
        auth.sign(get_req);
        HttpResponse response = client.execute(get_req);
        client.getConnectionManager().shutdown();        
    }

}
