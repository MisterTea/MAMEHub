package com.mamehub.client.login;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mamehub.client.Main;
import com.mamehub.client.MainFrame;
import com.mamehub.client.Utils;
import com.mamehub.client.login.FacebookLogin.FacebookLoginCallback;
import com.mamehub.client.login.GoogleLogin.GoogleLoginCallback;
import com.mamehub.client.net.RpcEngine;
import com.mamehub.client.server.ClientHttpServer;
import com.mamehub.client.server.UDPReflectionServer;
import com.mamehub.thrift.ApplicationSettings;
import com.mamehub.thrift.Player;

public class LoginDialog extends JFrame implements FacebookLoginCallback, GoogleLoginCallback {
	private static final long serialVersionUID = 1L;
	final Logger logger = LoggerFactory.getLogger(LoginDialog.class);
	
	private final JPanel contentPanel = new JPanel();
	private JLabel lblWelcomeToMamehub;
	private JButton facebookLoginButton;
	
	public boolean loggingIn=false;
	protected FacebookLogin facebookLogin;
	protected GoogleLogin googleLogin;
	protected RpcEngine rpcEngine;
	
	private JLabel lblWaitingForAuthorization;
	private JButton btnCancelLogin;
	private JLabel errorLabel;
	private JPanel panel;
	private JPanel internalAccountPanel;
	private JLabel lblUserName;
	private JTextField usernameTextField;
	private JLabel lblPassword;
	private JTextField passwordTextField;
	private JButton manualLoginButton;
	private JButton newAccountButton;
	private JLabel lblNewLabel;
	private JButton googleLoginButton;
	private UDPReflectionServer udpReflectionServer;
	private ClientHttpServer clientHttpServer;
	private JPanel panel_1;
	private JLabel lblNewLabel_1;
	private JButton forgotPasswordButton;
	private JLabel lblOrCreateA;
	
