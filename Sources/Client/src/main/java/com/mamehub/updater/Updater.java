package com.mamehub.updater;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/**
 * Hello world!
 * 
 */
public class Updater {
  private static final String UPDATER_DB_RAN_ONCE_TOOLS_TXT = "UpdaterDB/RanOnceTools1.txt";
  private static final String UPDATER_DB_RAN_ONCE_MAIN_TXT = "UpdaterDB/RanOnceMain1.txt";
  boolean finished = false;

  public Updater() {
  }

  int sumCompleted;
  int totalWork;

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

  public static void main(String[] args) throws Exception {
    addToolsFirewallExceptions();

    Updater app = new Updater();
    System.out.println("Updating MAMEHub, please be patient!!!");
    System.out.println("Working Directory: " + new File(".").getAbsolutePath());

    try {
      Set<PosixFilePermission> permissions = new HashSet<PosixFilePermission>();
      permissions.add(PosixFilePermission.OWNER_READ);
      permissions.add(PosixFilePermission.OWNER_WRITE);
      permissions.add(PosixFilePermission.OWNER_EXECUTE);

      List<URL> urls = new ArrayList<URL>();
      if (Updater.isMac()) {
        urls.add(new URL(
            "http://10ghost.net/MAMEHubDownloads/Emulators/Mac/csume64"));
      } else if (Updater.isUnix()) {
        if (System.getProperty("sun.arch.data.model").equals("64")) {
          urls.add(new URL(
              "http://10ghost.net/MAMEHubDownloads/Emulators/Linux/csume64"));
        } else {
          urls.add(new URL(
              "http://10ghost.net/MAMEHubDownloads/Emulators/Linux32/csume"));
        }
      } else if (Updater.isWindows()) {
        boolean is64bit = (System.getenv("ProgramFiles(x86)") != null);
        if (is64bit) {
          urls.add(new URL(
              "http://10ghost.net/MAMEHubDownloads/Emulators/Windows/csume64.exe"));
          urls.add(new URL(
              "http://10ghost.net/MAMEHubDownloads/Emulators/Windows/csume64.sym"));
        } else {
          urls.add(new URL(
              "http://10ghost.net/MAMEHubDownloads/Emulators/Windows/csume.exe"));
          urls.add(new URL(
              "http://10ghost.net/MAMEHubDownloads/Emulators/Windows/csume.sym"));
        }
      }

      for (int a = 0; a < urls.size(); a++) {
        app.wget(urls.get(a), ".");
      }

      try {
        if (Updater.isMac()) {
          Files.setPosixFilePermissions(
              FileSystems.getDefault().getPath("csume64"), permissions);
        } else if (Updater.isUnix()) {
          if (System.getProperty("sun.arch.data.model").equals("64")) {
            Files.setPosixFilePermissions(
                FileSystems.getDefault().getPath("csume64"), permissions);
          } else {
            Files.setPosixFilePermissions(
                FileSystems.getDefault().getPath("csume"), permissions);
          }
        } else if (Updater.isWindows()) {
          boolean is64bit = (System.getenv("ProgramFiles(x86)") != null);
          if (is64bit) {
            Files.setPosixFilePermissions(
                FileSystems.getDefault().getPath("csume64.exe"), permissions);
          } else {
            Files.setPosixFilePermissions(
                FileSystems.getDefault().getPath("csume.exe"), permissions);
          }
        }
      } catch (UnsupportedOperationException e) {
        System.out.println("Could not chmod file" + e.toString());
        // Ignore these exceptions
      }

      // System.out.println("Fetching frontend...");
      // app.wget(new URL(
      // "http://10ghost.net/MAMEHubDownloads/Frontend/MAMEHubClient.jar"),
      // "MAMEHubRepo/Binaries/dist");

      addMainFirewallExceptions();
    } catch (Exception e) {
      System.out
          .println("Error getting updates, please copy the error from the console and file a bug here: https://github.com/MisterTea/MAMEHub/issues");
      e.printStackTrace();
      System.in.read();
    }
    app.finished = true;
  }

