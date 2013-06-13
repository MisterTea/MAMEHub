package com.mamehub.client;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import com.mamehub.client.net.RpcEngine;
import com.mamehub.thrift.ApplicationSettings;

public class UpdateSettingsDialog extends JDialog {
	private static final long serialVersionUID = 1L;
	private final JPanel contentPanel = new JPanel();
	private JLabel lblUsername;
	private JCheckBox chckbxAudioNotifications;
	private JCheckBox checkBoxAllowUploading;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			UpdateSettingsDialog dialog = new UpdateSettingsDialog(null, null);
			dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	public UpdateSettingsDialog(JFrame parent, final RpcEngine rpcEngine) {
		super(parent, true);
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 400, 400);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new GridLayout(1, 0, 0, 0));
		{
			JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP);
			contentPanel.add(tabbedPane);
			{
				JPanel panel = new JPanel();
				tabbedPane.addTab("Chat", null, panel, null);
				panel.setLayout(new BorderLayout(0, 0));
				{
					JPanel panel_1 = new JPanel();
					panel.add(panel_1, BorderLayout.NORTH);
					panel_1.setLayout(new GridLayout(0, 2, 0, 0));
					{
						lblUsername = new JLabel("");
						panel_1.add(lblUsername);
						lblUsername.setHorizontalAlignment(SwingConstants.RIGHT);
					}
					{
						chckbxAudioNotifications = new JCheckBox("Audio Notifications");
						panel_1.add(chckbxAudioNotifications);
					}
					{
						JLabel label = new JLabel("");
						label.setHorizontalAlignment(SwingConstants.RIGHT);
						panel_1.add(label);
					}
					{
						checkBoxAllowUploading = new JCheckBox("Allow Uploading");
						checkBoxAllowUploading.setSelected(true);
						panel_1.add(checkBoxAllowUploading);
					}
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
					@Override
					public void actionPerformed(ActionEvent e) {
						Utils.putApplicationSettings(getNewApplicationSettings());
						UpdateSettingsDialog.this.dispose();
					}
				});
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						UpdateSettingsDialog.this.dispose();
					}
				});
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
		
		loadFromApplicationSettings();
	}

	private void loadFromApplicationSettings() {
		ApplicationSettings as = Utils.getApplicationSettings();
		chckbxAudioNotifications.setSelected(as.chatAudio);
		checkBoxAllowUploading.setSelected(as.allowUploading);
	}

	protected ApplicationSettings getNewApplicationSettings() {
		ApplicationSettings as = Utils.getApplicationSettings();
		as.chatAudio = chckbxAudioNotifications.isSelected();
		as.allowUploading = checkBoxAllowUploading.isSelected();
		return as;
	}

}
