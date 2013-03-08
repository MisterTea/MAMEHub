package com.mamehub.client;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import com.mamehub.client.net.RpcEngine;
import com.mamehub.thrift.PlayerProfile;

public class UpdateProfileDialog extends JDialog {
	private static final long serialVersionUID = 1L;
	private final JPanel contentPanel = new JPanel();
	private JTextField newPasswordOnce;
	private JTextField newPasswordTwice;
	private JLabel lblNewPassword;
	private JLabel lblNewPasswordagain;
	private JLabel lblNewLabel;
	private JLabel lblChangePassword;
	private JLabel label;
	private JLabel label_1;
	private JLabel label_2;
	private JTextField oldPassword;
	private PlayerProfile currentProfile;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			UpdateProfileDialog dialog = new UpdateProfileDialog(null, null);
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	public UpdateProfileDialog(JFrame parent, final RpcEngine rpcEngine) {
		super(parent, true);
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 400, 400);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new GridLayout(1, 0, 0, 0));
		{
			JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
			contentPanel.add(tabbedPane);
			{
				JPanel panel = new JPanel();
				tabbedPane.addTab("Personal", null, panel, null);
				panel.setLayout(new BorderLayout(0, 0));
				{
					JPanel panel2 = new JPanel();
					panel.add(panel2, BorderLayout.NORTH);
					{
						lblNewPassword = new JLabel("New Password");
						lblNewPassword.setHorizontalAlignment(SwingConstants.RIGHT);
					}
					panel2.setLayout(new GridLayout(0, 2, 0, 0));
					{
						lblNewLabel = new JLabel("");
						panel2.add(lblNewLabel);
					}
					{
						label_1 = new JLabel("");
						panel2.add(label_1);
					}
					{
						label = new JLabel("");
						panel2.add(label);
					}
					{
						lblChangePassword = new JLabel("Change Password");
						panel2.add(lblChangePassword);
					}
					{
						label_2 = new JLabel("Old Password");
						label_2.setHorizontalAlignment(SwingConstants.RIGHT);
						panel2.add(label_2);
					}
					{
						oldPassword = new JPasswordField();
						oldPassword.setColumns(10);
						panel2.add(oldPassword);
					}
					panel2.add(lblNewPassword);
					{
						newPasswordTwice = new JPasswordField();
						newPasswordTwice.setColumns(10);
					}
					{
						lblNewPasswordagain = new JLabel("New Password (Again)");
						lblNewPasswordagain.setHorizontalAlignment(SwingConstants.RIGHT);
					}
					{
						newPasswordOnce = new JPasswordField();
						newPasswordOnce.setColumns(10);
					}
					panel2.add(newPasswordOnce);
					panel2.add(lblNewPasswordagain);
					panel2.add(newPasswordTwice);
				}
			}
			}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						if(newPasswordOnce.getText().length()>0) {
							if(!newPasswordOnce.getText().equals(newPasswordTwice.getText())) {
								JOptionPane.showMessageDialog(UpdateProfileDialog.this, "Passwords much match.");
								return;
							}
							if(newPasswordOnce.getText().length()<3) {
								JOptionPane.showMessageDialog(UpdateProfileDialog.this, "New Password must be at least 3 characters.");
								return;
							}
						}
						if(newPasswordOnce.getText().length()>0) {
							if(!rpcEngine.changePassword(oldPassword.getText(), newPasswordOnce.getText())) {
								JOptionPane.showMessageDialog(UpdateProfileDialog.this, "You must enter a correct old password to change your password.");
								return;
							}
						}
						
						rpcEngine.updateProfile(makeProfile());
						UpdateProfileDialog.this.dispose();
					}
				});
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						UpdateProfileDialog.this.dispose();
					}
				});
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
		
		loadProfile(Utils.getPlayerProfile(rpcEngine));
	}
	
	void loadProfile(PlayerProfile profile) {
		currentProfile = new PlayerProfile(profile);
	}

	protected PlayerProfile makeProfile() {
		return currentProfile;
	}

}