  private static void addToolsFirewallExceptions() throws IOException {
    boolean firstTime = !(new File(UPDATER_DB_RAN_ONCE_TOOLS_TXT).exists());
    if (firstTime && isWindows()) {
      System.out.println("Configuring windows firewall for tools...");
      // Configure the windows firewall
      String[][] commands;
      if (System.getProperty("os.version").equals("5.1")) {
        // Windows XP
        String[][] lCommands = {
            { "netsh", "firewall", "add", "allowedprogram",
                "program=Tools\\Updater.exe", "name=MAMEHub Updater" },
            { "netsh", "firewall", "add", "allowedprogram",
                "program=Tools\\wget.exe", "name=MAMEHub WGet" }, };
        commands = lCommands;
      } else {
        // Vista or greater
        String[][] lCommands = {
            { "netsh", "advfirewall", "firewall", "add", "rule", "dir=in",
                "action=allow", "program=Tools\\Updater.exe",
                "name=MAMEHub Updater In", "enable=yes" },
            { "netsh", "advfirewall", "firewall", "add", "rule", "dir=out",
                "action=allow", "program=Tools\\Updater.exe",
                "name=MAMEHub Updater Out", "enable=yes" },
            { "netsh", "advfirewall", "firewall", "add", "rule", "dir=in",
                "action=allow", "program=Tools\\wget.exe",
                "name=MAMEHub WGet In", "enable=yes" },
            { "netsh", "advfirewall", "firewall", "add", "rule", "dir=out",
                "action=allow", "program=Tools\\wget.exe",
                "name=MAMEHub WGet Out", "enable=yes" }, };
        commands = lCommands;
      }
      for (String[] command : commands) {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
            process.getInputStream()));
        String line = null;
        while ((line = reader.readLine()) != null) {
          System.out.println(line);
        }
      }
    }
    new File(UPDATER_DB_RAN_ONCE_TOOLS_TXT).createNewFile();
  }

  private static void addMainFirewallExceptions() throws IOException {
    boolean firstTime = !(new File(UPDATER_DB_RAN_ONCE_MAIN_TXT).exists());
    if (firstTime && isWindows()) {
      System.out.println("Configuring windows firewall for main binaries...");
      // Configure the windows firewall
      String[][] commands;
      if (System.getProperty("os.version").equals("5.1")) {
        // Windows XP
        String[][] lCommands = {
            { "netsh", "firewall", "add", "allowedprogram",
                "program=MAMEHubRepo\\Binaries\\dist\\MAMEHubClient.exe",
                "name=MAMEHub Client" },
            { "netsh", "firewall", "add", "allowedprogram",
                "program=MAMEHubRepo\\Binaries\\dist\\csume.exe", "name=csume" },
            { "netsh", "firewall", "add", "allowedprogram",
                "program=MAMEHubRepo\\Binaries\\dist\\csume64.exe",
                "name=csume" } };
        commands = lCommands;
      } else {
        // Vista or greater
        String[][] lCommands = {
            { "netsh", "advfirewall", "firewall", "add", "rule", "dir=in",
                "action=allow",
                "program=MAMEHubRepo\\Binaries\\dist\\MAMEHubClient.exe",
                "name=MAMEHub Client In", "enable=yes" },
            { "netsh", "advfirewall", "firewall", "add", "rule", "dir=out",
                "action=allow",
                "program=MAMEHubRepo\\Binaries\\dist\\MAMEHubClient.exe",
                "name=MAMEHub Client Out", "enable=yes" },
            { "netsh", "advfirewall", "firewall", "add", "rule", "dir=in",
                "action=allow",
                "program=MAMEHubRepo\\Binaries\\dist\\csume.exe",
                "name=csume In", "enable=yes" },
            { "netsh", "advfirewall", "firewall", "add", "rule", "dir=out",
                "action=allow",
                "program=MAMEHubRepo\\Binaries\\dist\\csume.exe",
                "name=csume Out", "enable=yes" },
            { "netsh", "advfirewall", "firewall", "add", "rule", "dir=in",
                "action=allow",
                "program=MAMEHubRepo\\Binaries\\dist\\csume64.exe",
                "name=csume64 In", "enable=yes" },
            { "netsh", "advfirewall", "firewall", "add", "rule", "dir=out",
                "action=allow",
                "program=MAMEHubRepo\\Binaries\\dist\\csume64.exe",
                "name=csume64 Out", "enable=yes" } };
        commands = lCommands;
      }
      for (String[] command : commands) {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
            process.getInputStream()));
        String line = null;
        while ((line = reader.readLine()) != null) {
          System.out.println(line);
        }
      }
    }
    new File(UPDATER_DB_RAN_ONCE_MAIN_TXT).createNewFile();
  }

  public void wget(URL url, String destination) throws IOException,
      InterruptedException {
    String[] cmd = new String[] { "tools/wget", "-P" + destination, "-N",
        url.toString() + ".gz" };
    if (!Updater.isWindows()) {
      cmd[0] = "wget"; // Linux & mac users should have wget in their OS.
    }
    for (String s : cmd) {
      System.out.print(s + " ");
    }
    System.out.println("");
    ProcessBuilder pb = new ProcessBuilder(cmd);
    pb.redirectErrorStream(true);
    Process p = pb.start();
    BufferedReader r = new BufferedReader(new InputStreamReader(
        p.getInputStream()));
    while (true) {
      String s = r.readLine();
      if (s == null) {
        break;
      }
      System.out.println(s);
    }
    int val = p.waitFor();
    if (val != 0) {
      throw new IOException("Exception during wget; return code = " + val);
    } else {
      System.out.println("WGET COMPLETE");
    }

    String urlString = url.toString();
    String fileName = urlString.substring(urlString.lastIndexOf('/') + 1,
        urlString.length());
    gunzip(new File("MAMEHubRepo/Binaries/dist/" + fileName + ".gz"), new File(
        "MAMEHubRepo/Binaries/dist/" + fileName));
  }

  public void gunzip(File source, File dest) {

    byte[] buffer = new byte[1024];

    try {

      GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(source));

      FileOutputStream out = new FileOutputStream(dest);

      int len;
      while ((len = gzis.read(buffer)) > 0) {
        out.write(buffer, 0, len);
      }

      gzis.close();
      out.close();

      System.out.println("Done");

    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  public void start(int totalTasks) {
    System.out.println("Total Tasks: " + totalTasks);
    sumCompleted = 0;
    this.totalWork = 0;
  }

  public void beginTask(String title, int totalWork) {
    this.totalWork = totalWork;
    sumCompleted = 0;
    System.out.println("Beginning: " + title + " : " + totalWork);
  }

  public void update(int completed) {
    if (totalWork > 0) {
      if (sumCompleted * 100 / totalWork != ((sumCompleted + completed) * 100 / totalWork)) {
        System.out.println("Completed: "
            + ((sumCompleted + completed) * 100 / totalWork) + "%");
      }
      sumCompleted += completed;
    }
  }

  public void endTask() {
    System.out.println("DONE");
  }

  public boolean isCancelled() {
    return false;
  }
}
