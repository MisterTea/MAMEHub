package com.mamehub.client;

import java.awt.Desktop;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.FileAppender;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.slf4j.LoggerFactory;

import com.mamehub.client.net.RpcEngine;
import com.mamehub.client.utility.ClientDatabaseEngine;
import com.mamehub.thrift.ApplicationSettings;
import com.mamehub.thrift.OperatingSystem;
import com.mamehub.thrift.PlayerProfile;
import com.petebevin.markdown.MarkdownProcessor;

public class Utils {
  static final org.slf4j.Logger logger = LoggerFactory.getLogger(Utils.class);

  public static Set<Window> windows = new HashSet<Window>();
  private static ClientDatabaseEngine auditDatabaseEngine;
  private static ClientDatabaseEngine applicationDatabaseEngine;

  public static final int AUDIT_DATABASE_VERSION = 21;
  public static final int APPLICATION_DATABASE_VERSION = 9;

  private static PlayerProfile playerProfile = null;

  public static PlayerProfile getPlayerProfile(RpcEngine rpcEngine) {
    if (playerProfile == null) {
      playerProfile = rpcEngine.getMyProfile();
    }
    return playerProfile;
  }

  public static void commitProfile(RpcEngine rpcEngine) {
    rpcEngine.updateProfile(playerProfile);
  }

