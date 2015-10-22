package com.mamehub.client.net;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.configuration.Configuration;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DecompressingHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TJSONProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mamehub.client.Utils;
import com.mamehub.rpc.MameHubRpc;
import com.mamehub.rpc.NotAuthorizedException;
import com.mamehub.thrift.Message;
import com.mamehub.thrift.Player;
import com.mamehub.thrift.PlayerProfile;
import com.mamehub.thrift.PlayerStatus;
import com.mamehub.thrift.RomPointer;
import com.mamehub.thrift.ServerState;
import com.mamehub.thrift.coreConstants;

public class RpcEngine implements Runnable {
  final Logger logger = LoggerFactory.getLogger(RpcEngine.class);

  private THttpClient gameTransport;
  private TProtocol gameProtocol;
  private MameHubRpc.Client gameClient;
  public String token;
  private Thread rpcEngineThread;
  public Set<RomPointer> favoriteRoms;

  public interface NetworkHandler {
    public void handleMessage(Message message);

    public void handleSessionExpired();

    public void handleServerDown(Exception e);

    public void handleException(Exception e);
  }

  private NetworkHandler networkHandler;

  public volatile boolean finished = false;
  private Thread rpcShutdownThread;

  private HttpClient httpClient;

  private int lastMessageIndex = 0;

  class PrivateMessageContainer {
    public String targetId;
    public Message message;

    public PrivateMessageContainer(String targetId, Message message) {
      this.targetId = targetId;
      this.message = message;
    }
  }

  private ConcurrentLinkedQueue<Message> messagesToBroadcast = new ConcurrentLinkedQueue<Message>();;
  private ConcurrentLinkedQueue<PrivateMessageContainer> privateMessagesToSend = new ConcurrentLinkedQueue<PrivateMessageContainer>();

  private int failCount = 0;

  public class RpcEngineShutdownHook implements Runnable {

    @Override
    public void run() {
      logger.info("LOGGING OUT");
      logout();
    }

  }

  public RpcEngine() throws IOException {
    this.token = generateString(new Random(System.currentTimeMillis()),
        "123456789qwertyuiopasdfghjklzxcvbnm", 16);

    try {
      final HttpParams httpParams = new BasicHttpParams();
      DefaultHttpClient baseHttpClient = new DefaultHttpClient(httpParams);
      // set the connection timeout value to 3 seconds (3000 milliseconds)
      HttpConnectionParams.setConnectionTimeout(baseHttpClient.getParams(),
          3000);
      baseHttpClient.setCookieStore(new BasicCookieStore());
      httpClient = new DecompressingHttpClient(baseHttpClient);

      Configuration conf = Utils.getConfiguration();
      gameTransport = new THttpClient(conf.getString("serverUrl")
          + "/MAMEHubServer/mamehub", httpClient);
      gameTransport.setConnectTimeout(10000);
      gameTransport.setReadTimeout(10000);
      gameTransport.open();
      gameProtocol = new TJSONProtocol(gameTransport);
      gameClient = new MameHubRpc.Client(gameProtocol);
    } catch (TException e) {
      throw new IOException(e);
    }
  }

  public static String generateString(Random rng, String characters, int length) {
    char[] text = new char[length];
    for (int i = 0; i < length; i++) {
      text[i] = characters.charAt(rng.nextInt(characters.length()));
    }
    return new String(text);
  }

  public synchronized void startThread() throws NotAuthorizedException,
      TException {
    favoriteRoms = gameClient.getFavorites();
    rpcEngineThread = new Thread(this);
    rpcEngineThread.start();
  }

