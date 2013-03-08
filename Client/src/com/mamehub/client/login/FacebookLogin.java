package com.mamehub.client.login;

import java.awt.Desktop;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mamehub.client.net.RpcEngine;

public class FacebookLogin extends Thread {
	final Logger logger = LoggerFactory.getLogger(FacebookLogin.class);

	public static String APP_ID = "462026417146889";

	public boolean giveUp = false;
	private FacebookLoginCallback callback;
	private RpcEngine rpcEngine;

	//EmbeddedBrowser eb;
	
	public interface FacebookLoginCallback {
		public void facebookLoginComplete();
	}
	
	public FacebookLogin(RpcEngine rpcEngine, FacebookLoginCallback callback) {
		this.rpcEngine = rpcEngine;
		this.callback = callback;
	}

	public void run() {
		try {
			// Build the authentication URL for the user to fill out
			final String url = "https://www.facebook.com/dialog/oauth?" +
				"&redirect_uri=http://"+RpcEngine.HOSTNAME+":"+RpcEngine.PORT+"/MAMEHubServer/facebooklogin" +
			    "&client_id=" + APP_ID + 
			    "&state=" + rpcEngine.token;
			// Open an external browser to login to your application
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					try {
						Desktop.getDesktop().browse(new URI(url));
						//eb = new EmbeddedBrowser(url);
					} catch (Exception e) {
						giveUp = true;
					}
				}
			});
			// Wait until the login process is completed
			// System.in.read();

			while(!giveUp) {
				Thread.sleep(1000);
				if(rpcEngine.checkExternalAuth()) {
					break;
				} else {
					logger.info("Could not authenticate (yet)");
				}
			}
			
			//eb.dispose();
			
			if (giveUp) {
				return;
			}
			
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					callback.facebookLoginComplete();
				}
			});
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
