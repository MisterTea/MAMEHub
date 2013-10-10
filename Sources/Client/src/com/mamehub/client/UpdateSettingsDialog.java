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

import com.mamehub.thrift.ApplicationSettings;
import javax.swing.JTextField;

public class UpdateSettingsDialog extends JDialog {
	private static final long serialVersionUID = 1L;
	private final JPanel contentPanel = new JPanel();
	private JLabel lblUsername;
	private JCheckBox chckbxAudioNotifications;
	private JCheckBox checkBoxAllowUploading;
	private JCheckBox checkBoxShowEmulatorLog;
	private JTextField basePort;
	private JTextField secondaryPort;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			UpdateSettingsDialog dialog = new UpdateSettingsDialog(null);
			dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	public UpdateSettingsDialog(JFrame parent) {
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
				tabbedPane.addTab("Settings", null, panel, null);
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
					{
						JLabel label = new JLabel("");
						label.setHorizontalAlignment(SwingConstants.RIGHT);
						panel_1.add(label);
					}
					{
						checkBoxShowEmulatorLog = new JCheckBox("Show Emulator Log");
						panel_1.add(checkBoxShowEmulatorLog);
					}
					{
						JLabel lblBasePort = new JLabel("Main Port");
						lblBasePort.setHorizontalAlignment(SwingConstants.RIGHT);
						panel_1.add(lblBasePort);
					}
					{
						basePort = new JTextField();
						panel_1.add(basePort);
						basePort.setColumns(10);
					}
					{
						JLabel lblSecondaryPort = new JLabel("Status Port");
						lblSecondaryPort.setHorizontalAlignment(SwingConstants.RIGHT);
						panel_1.add(lblSecondaryPort);
					}
					{
						secondaryPort = new JTextField();
						secondaryPort.setColumns(10);
						panel_1.add(secondaryPort);
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
		
		setVisible(true);
	}

	private void loadFromApplicationSettings() {
		ApplicationSettings as = Utils.getApplicationSettings();
		chckbxAudioNotifications.setSelected(as.chatAudio);
		checkBoxAllowUploading.setSelected(as.allowUploading);
		checkBoxShowEmulatorLog.setSelected(as.showEmulatorLog);
		basePort.setText(""+as.basePort);
		secondaryPort.setText(""+as.secondaryPort);
	}

	protected ApplicationSettings getNewApplicationSettings() {
		ApplicationSettings as = Utils.getApplicationSettings();
		as.chatAudio = chckbxAudioNotifications.isSelected();
		as.allowUploading = checkBoxAllowUploading.isSelected();
		as.showEmulatorLog = checkBoxShowEmulatorLog.isSelected();
		as.basePort = Integer.parseInt(basePort.getText());
		as.secondaryPort = Integer.parseInt(secondaryPort.getText());
		return as;
	}

}
