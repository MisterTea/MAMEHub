package com.mamehub.client.login;

import java.awt.Desktop;
import java.net.URI;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mamehub.client.Utils;
import com.mamehub.client.net.RpcEngine;

public class GoogleLogin extends Thread {
  final Logger logger = LoggerFactory.getLogger(GoogleLogin.class);

  public static String APP_ID = "727179905666-1phgdhs94ifu3q0nvsg7um9nuv95fo3l.apps.googleusercontent.com";
  public static String SCOPE = "https://www.googleapis.com/auth/plus.me+https://www.googleapis.com/auth/userinfo.email+https://www.googleapis.com/auth/userinfo.profile";

  public boolean giveUp = false;
  private GoogleLoginCallback callback;
  private RpcEngine rpcEngine;

  public interface GoogleLoginCallback {
    public void googleLoginComplete();
  }

  public GoogleLogin(RpcEngine rpcEngine, GoogleLoginCallback callback) {
    this.rpcEngine = rpcEngine;
    this.callback = callback;
  }

  @Override
  public void run() {
    try {
      // Build the authentication URL for the user to fill out
      Configuration conf = Utils.getConfiguration();
      final String url = "https://accounts.google.com/o/oauth2/auth?"
          + "response_type=code" + "&redirect_uri="
          + conf.getString("serverUrl") + "/MAMEHubServer/googlelogin"
          + "&client_id=" + APP_ID + "&scope=" + SCOPE + "&state="
          + rpcEngine.token;
      // Open an external browser to login to your application
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          // eb = new EmbeddedBrowser(url);
          try {
            Desktop.getDesktop().browse(new URI(url));
          } catch (Exception e) {
            giveUp = true;
          }
        }
      });
      // Wait until the login process is completed
      // System.in.read();

      while (!giveUp) {
        if (rpcEngine.checkExternalAuth()) {
          break;
        } else {
          logger.info("Could not authenticate (yet)");
        }
        Thread.sleep(1000);
      }

      // eb.dispose();

      if (giveUp) {
        return;
      }

      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          callback.googleLoginComplete();
        }
      });
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

}