  @Override
  public void run() {
    rpcShutdownThread = new Thread(new RpcEngineShutdownHook());
    Runtime.getRuntime().addShutdownHook(rpcShutdownThread);

    try {
      while (finished == false) {
        try {
          Thread.sleep(500);
          synchronized (this) {
            while (!messagesToBroadcast.isEmpty()) {
              gameClient.broadcastMessage(messagesToBroadcast.peek());
              messagesToBroadcast.poll();
            }
            while (!privateMessagesToSend.isEmpty()) {
              PrivateMessageContainer pmc = privateMessagesToSend.peek();
              privateMessagesToSend.poll();
              gameClient.sendMessage(pmc.targetId, pmc.message);
            }
          }

          try {
            while (finished == false) {
              List<Message> messages = null;
              synchronized (this) {
                if (token == null) {
                  finished = true;
                  break;
                }
                messages = gameClient.getMessages(lastMessageIndex);
                lastMessageIndex += messages.size();
              }
              if (messages != null && !messages.isEmpty()) {
                for (Message message : messages) {
                  networkHandler.handleMessage(message);
                }
              } else {
                break;
              }
            }
            failCount = 0;
          } catch (TTransportException e) {
            failCount++;
            logger.error(failCount + ") GOT A TRANSPORT ERROR: " + e);
            if (failCount >= 3000) {
              throw e;
            } else {
              Thread.sleep(3 * 1000);
            }
          }

          if (finished) {
            break;
          }

        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    } catch (TTransportException e) {
      e.printStackTrace();
      networkHandler.handleServerDown(e);
      finished = true;
    } catch (NotAuthorizedException e) {
      e.printStackTrace();
      networkHandler.handleSessionExpired();
      finished = true;
    } catch (TException e) {
      e.printStackTrace();
      networkHandler.handleException(e);
      finished = true;
    }

    try {
      Runtime.getRuntime().removeShutdownHook(rpcShutdownThread);
    } catch (IllegalStateException e) {
      // Ignore the error from removing the hook if we are already
      // shutting down
    }
    logger.info("RPC ENGINE TERMINATED");
  }

  public synchronized boolean checkExternalAuth() {
    try {
      boolean success = gameClient.validateToken(token);
      if (success) {
        // Now would be a good time to send any error logs
        File errorFile = new File("ErrorLog.txt");
        if (errorFile.exists()) {
          BufferedReader reader = null;
          try {
            String s = "";
            reader = new BufferedReader(new FileReader(errorFile));
            while (true) {
              String line = reader.readLine();
              if (line == null) {
                break;
              }
              s += line + "\n";
            }
            try {
              gameClient.sendError(s);
            } catch (TException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
          } catch (IOException e) {
            e.printStackTrace();
          } finally {
            if (reader != null) {
              try {
                reader.close();
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            }
          }
          errorFile.delete();
        }
      }
      return success;
    } catch (TException e) {
      e.printStackTrace();
      return false;
    }
  }

  public void setMessageHandler(NetworkHandler messageHandler) {
    this.networkHandler = messageHandler;
  }

  public void broadcastMessage(Message message) {
    messagesToBroadcast.add(message);
  }

  public synchronized Player getPlayer(String id) {
    try {
      return gameClient.getPlayer(id);
    } catch (NotAuthorizedException e) {
      networkHandler.handleSessionExpired();
    } catch (TTransportException e) {
      networkHandler.handleServerDown(e);
    } catch (TException e) {
      networkHandler.handleException(e);
    }
    return null;
  }

  public synchronized ServerState getServerState() {
    try {
      return gameClient.getServerState();
    } catch (NotAuthorizedException e) {
      networkHandler.handleSessionExpired();
    } catch (TTransportException e) {
      networkHandler.handleServerDown(e);
    } catch (TException e) {
      networkHandler.handleException(e);
    }
    return null;
  }

  public synchronized void logout() {
    try {
      if (token != null && !finished) {
        gameClient.logout();
      }
    } catch (NotAuthorizedException e) {
      // Silently fail if you can't log out
      e.printStackTrace();
    } catch (TException e) {
      // Silently fail if you can't log out
      e.printStackTrace();
    }
    token = null;
    finished = true;
  }

  public synchronized void hostGame(String system, String romName,
      String cartName) {
    try {
      gameClient.hostGame(system, romName, cartName);
    } catch (NotAuthorizedException e) {
      networkHandler.handleSessionExpired();
    } catch (TTransportException e) {
      networkHandler.handleServerDown(e);
    } catch (TException e) {
      networkHandler.handleException(e);
    }
  }

  public synchronized void leaveGame() {
    try {
      gameClient.leaveGame();
    } catch (NotAuthorizedException e) {
      networkHandler.handleSessionExpired();
    } catch (TTransportException e) {
      networkHandler.handleServerDown(e);
    } catch (TException e) {
      networkHandler.handleException(e);
    }
  }

  public synchronized String joinGame(String gameId) {
    try {
      return gameClient.joinGame(gameId);
    } catch (NotAuthorizedException e) {
      networkHandler.handleSessionExpired();
    } catch (TTransportException e) {
      networkHandler.handleServerDown(e);
    } catch (TException e) {
      networkHandler.handleException(e);
    }
    return null;
  }

  public synchronized Player getMyself() {
    try {
      Player p = gameClient.getMyself();
      if (p != null) {
        logger.info("GOT MYSELF: " + p);
      }
      return p;
    } catch (TException e) {
      // getMyself is used to verify login, so don't throw here
    }
    return null;
  }

  public void sendPrivateMessage(String targetId, Message message) {
    privateMessagesToSend.add(new PrivateMessageContainer(targetId, message));
  }

  public enum PingResponse {
    OK, CLIENT_TOO_OLD, SERVER_DOWN
  }

  public synchronized PingResponse ping() {
    try {
      int ping = gameClient.ping();
      if (ping > coreConstants.MAMEHUB_VERSION) {
        return PingResponse.CLIENT_TOO_OLD;
      } else {
        return PingResponse.OK;
      }
    } catch (TException e) {
      System.out.println("Connection failed:");
      e.printStackTrace();
      return PingResponse.SERVER_DOWN;
    } finally {
    }
  }

  public synchronized String login(String username, String password) {
    Configuration conf = Utils.getConfiguration();
    String url = conf.getString("serverUrl") + "/MAMEHubServer/internallogin?"
        + "token=" + token + "&username=" + username + "&password=" + password;

    HttpGet httpget = new HttpGet(url);

    try {
      logger.info("executing login request ");

      // Create a response handler
      HttpResponse loginResponse = httpClient.execute(httpget);

      if (loginResponse.getStatusLine().getStatusCode() != 200) {
        String errorMessage = convertStreamToString(loginResponse.getEntity()
            .getContent());
        if (errorMessage.length() > 100) {
          logger.info("ERROR LOGGING IN: " + errorMessage);
          return "Server is down";
        } else {
          return errorMessage;
        }
      }

      EntityUtils.consume(loginResponse.getEntity());

      return "";
    } catch (IOException e) {
      return e.getMessage();
    } finally {
      httpget.reset();
    }
  }

  public synchronized void setPorts() {
    try {
      logger.info("Setting ports");
      gameClient.setPorts(Utils.getConfiguration().getInt("basePort"),
          Utils.getConfiguration().getInt("secondaryPort"));
    } catch (TException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  public synchronized String newAccount(String email, String username,
      String password) {
    Configuration conf = Utils.getConfiguration();
    String url = conf.getString("serverUrl") + "/MAMEHubServer/internallogin?"
        + "token=" + token + "&email=" + email + "&username=" + username
        + "&password=" + password + "&newaccount=yes";

    HttpGet httpget = new HttpGet(url);

    try {
      logger.info("executing login request ");

      // Create a response handler
      HttpResponse loginResponse = httpClient.execute(httpget);

      if (loginResponse.getStatusLine().getStatusCode() != 200) {
        String errorMessage = convertStreamToString(loginResponse.getEntity()
            .getContent());
        if (errorMessage.length() > 100) {
          logger.info("ERROR LOGGING IN: " + errorMessage);
          return "Server is down";
        } else {
          return errorMessage;
        }
      }

      EntityUtils.consume(loginResponse.getEntity());

      return "";
    } catch (IOException e) {
      return e.getMessage();
    } finally {
      httpget.reset();
    }
  }

  private String convertStreamToString(java.io.InputStream is) {
    try {
      return new java.util.Scanner(is).useDelimiter("\\A").next();
    } catch (java.util.NoSuchElementException e) {
      return "";
    }
  }

  public synchronized void sendError(String string) {
    try {
      gameClient.sendError(string);
    } catch (TException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public synchronized boolean changePassword(String oldPassword,
      String newPassword) {
    try {
      return gameClient.changePassword(oldPassword, newPassword);
    } catch (TTransportException e) {
      networkHandler.handleServerDown(e);
    } catch (NotAuthorizedException e) {
      networkHandler.handleSessionExpired();
    } catch (TException e) {
      networkHandler.handleException(e);
    }
    return false;
  }

  public synchronized boolean changeUsername(String newUsername) {
    try {
      return gameClient.changeUsername(newUsername);
    } catch (TTransportException e) {
      networkHandler.handleServerDown(e);
    } catch (NotAuthorizedException e) {
      networkHandler.handleSessionExpired();
    } catch (TException e) {
      networkHandler.handleException(e);
    }
    return false;
  }

  public synchronized boolean emailPassword(String emailAddress) {
    try {
      return gameClient.emailPassword(emailAddress);
    } catch (TTransportException e) {
      networkHandler.handleServerDown(e);
    } catch (NotAuthorizedException e) {
      networkHandler.handleSessionExpired();
    } catch (TException e) {
      networkHandler.handleException(e);
    }
    return false;
  }

  public synchronized PlayerProfile getMyProfile() {
    return getProfile(getMyself()._id);
  }

  public synchronized PlayerProfile getProfile(String playerId) {
    try {
      return gameClient.getPlayerProfile(playerId);
    } catch (TTransportException e) {
      networkHandler.handleServerDown(e);
    } catch (NotAuthorizedException e) {
      networkHandler.handleSessionExpired();
    } catch (TException e) {
      networkHandler.handleException(e);
    }
    return null;
  }

  public synchronized void updateProfile(PlayerProfile newProfile) {
    try {
      gameClient.updateProfile(newProfile);
    } catch (TTransportException e) {
      networkHandler.handleServerDown(e);
    } catch (NotAuthorizedException e) {
      networkHandler.handleSessionExpired();
    } catch (TException e) {
      networkHandler.handleException(e);
    }
  }

  public synchronized void postUserFeedback(String result, String log) {
    try {
      gameClient.postUserFeedback(result, log);
    } catch (Exception e) {
      // Expecting errors for large logs, so just skip exceptions
    }
  }

  public synchronized void updateStatus(PlayerStatus playerStatus) {
    try {
      gameClient.updateStatus(playerStatus);
    } catch (TTransportException e) {
      networkHandler.handleServerDown(e);
    } catch (NotAuthorizedException e) {
      networkHandler.handleSessionExpired();
    } catch (TException e) {
      networkHandler.handleException(e);
    }
  }

  public synchronized String getMOTD() {
    try {
      return gameClient.getMOTD();
    } catch (TTransportException e) {
      networkHandler.handleServerDown(e);
    } catch (NotAuthorizedException e) {
      networkHandler.handleSessionExpired();
    } catch (TException e) {
      networkHandler.handleException(e);
    }
    return "";
  }
}
