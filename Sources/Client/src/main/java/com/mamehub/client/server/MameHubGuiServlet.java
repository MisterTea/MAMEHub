package com.mamehub.client.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.thrift.protocol.TJSONProtocol;
import org.apache.thrift.server.TServlet;

import com.mamehub.rpc.MameHubGuiRpc;

/**
 * Servlet implementation class Login
 */
@WebServlet("/mamehubgui")
public class MameHubGuiServlet extends TServlet {
  private static final long serialVersionUID = 1L;

  public MameHubGuiServlet() {
    super(new MameHubGuiRpc.Processor<MameHubGuiRpcImpl>(
        new MameHubGuiRpcImpl()), new TJSONProtocol.Factory());
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    super.doPost(request, response);
  }
}