	/**
	 * Create the dialog.
	 * @throws IOException 
	 */
	@SuppressWarnings("restriction")
	public LoginDialog(ClientHttpServer clientHttpServer) throws IOException {
		super();
		URL u = Utils.getResource(LoginDialog.class, "/MAMEHub.png");
		BufferedImage bi = ImageIO.read(u);
		this.setIconImage(bi);
		
		udpReflectionServer = new UDPReflectionServer();
		
		rpcEngine = new RpcEngine();
		logger.info("Adding intro dialog");
		Utils.windows.add(this);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent arg0) {
				logger.info("Got windowClosed for intro dialog");
				if(facebookLogin != null) {
					facebookLogin.giveUp = true;
					try {
						facebookLogin.join();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				logger.info("Removing intro dialog");
				Utils.removeWindow(LoginDialog.this);
			}
		});
		setBounds(100, 100, 400, 400);
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel);
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		{
			lblWelcomeToMamehub = new JLabel("Welcome to MAMEHub 2.0!");
			lblWelcomeToMamehub.setAlignmentX(Component.CENTER_ALIGNMENT);
			contentPanel.add(lblWelcomeToMamehub);
		}
		{
			lblNewLabel = new JLabel("Login through Facebook or Google");
			lblNewLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
			contentPanel.add(lblNewLabel);
		}
		{
			panel = new JPanel();
			contentPanel.add(panel);
			{
				facebookLoginButton = new JButton("Login");
				panel.add(facebookLoginButton);
				facebookLoginButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						RpcEngine.PingResponse pr = rpcEngine.ping();
						if(pr == RpcEngine.PingResponse.SERVER_DOWN) {
							JOptionPane.showMessageDialog(LoginDialog.this, "Login failed: MAMEHub server is down.");
							return;
						}
						if(pr == RpcEngine.PingResponse.CLIENT_TOO_OLD) {
							JOptionPane.showMessageDialog(LoginDialog.this, "Login failed: Your client is out of date, please restart MAMEHub.");
							return;
						}
						if(loggingIn)
							return;
						loggingIn = true;
						try {
							Main.portOpenerThread.join();
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						facebookLoginButton.setVisible(false);
						googleLoginButton.setVisible(false);
						internalAccountPanel.setVisible(false);
						btnCancelLogin.setVisible(true);
						lblWaitingForAuthorization.setVisible(true);
						facebookLogin = new FacebookLogin(rpcEngine, LoginDialog.this);
						facebookLogin.start();
						loggingIn = false;
					}
				});
				facebookLoginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
				facebookLoginButton.setIcon(new ImageIcon(Utils.getResource(LoginDialog.class, "/images/f_logo_icon.png")));
			}
			{
				googleLoginButton = new JButton("Login");
				googleLoginButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						RpcEngine.PingResponse pr = rpcEngine.ping();
						if(pr == RpcEngine.PingResponse.SERVER_DOWN) {
							JOptionPane.showMessageDialog(LoginDialog.this, "Login failed: MAMEHub server is down.");
							return;
						}
						if(pr == RpcEngine.PingResponse.CLIENT_TOO_OLD) {
							JOptionPane.showMessageDialog(LoginDialog.this, "Login failed: Your client is out of date, please restart MAMEHub.");
							return;
						}
						if(loggingIn)
							return;
						loggingIn = true;
						try {
							Main.portOpenerThread.join();
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						facebookLoginButton.setVisible(false);
						googleLoginButton.setVisible(false);
						internalAccountPanel.setVisible(false);
						btnCancelLogin.setVisible(true);
						lblWaitingForAuthorization.setVisible(true);
						googleLogin = new GoogleLogin(rpcEngine, LoginDialog.this);
						googleLogin.start();
						loggingIn = false;
					}
				});
				googleLoginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
				googleLoginButton.setIcon(new ImageIcon(Utils.getResource(LoginDialog.class, "/images/g_logo_icon.png")));
				panel.add(googleLoginButton);
			}
			{
				lblWaitingForAuthorization = new JLabel("Waiting for authorization...");
				panel.add(lblWaitingForAuthorization);
				lblWaitingForAuthorization.setVisible(false);
				lblWaitingForAuthorization.setAlignmentX(Component.CENTER_ALIGNMENT);
			}
			{
				btnCancelLogin = new JButton("Cancel Login");
				panel.add(btnCancelLogin);
				btnCancelLogin.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						cancelLogin();
					}
				});
				btnCancelLogin.setVisible(false);
				btnCancelLogin.setAlignmentX(Component.CENTER_ALIGNMENT);
			}
			{
				errorLabel = new JLabel("");
				panel.add(errorLabel);
				errorLabel.setForeground(Color.RED);
			}
		}
		{
			newAccountButton = new JButton("Create a New Account");
			newAccountButton.setAlignmentX(Component.CENTER_ALIGNMENT);
			newAccountButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					RpcEngine.PingResponse pr = rpcEngine.ping();
					if(pr == RpcEngine.PingResponse.SERVER_DOWN) {
						JOptionPane.showMessageDialog(LoginDialog.this, "New Account failed: MAMEHub server is down.");
						return;
					}
					if(pr == RpcEngine.PingResponse.CLIENT_TOO_OLD) {
						JOptionPane.showMessageDialog(LoginDialog.this, "New Account failed: Your client is out of date, please restart MAMEHub.");
						return;
					}
					try {
						Main.portOpenerThread.join();
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
					new NewAccountDialog(LoginDialog.this, rpcEngine).setVisible(true);
				}
			});
			{
				lblOrCreateA = new JLabel("Or create a new MAMEHub account");
				lblOrCreateA.setAlignmentX(0.5f);
				contentPanel.add(lblOrCreateA);
			}
			contentPanel.add(newAccountButton);
		}
		{
			lblNewLabel_1 = new JLabel("Or Login with an existing MAMEHub account");
			lblNewLabel_1.setAlignmentX(Component.CENTER_ALIGNMENT);
			contentPanel.add(lblNewLabel_1);
		}
		{
			internalAccountPanel = new JPanel();
			contentPanel.add(internalAccountPanel);
			internalAccountPanel.setLayout(new GridLayout(3, 2, 0, 0));
			{
				lblUserName = new JLabel("Username");
				internalAccountPanel.add(lblUserName);
			}
			{
				usernameTextField = new JTextField();
				internalAccountPanel.add(usernameTextField);
				usernameTextField.setColumns(10);
			}
			{
				lblPassword = new JLabel("Password");
				internalAccountPanel.add(lblPassword);
			}
			{
				passwordTextField = new JPasswordField();
				internalAccountPanel.add(passwordTextField);
				passwordTextField.setColumns(10);
			}
			{
				manualLoginButton = new JButton("Login");
				manualLoginButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						RpcEngine.PingResponse pr = rpcEngine.ping();
						if(pr == RpcEngine.PingResponse.SERVER_DOWN) {
							JOptionPane.showMessageDialog(LoginDialog.this, "Login failed: MAMEHub server is down.");
							return;
						}
						if(pr == RpcEngine.PingResponse.CLIENT_TOO_OLD) {
							JOptionPane.showMessageDialog(LoginDialog.this, "Login failed: Your client is out of date, please restart MAMEHub.");
							return;
						}
						try {
							Main.portOpenerThread.join();
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						String errorMessage = rpcEngine.login(usernameTextField.getText(), passwordTextField.getText());
						if(errorMessage.length()==0) {
							ApplicationSettings as = Utils.getApplicationSettings();
							as.lastInternalLoginId = usernameTextField.getText();
							Utils.putApplicationSettings(as);
							loginComplete();
						} else {
							JOptionPane.showMessageDialog(LoginDialog.this, "Login failed: " + errorMessage);
						}
					}
				});
				{
					panel_1 = new JPanel();
					internalAccountPanel.add(panel_1);
				}
				internalAccountPanel.add(manualLoginButton);
			}
			{
				forgotPasswordButton = new JButton("Email Password Reminder");
				forgotPasswordButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						RpcEngine.PingResponse pr = rpcEngine.ping();
						if(pr == RpcEngine.PingResponse.SERVER_DOWN) {
							JOptionPane.showMessageDialog(LoginDialog.this, "Email Password: MAMEHub server is down.");
							return;
						}
						if(pr == RpcEngine.PingResponse.CLIENT_TOO_OLD) {
							JOptionPane.showMessageDialog(LoginDialog.this, "Email Password: Your client is out of date, please restart MAMEHub.");
							return;
						}
						if(manualLoginButton.getText().length()<3) {
							JOptionPane.showMessageDialog(LoginDialog.this, "Enter a valid email address in the login field.");
							return;
						}
						if(!rpcEngine.emailPassword(manualLoginButton.getText())) {
							JOptionPane.showMessageDialog(LoginDialog.this, "Email address not found on server (did you make an account yet?).");
							return;
						} else {
							JOptionPane.showMessageDialog(LoginDialog.this, "Password sent to " + manualLoginButton.getText());
							return;
						}
					}
				});
				forgotPasswordButton.setAlignmentX(0.5f);
				contentPanel.add(forgotPasswordButton);
			}
		}
		
		ApplicationSettings as = Utils.getApplicationSettings();
		usernameTextField.setText(as.lastInternalLoginId);
	}

	protected void cancelLogin() {
		internalAccountPanel.setVisible(true);
		facebookLoginButton.setVisible(true);
		googleLoginButton.setVisible(true);
		btnCancelLogin.setVisible(false);
		lblWaitingForAuthorization.setVisible(false);
		if(facebookLogin != null) {
			facebookLogin.giveUp = true;
			try {
				facebookLogin.join();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			facebookLogin = null;
		}
		loggingIn = false;
	}
	
	void loginComplete() {
		try {
			udpReflectionServer.shutdown();
			MainFrame mainFrame = new MainFrame(rpcEngine, clientHttpServer);
			
			mainFrame.setVisible(true);
			this.dispose();
			
			Player myself = rpcEngine.getMyself();
			if(!myself.portsOpen) {
				JOptionPane.showMessageDialog(mainFrame, "MAMEHub has detected that you don't have port 6805 open (both TCP & UDP are required).\nAs a result, you cannot play networked games or transfer data with other players.\nSee http://portforward.com/ for details.");
			}
			
			return;
			
		} catch (Exception e) {
			e.printStackTrace();
			errorLabel.setText(e.getMessage());
		}
		
		// If we get here, an exception was thrown
		cancelLogin();
	}

	@Override
	public void facebookLoginComplete() {
		loginComplete();
	}

	@Override
	public void googleLoginComplete() {
		loginComplete();
	}

}
