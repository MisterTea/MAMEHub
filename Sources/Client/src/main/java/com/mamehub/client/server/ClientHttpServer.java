package com.mamehub.client.server;

import java.util.EnumSet;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.GzipFilter;

public class ClientHttpServer {
  private Thread httpServerThread;

  public ClientHttpServer(final int port) {
    httpServerThread = new Thread(new Runnable() {
      @Override
      public void run() {
        System.out.println("Creating server...");
        final Server server = new Server(port);

        ServletContextHandler context = new ServletContextHandler(
            ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        EnumSet<DispatcherType> all = EnumSet.of(DispatcherType.ASYNC,
            DispatcherType.ERROR, DispatcherType.FORWARD,
            DispatcherType.INCLUDE, DispatcherType.REQUEST);
        FilterHolder gzipFilter = new FilterHolder(new GzipFilter());
        gzipFilter
            .setInitParameter(
                "mimeTypes",
                "text/html,text/xml,text/plain,application/json,application/x-javascript,text/javascript,text/x-javascript,text/x-json");
        gzipFilter.setInitParameter("minGzipSize", "0");
        context.addFilter(gzipFilter, "/mamehubclient/*", all);

        context.addServlet(new ServletHolder(new MameHubClientServlet()),
            "/mamehubclient/*");

        try {
          System.out.println("Starting server");
          server.start();
          System.out.println("Blocking until shutdown");
        } catch (Exception e) {
          e.printStackTrace();
          System.exit(1);
        }
        try {
          server.join();
        } catch (InterruptedException e) {
          System.out.println("Server interrupted");
        }
      }
    });

    httpServerThread.start();
  }

  public void join() throws InterruptedException {
    httpServerThread.join();
  }

  public void shutdown() {
    httpServerThread.interrupt();
    try {
      httpServerThread.join();
    } catch (InterruptedException e) {
      throw new RuntimeException("A thread was interrupted at a bad time");
    }
  }
}
