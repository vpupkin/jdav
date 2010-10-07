package com.dropbox.client;

import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.*;
import java.io.*;

public class Util {

    public static void authorizeForm(String url, String testing_user, String testing_password) throws IOException {
        BufferedReader cin = new BufferedReader(new InputStreamReader(System.in));


        assert url != null : "You must give a url.";
        assert testing_user != null : "You gave a null testing_user.";
        assert testing_password != null : "You gave a null testing_password.";

        System.out.println("AUTHORIZING: " + url);

        
		String proxyHost = "proxy.Host";
		String schemes[] = {"https", "http" };
		WebClient webClient = null;
		try{
			for (String scheme : schemes) {
				String proxHostTmp = System.getProperty(scheme + ".proxyHost"); 
				int proxyPortTmp = Integer.parseInt( System.getProperty(scheme + ".proxyPort"));	
				if (url.startsWith(scheme) && null!=proxHostTmp){
					webClient = new WebClient(BrowserVersion.FIREFOX_3 , proxyHost, proxyPortTmp);
					System.out.println("choosed PROXY for ["+scheme+"]" +
							"  "+proxHostTmp+":"+proxyPortTmp);
					break;
				}
			}
		}catch(Exception e){}
		if (webClient == null)
			webClient = new WebClient();
        webClient.setJavaScriptEnabled(false);
         
        HtmlPage page = (HtmlPage)webClient.getPage(url);
        HtmlForm form = (HtmlForm)page.getForms().get(1);
        HtmlSubmitInput button = (HtmlSubmitInput)form.getInputByValue("Log in");


        HtmlTextInput emailField = (HtmlTextInput)form.getInputByName("login_email");
        emailField.setValueAttribute(testing_user);

        HtmlPasswordInput password = (HtmlPasswordInput)form.getInputByName("login_password");
        password.setValueAttribute(testing_password);

        // Now submit the form by clicking the button and get back the second page.
        HtmlPage page2 = (HtmlPage)button.click();

        try {
            form = (HtmlForm)page2.getForms().get(1);
            button = (HtmlSubmitInput)form.getInputByValue("Allow");
            button.click();
        } catch(ElementNotFoundException e) {
            System.out.println("No allow button, must be already approved.");
        } catch(IndexOutOfBoundsException e) {
            System.out.println("No second form, must be already approved.");
        }
    }

}
