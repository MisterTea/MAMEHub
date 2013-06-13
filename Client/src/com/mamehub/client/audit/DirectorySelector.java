package com.mamehub.client.audit;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.AbstractListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DirectorySelector extends JDialog {
	final Logger logger = LoggerFactory.getLogger(DirectorySelector.class);
	private static final long serialVersionUID = 1L;

	private final JPanel contentPanel = new JPanel();
	private JList directoryList;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			DirectorySelector dialog = new DirectorySelector();
			dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static class FileNameModel extends AbstractListModel {
		private static final long serialVersionUID = 1L;
		IniParser iniParser;

		public FileNameModel() {
			iniParser = new IniParser();
		}
		
		@Override
		public int getSize() {
			return iniParser.getRomPaths().size();
		}
		
		@Override
		public Object getElementAt(int index) {
			return iniParser.getRomPaths().get(index);
		}
	}

	/**
	 * Create the dialog.
	 */
	public DirectorySelector() {
		setModal(true);
		setBounds(100, 100, 800, 400);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BorderLayout(0, 0));
		{
			directoryList = new JList();
			directoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			directoryList.setModel(new FileNameModel());
			contentPanel.add(directoryList);
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton addDirectory = new JButton("Add Directory");
				addDirectory.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						JFileChooser chooser = new JFileChooser("./");
						chooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY);
						int retval = chooser.showOpenDialog(DirectorySelector.this);
						if(retval == JFileChooser.APPROVE_OPTION) {
							File file = chooser.getSelectedFile();
							logger.info("ADDING PATH: " + file);
							new IniParser().addRomPath(file);
							directoryList.setModel(new FileNameModel());
						}
					}
				});
				buttonPane.add(addDirectory);
			}
			{
				JButton removeSelectedDirectory = new JButton("Remove Selected Directory");
				removeSelectedDirectory.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						File f = (File)directoryList.getSelectedValue();
						new IniParser().removeRomPath(f);
						directoryList.setModel(new FileNameModel());
					}
				});
				removeSelectedDirectory.setActionCommand("Save Changes");
				buttonPane.add(removeSelectedDirectory);
			}
			{
				JButton exitButton = new JButton("Exit");
				exitButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						DirectorySelector.this.dispose();
					}
				});
				exitButton.setActionCommand("Save Changes");
				buttonPane.add(exitButton);
				getRootPane().setDefaultButton(exitButton);
			}
		}
		
		init();
	}
	
	private void init() {
		directoryList.removeAll();
	}

}
