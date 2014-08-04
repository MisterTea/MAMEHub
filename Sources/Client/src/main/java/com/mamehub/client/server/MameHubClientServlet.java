package com.mamehub.client.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.thrift.protocol.TJSONProtocol;
import org.apache.thrift.server.TServlet;

import com.mamehub.rpc.MameHubClientRpc;

/**
 * Servlet implementation class Login
 */
@WebServlet("/mamehubclient")
public class MameHubClientServlet extends TServlet {
	private static final long serialVersionUID = 1L;
	
    public MameHubClientServlet() {
        super(new MameHubClientRpc.Processor<MameHubClientRpcImpl>(new MameHubClientRpcImpl()), new TJSONProtocol.Factory());
    }
    
    @Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	super.doPost(request, response);
	}
}