  public static void openWebpage(URL url) {
    try {
      openWebpage(url.toURI());
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
  }

  public static void openWebpage(URI uri) {
    try {
      Desktop.getDesktop().browse(uri);
    } catch (UnsupportedOperationException e) {
      try {
        Runtime.getRuntime().exec("xdg-open " + uri.toString());
      } catch (IOException e1) {
        e1.printStackTrace();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static String fileToString(File iniFile) throws IOException {
    StringBuilder sb = new StringBuilder((int) iniFile.length());

    String line;
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader(iniFile));

      while (true) {
        line = reader.readLine();
        if (line == null) {
          break;
        }
        if (sb.length() > 0) {
          sb.append('\n');
        }
        sb.append(line);
      }

      return sb.toString();
    } finally {
      if (reader != null) {
        reader.close();
      }
    }
  }

  public static synchronized void deleteAuditDatabaseEngine() {
    if (Utils.applicationDatabaseEngine != null) {
      Utils.applicationDatabaseEngine.close();
    }
    Utils.applicationDatabaseEngine = null;
    String dbDirectory = "./";// System.getProperty( "user.home" );
    try {
      FileUtils.deleteDirectory(new File(dbDirectory, "MAMEHubAuditDB"
          + AUDIT_DATABASE_VERSION));
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public static synchronized ClientDatabaseEngine getAuditDatabaseEngine() {
    String dbDirectory = "./";// System.getProperty( "user.home" );
    if (Utils.auditDatabaseEngine == null) {
      try {
        boolean inMemory = false;
        Utils.auditDatabaseEngine = new ClientDatabaseEngine(dbDirectory,
            "MAMEHubAuditDB" + AUDIT_DATABASE_VERSION, false, inMemory, true);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return Utils.auditDatabaseEngine;
  }

  public static synchronized ClientDatabaseEngine getApplicationDatabaseEngine() {
    String dbDirectory = "./";// System.getProperty( "user.home" );
    if (Utils.applicationDatabaseEngine == null) {
      try {
        boolean inMemory = false;
        Utils.applicationDatabaseEngine = new ClientDatabaseEngine(dbDirectory,
            "MAMEHubAppDB" + APPLICATION_DATABASE_VERSION, false, inMemory, false);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return Utils.applicationDatabaseEngine;
  }

  public static void shutdownDatabaseEngine() {
    if (Utils.auditDatabaseEngine != null) {
      Utils.auditDatabaseEngine.close();
    }
    if (Utils.applicationDatabaseEngine != null) {
      Utils.applicationDatabaseEngine.close();
    }
    Utils.auditDatabaseEngine = null;
    Utils.applicationDatabaseEngine = null;
  }

  public static boolean isWindows() {

    String os = System.getProperty("os.name").toLowerCase();
    // windows
    return (os.indexOf("win") >= 0);

  }

  public static boolean isMac() {

    String os = System.getProperty("os.name").toLowerCase();
    // Mac
    return (os.indexOf("mac") >= 0);

  }

  public static boolean isUnix() {

    String os = System.getProperty("os.name").toLowerCase();
    // linux or unix
    return (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0);

  }

  @SuppressWarnings("rawtypes")
  public static void flushAllLogs() {
    try {
      Set<FileAppender> flushedFileAppenders = new HashSet<FileAppender>();
      Enumeration currentLoggers = LogManager.getLoggerRepository()
          .getCurrentLoggers();
      while (currentLoggers.hasMoreElements()) {
        Object nextLogger = currentLoggers.nextElement();
        if (nextLogger instanceof Logger) {
          Logger currentLogger = (Logger) nextLogger;
          Enumeration allAppenders = currentLogger.getAllAppenders();
          while (allAppenders.hasMoreElements()) {
            Object nextElement = allAppenders.nextElement();
            if (nextElement instanceof FileAppender) {
              FileAppender fileAppender = (FileAppender) nextElement;
              if (!flushedFileAppenders.contains(fileAppender)
                  && !fileAppender.getImmediateFlush()) {
                flushedFileAppenders.add(fileAppender);
                // log.info("Appender "+fileAppender.getName()+" is not doing immediateFlush ");
                fileAppender.setImmediateFlush(true);
                currentLogger.info("FLUSH");
                fileAppender.setImmediateFlush(false);
              } else {
                // log.info("fileAppender"+fileAppender.getName()+" is doing immediateFlush");
              }
            }
          }
        }
      }
    } catch (RuntimeException e) {
      logger.error("Failed flushing logs", e);
    }
  }

  public static void removeWindow(Window window) {
    windows.remove(window);
    if (windows.isEmpty()) {
      logger.info("No windows left, exiting");
      System.exit(0);
    }
  }

  public static ApplicationSettings getApplicationSettings() {
    ApplicationSettings as = Utils.getApplicationDatabaseEngine()
        .getOrCreateMap(ApplicationSettings.class, "1").get("1");
    if (as == null) {
      as = new ApplicationSettings();
    }
    if (as.basePort == 0) {
      as.basePort = 6805;
      as.secondaryPort = 6806;
    }
    return as;
  }

  public static void putApplicationSettings(ApplicationSettings as) {
    Utils.getApplicationDatabaseEngine()
        .getOrCreateMap(ApplicationSettings.class, "1").put("1", as);
    Utils.getApplicationDatabaseEngine().commit();
  }

  public static String osToShortOS(OperatingSystem operatingSystem) {
    switch (operatingSystem) {
    case WINDOWS:
      return "WIN";
    case LINUX:
      return "LNX";
    case MAC:
      return "MAC";
    default:
      throw new RuntimeException("UNKNOWN OS");
    }
  }

  public static synchronized URL getResource(Class<?> classIn, String suffix) {
    URL u = classIn.getResource(suffix);
    if (u == null) {
      u = classIn.getResource("/data" + suffix);
    }
    if (u == null) {
      throw new RuntimeException("Could not find resource: " + suffix);
    }
    return u;
  }

  public static <K, V> void stagedClear(Map<K, V> map,
      ClientDatabaseEngine databaseEngine) {
    Set<K> keys = new HashSet<K>();
    keys.addAll(map.keySet());
    int count = 0;
    for (K key : keys) {
      map.remove(key);
      count++;
      if (count % 5000 == 0) {
        databaseEngine.commit();
      }
    }
    databaseEngine.commit();
  }

  public static void dumpMemoryUsage() {
    System.out.println("MEMORY USAGE: "
        + (Runtime.getRuntime().freeMemory() / 1024 / 1024)
        + " / "
        + (Runtime.getRuntime().maxMemory() / 1024 / 1024)
        + "     ("
        + (((float) Runtime.getRuntime().freeMemory()) * 100.0 / Runtime
            .getRuntime().maxMemory()) + ")");
  }

  public static boolean windowIsInactive(MameHubEngine mameHubEngine) {
    return KeyboardFocusManager.getCurrentKeyboardFocusManager()
        .getFocusedWindow() == null && !mameHubEngine.isGameRunning();
  }

  public static String injectLinks(String string) {
    // separete input by spaces ( URLs don't have spaces )
    String[] parts = string.split("\\s");
    String result = "";

    // Attempt to convert each item into an URL.
    for (String item : parts)
      try {
        URL url = new URL(item);
        // If possible then replace with anchor...
        result += "[" + url + "](" + url + ") ";
      } catch (MalformedURLException e) {
        // If there was an URL that was not it!...
        result += item + " ";
      }
    return result;
  }

  public static String markdownToHtml(String str) {
    // Escape html
    str = StringUtils.replaceEach(str, new String[] { "&", "\"", "<", ">" },
        new String[] { "&amp;", "&quot;", "&lt;", "&gt;" });
    str = injectLinks(str);
    MarkdownProcessor m = new MarkdownProcessor();
    str = m.markdown(str);
    return str;
  }

  public static void wipeAuditDatabaseEngine() throws IOException {
    Utils.auditDatabaseEngine.wipeDatabase();
    Utils.auditDatabaseEngine = null;
  }

  public static File getHashDirectory() {
    if (Utils.class.getResource("Utils.class").toString().contains("jar")) {
      return new File("../hash");
    } else {
      return new File("../Emulator/hash");
    }
  }
}
