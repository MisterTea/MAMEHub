package com.mamehub.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.output.WriterOutputStream;

import com.mamehub.updater.Updater;

public class EmulatorUpdater implements Runnable {
  private Process process;
  private ProcessBuilder pb;
  private Writer writer;

  public EmulatorUpdater(Writer writer) {
    this.writer = writer;
  }

  public void run() {
    try {
      writer.write("STARTING EMULATOR UPDATER\n");
      updateEmulators();
      updateMetaData();
    } catch (IOException e) {
      flushStackTrace(e);
    }
  }

  public void updateEmulators() throws IOException {
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
        wget(urls.get(a), ".");
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
        writer.write("Could not chmod file" + e.toString() + "\n");
        flushStackTrace(e);
        // Ignore these exceptions
      }
    } catch (Exception e) {
      flushStackTrace(e);
    }
  }

  private void flushStackTrace(Exception e) {
    PrintStream ps = new PrintStream(new WriterOutputStream(writer));
    e.printStackTrace(ps);
    ps.flush();
    ps.close();
  }

  public void wget(URL url, String destination) throws IOException,
      InterruptedException {
    String[] cmd = new String[] { "../../../Tools/wget", "-P" + destination,
        "-N", url.toString() + ".gz" };
    if (Updater.isMac()) {
      cmd[0] = "../../../Tools/wget_osx";
    } else if (!Updater.isWindows()) {
      cmd[0] = "wget"; // Linux users should have wget in their OS.
    }
    for (String s : cmd) {
      writer.write(s + " ");
    }
    writer.write("\n");
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
      writer.write(s + "\n");
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
    gunzip(new File("./" + fileName + ".gz"), new File("./" + fileName));
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

    } catch (IOException e) {
      flushStackTrace(e);
    }
  }

  public void updateMetaData() {
    // wget --mirror --no-parent -nH --cut-dirs=1
    // http://10ghost.net/MAMEHubDownloads/MAMEHubRepo/
    List<String> cmdList = new ArrayList<String>();
    try {
      if (Utils.isWindows()) {
        cmdList.add(new File("..\\..\\..\\Tools\\wget.exe").getCanonicalPath());
      } else if (Utils.isMac()) {
        cmdList.add(new File("../../../Tools/wget_osx").getCanonicalPath());
      } else {
        cmdList.add("wget");
      }
      cmdList.add("--mirror");
      cmdList.add("--no-parent");
      cmdList.add("-nH");
      cmdList.add("--cut-dirs=1");
      cmdList.add("http://10ghost.net/MAMEHubDownloads/MAMEHubRepo/");
      pb = new ProcessBuilder(cmdList);
      if (Utils.isWindows()) {
        pb.directory(new File("..\\..\\..").getCanonicalFile());
      } else {
        pb.directory(new File("../../..").getCanonicalFile());
      }
      pb.redirectErrorStream(true);
      process = pb.start();
    } catch (IOException e) {
      flushStackTrace(e);
      throw new RuntimeException(e);
    }

    Scanner s = new Scanner(process.getInputStream());
    while (s.hasNextLine()) {
      try {
        writer.write("EMULATOR_UPDATE: " + s.nextLine() + "\n");
      } catch (IOException e) {
        flushStackTrace(e);
        throw new RuntimeException(e);
      }
    }
    s.close();

    int result = -1;
    try {
      result = process.waitFor();
    } catch (InterruptedException e) {
      flushStackTrace(e);
    }

    System.out.printf("Process exited with result %d", result);
  }

  public static void main(String[] args) throws InterruptedException {
    new EmulatorUpdater(new PrintWriter(System.out)).run();
  }
}
