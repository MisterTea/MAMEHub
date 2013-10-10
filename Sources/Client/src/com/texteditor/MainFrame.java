package com.texteditor;

import javax.management.RuntimeErrorException;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JToolBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.JPopupMenu;

/**
 * 
 * class for the creation of the main JFrame that includes a JTabbedPane
 * containing all open documents
 * 
 * @author Pasquale Puzio
 *
 */
public class MainFrame extends JFrame implements ActionListener {
	
	private static final Dimension startDimension = new Dimension(800, 600);
	
	private JLabel saveLabel;
	
	private JTabbedPane tabbedPane;
	
	private JPopupMenu textPopup;
	
	private JMenu menuFile, menuEdit, tools, help, skin;
	
	private JMenuItem newDocument;
	
	private JMenuItem open;
	
	private JMenuItem save;
	
	private JMenuItem saveAs;
	
	private JMenuItem print;
	
	private JMenuItem close;
	
	private JMenuItem exit;
	
	private JMenuItem copy;
	
	private JMenuItem paste;
	
	private JMenuItem cut;
	
	private JMenuItem selectAll;
	
	private JMenuItem find;
	
	private JMenuItem format;
	
	private JMenuItem insertTime;
	
	private JMenuItem toPDF;
	
	private JMenuItem copyPopup;
	
	private JMenuItem pastePopup;
	
	private JMenuItem cutPopup;
	
	private JMenuItem selectAllPopup;
	
	private JMenuItem formatPopup;
	
	private JMenuItem license;
	
	private JMenuItem bug;
	
	private JMenuItem about;
	
	private JMenuBar menuBar;
	
	private JCheckBoxMenuItem classic, system, motif, gtk, windows, nimbus;
	
	private JToolBar toolBar;
	
	private JButton buttonNew, buttonOpen, buttonCopy, buttonPaste, buttonCut ,buttonPrint, buttonSave, buttonFind, buttonPDF;
	
	private int numNewDocument = 0;
	
	private ArrayList<TextEditorPane> documents = new ArrayList<TextEditorPane>();
	
	public MainFrame()
	{	
		this.setTitle("TextEditor++ 1.0");
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.setSize(startDimension);
		createComponent();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int x, y;
		y = (screenSize.height/2) - (getHeight()/2);
		x = (screenSize.width/2) - (getWidth()/2);
		setLocation(x, y);
	}
	
	/**
	 * method for the creation and visualization of all the components:
	 * menu bar, tabbed panel and icon toolbar
	 */
	private void createComponent()
	{
		
		menuBar = new JMenuBar();
		menuFile = new JMenu("File");
			newDocument = new JMenuItem("New");
				newDocument.addActionListener(this);
			open = new JMenuItem("Open");
				open.addActionListener(this);
			save = new JMenuItem("Save");
				save.addActionListener(this);
			saveAs = new JMenuItem("Save as...");
				saveAs.addActionListener(this);
			print = new JMenuItem("Print");
				print.addActionListener(this);
			close = new JMenuItem("Close");
				close.addActionListener(this);
			exit = new JMenuItem("Exit");
				exit.addActionListener(this);
			menuFile.add(newDocument);
			menuFile.add(open);
			menuFile.add(save);
			menuFile.add(saveAs);
			menuFile.add(print);
			menuFile.addSeparator();
			menuFile.add(close);
			menuFile.add(exit);
			menuFile.setMnemonic('F');
		menuEdit = new JMenu("Edit");
			copy = new JMenuItem("Copy");
				copy.addActionListener(this);
			paste = new JMenuItem("Paste");
				paste.addActionListener(this);
			cut = new JMenuItem("Cut");
				cut.addActionListener(this);
			selectAll = new JMenuItem("Select all");
				selectAll.addActionListener(this);
			find = new JMenuItem("Find/Replace");
				find.addActionListener(this);
			format = new JMenuItem("Format");
				format.addActionListener(this);
			insertTime = new JMenuItem("Insert Date & Time");
				insertTime.addActionListener(this);
			menuEdit.add(copy);
			menuEdit.add(paste);
			menuEdit.add(cut);
			menuEdit.add(selectAll);
			menuEdit.add(find);
			menuEdit.addSeparator();
			menuEdit.add(format);
			menuEdit.add(insertTime);
			menuEdit.setMnemonic('E');
		help = new JMenu("?");
			license = new JMenuItem("License");
			license.addActionListener(this);
			bug = new JMenuItem("Bug report and more info");
			bug.addActionListener(this);
			about = new JMenuItem("About");
			about.addActionListener(this);
			help.add(license);
			help.add(bug);
			help.addSeparator();
			help.add(about);
			help.setMnemonic('?');
		tools = new JMenu("Tools");
			tools.setMnemonic('T');
			skin = new JMenu("Skin");
			classic = new JCheckBoxMenuItem("Java Classic");
			classic.addActionListener(this);
			classic.setSelected(true);
			system = new JCheckBoxMenuItem("System");
			system.setSelected(false);
			system.addActionListener(this);
			motif = new JCheckBoxMenuItem("Motif");
			motif.setSelected(false);
			motif.addActionListener(this);
			gtk = new JCheckBoxMenuItem("Gtk");
			gtk.setSelected(false);
			gtk.addActionListener(this);
			windows = new JCheckBoxMenuItem("Windows");
			windows.setSelected(false);
			windows.addActionListener(this);
			nimbus = new JCheckBoxMenuItem("Nimbus");
			nimbus.setSelected(false);
			nimbus.addActionListener(this);
			tools.add(skin);
			skin.add(classic);
			skin.add(system);
			skin.add(motif);
			skin.add(gtk);
			skin.add(windows);
			skin.add(nimbus);
			tools.addSeparator();
			toPDF = new JMenuItem("Convert to PDF");
			toPDF.addActionListener(this);
			tools.add(toPDF);
		menuBar.add(menuFile);
		menuBar.add(menuEdit);
		menuBar.add(tools);
		menuBar.add(help);
		this.getContentPane().add(menuBar, BorderLayout.NORTH);
		
		textPopup = new JPopupMenu();
		copyPopup = new JMenuItem("Copy");
		copyPopup.addActionListener(this);
		pastePopup = new JMenuItem("Paste");
		pastePopup.addActionListener(this);
		cutPopup = new JMenuItem("Cut");
		cutPopup.addActionListener(this);
		selectAllPopup = new JMenuItem("Select all");
		selectAllPopup.addActionListener(this);
		formatPopup = new JMenuItem("Format");
		formatPopup.addActionListener(this);
		textPopup.add(copyPopup);
		textPopup.add(pastePopup);
		textPopup.add(cutPopup);
		textPopup.add(selectAllPopup);
		textPopup.add(formatPopup);
		
		saveLabel = new JLabel("Last Save: Modified: ");
		JPanel panel = new JPanel();
		panel.add(saveLabel);
		this.getContentPane().add(panel, BorderLayout.SOUTH);
		// creation and visualization of toolbar
		toolBar = new JToolBar(JToolBar.HORIZONTAL);
		toolBar.setFloatable(false);
		buttonNew = new JButton(new ImageIcon(getClass().getResource("images/new.png")));
		buttonNew.setToolTipText("New document");
		buttonNew.addActionListener(this);
		buttonOpen = new JButton(new ImageIcon(getClass().getResource("images/open.png")));
		buttonOpen.setToolTipText("Open a document");
		buttonOpen.addActionListener(this);
		buttonSave = new JButton(new ImageIcon(getClass().getResource("images/save.png")));
		buttonSave.setToolTipText("Save the document");
		buttonSave.addActionListener(this);
		buttonCopy = new JButton(new ImageIcon(getClass().getResource("images/copy.png")));
		buttonCopy.setToolTipText("Copy to clipboard");
		buttonCopy.addActionListener(this);
		buttonPaste = new JButton(new ImageIcon(getClass().getResource("images/paste.png")));
		buttonPaste.setToolTipText("Paste from clipboard");
		buttonPaste.addActionListener(this);
		buttonCut = new JButton(new ImageIcon(getClass().getResource("images/cut.jpg")));
		buttonCut.setToolTipText("Cut in clipboard");
		buttonCut.addActionListener(this);
		buttonFind = new JButton(new ImageIcon(getClass().getResource("images/find.jpg")));
		buttonFind.setToolTipText("Find a word");
		buttonFind.addActionListener(this);
		buttonPrint = new JButton(new ImageIcon(getClass().getResource("images/print.jpg")));
		buttonPrint.setToolTipText("Print the document");
		buttonPrint.addActionListener(this);
		buttonPDF = new JButton(new ImageIcon(getClass().getResource("images/pdf.png")));
		buttonPDF.setToolTipText("Convert to PDF");
		buttonPDF.addActionListener(this);
		toolBar.add(buttonNew);
		toolBar.add(buttonOpen);
		toolBar.add(buttonSave);
		toolBar.addSeparator();
		toolBar.add(buttonCopy);
		toolBar.add(buttonPaste);
		toolBar.add(buttonCut);
		toolBar.add(buttonFind);
		toolBar.add(buttonPrint);
		toolBar.addSeparator();
		toolBar.add(buttonPDF);
		JPanel centralPanel = new JPanel(new BorderLayout());
		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.addChangeListener(new TabListener());
		centralPanel.add(toolBar, BorderLayout.NORTH);
		centralPanel.add(tabbedPane, BorderLayout.CENTER);
		this.add(centralPanel, BorderLayout.CENTER);
	}
	
	public void save_click()
	{
		if (!documents.get(tabbedPane.getSelectedIndex()).isAlreadySaved() && documents.get(tabbedPane.getSelectedIndex()).getPaneName().startsWith("New Document"))
			saveAs.doClick();
		else
		{
			boolean flag = documents.get(tabbedPane.getSelectedIndex()).saveToFile();
			saveLabel.setText("Last Save: " + documents.get(tabbedPane.getSelectedIndex()).getLastSave() + " Modified: No");
			if (!flag)
				JOptionPane.showMessageDialog(this,"Ci è stato un errore durante il salvataggio di " + documents.get(tabbedPane.getSelectedIndex()).getPaneName(), "Error" ,JOptionPane.ERROR_MESSAGE);
		}
	}
	
	/**
	 * method to simulate the print button click 
	 */
	public void print_click()
	{
		int n = JOptionPane.showOptionDialog(this, "Are you sure to print " + documents.get(tabbedPane.getSelectedIndex()).getPaneName() + "?", "Print", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
		if (n == 0)	
			documents.get(tabbedPane.getSelectedIndex()).printPane();
	}
	
	/**
	 * method to simulate the close button click 
	 */
	public void close_click()
	{
		if (documents.size() > 0)
		{
			if (documents.get(tabbedPane.getSelectedIndex()).isModified())
			{
				int result = JOptionPane.showOptionDialog(this, "Are you sure to close " + documents.get(tabbedPane.getSelectedIndex()).getPaneName() + " without saving?", "Exit", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[] { "Save", "Don't Save" ,"Cancel"}, null);
				if (result == 2)
				{
					return; // pressione dell'opzione Cancel
				}
				else if (result == 0)
				{
					save.doClick();
				}
			}
			documents.remove(tabbedPane.getSelectedIndex());
			tabbedPane.remove(tabbedPane.getSelectedIndex());
		}
		else
		{
			System.exit(0);
		}
	}
	
	/**
	 * method to simulate the about button click 
	 */
	public void about_click()
	{
		new AboutDialog();
	}
	
	/**
	 * method to change the skin of text editor
	 * @param className class reference
	 */
	private void changeSkin()
	{
		try {
			if (classic.isSelected())
			{
				UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
			}
			else if (system.isSelected())
			{
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}
			else if (motif.isSelected())
			{
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
			}
			else if (gtk.isSelected())
			{
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
			}
			else if (windows.isSelected())
			{
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
			}
			else if (nimbus.isSelected())
			{
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
			}
			
			// components update
			tabbedPane.updateUI();
			menuBar.updateUI();
			toolBar.updateUI();
			buttonNew.updateUI();
			buttonOpen.updateUI();
			buttonCopy.updateUI();
			buttonPaste.updateUI();
			buttonCut.updateUI();
			buttonSave.updateUI();
			buttonPrint.updateUI();
			buttonFind.updateUI();
			buttonPDF.updateUI();
			saveLabel.updateUI();
			newDocument.updateUI();
			open.updateUI();
			save.updateUI();
			saveAs.updateUI();
			print.updateUI();
			close.updateUI();
			exit.updateUI();
			format.updateUI();
			copy.updateUI();
			paste.updateUI();
			cut.updateUI();
			selectAll.updateUI();
			insertTime.updateUI();
			find.updateUI();
			skin.updateUI();
			toPDF.updateUI();
			about.updateUI();
			bug.updateUI();
			license.updateUI();
			classic.updateUI();
			windows.updateUI();
			gtk.updateUI();
			nimbus.updateUI();
			motif.updateUI();
			system.updateUI();
			copyPopup.updateUI();
			pastePopup.updateUI();
			cutPopup.updateUI();
			formatPopup.updateUI();
			selectAllPopup.updateUI();
			for (int i = 0; i<documents.size(); i++)
				documents.get(i).updateUI();
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(null, "Skin changing failed!", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	/**
	 * method to insert a new empty document
	 */
	public void insertNewDocument()
	{		
		// creiamo un oggetto di tipo TextEditorPane e lo aggiungiamo anche ad un arrayList
		// per sfruttare la tecnica del riferimento in memoria
		TextEditorPane document = new TextEditorPane();
		document.setAlreadySaved(false);
		document.setPaneName("New Document" + getNewDocumentAppend() + ".txt");
		document.setMouseListener(new PopupListener());
		documents.add(document);
		JScrollPane scrollPane = new JScrollPane(document);
		tabbedPane.add(document.getPaneName() , scrollPane);
		numNewDocument++;
	}
	
	/**
	 * method to open one or more documents from selected files of a file chooser
	 */
	public void openDocument()
	{
		TxtFileChooser fileChooser = new TxtFileChooser(System.getProperty("user.home") + System.getProperty("file.separator") + "Documenti");
		fileChooser.setMultiSelectionEnabled(true);
		int result = fileChooser.showOpenDialog(this);
		if (result == fileChooser.APPROVE_OPTION)
		{
			File[] files = fileChooser.getSelectedFiles();
			for (int i = 0; i < files.length; i++)
			{
					addDocument(files[i]);
					if (documents.size() > 0)
					{
						tabbedPane.setSelectedIndex(tabbedPane.getComponentCount() -1);
						saveLabel.setText("Last Save: " + documents.get(tabbedPane.getSelectedIndex()).getLastSave() + " Modified: No");
					}
			}
		}
	}
	
	/**
	 * method to read a selected file
	 * @param file file to read
	 */
	public void addDocument(File file)
	{
		if (!file.exists())
		{
			JOptionPane.showMessageDialog(this, file.getName() + " doesn't exists", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
			
		TextEditorPane textPane = null;
		String fileText = null;
		
		if (file.getName().toUpperCase().endsWith(".FTF"))
		{
			textPane = readFTFFile(file);
			textPane.setCaretPosition(0);
		}
		else
		{
			textPane = new TextEditorPane();
			textPane.setAlreadySaved(true);
			fileText = readTxtFile(file);
			if (fileText != null)
			{
				textPane.setText(fileText);
				textPane.setCaretPosition(0);
				textPane.setPanePath(file.getAbsolutePath());
				textPane.setMouseListener(new PopupListener());
			}
		}
		
		if (textPane != null || fileText != null)
		{
			JScrollPane scrollPane = new JScrollPane(textPane);
			tabbedPane.add(file.getName() , scrollPane);
			documents.add(textPane);
		}
		else
		{
			JOptionPane.showMessageDialog(null, "Error in the loading of " + file.getName(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	/**
	 * method to read a FTF file (FTF is a new format defined in this program)
	 * @param file file to read
	 * @return a new TextEditorPane
	 */
	private TextEditorPane readFTFFile(File file)
	{
	    try {
			FileInputStream inFileStream = new FileInputStream(file);
			ObjectInputStream inObjectStream = new ObjectInputStream(inFileStream);
			
			TextEditorPane textPane = ((TextEditorPane) (inObjectStream.readObject()));
			
			inObjectStream.close();
			if (textPane != null)
			{
				textPane.setKeyListener();
				textPane.setMouseListener(new PopupListener());
			}
			return textPane;
			
		} catch (FileNotFoundException e) {
			return null;
		} catch (IOException e) {
			return null;
		} catch (ClassNotFoundException e) {
			return null;
		}
	}
	
	/**
	 * method to read a text file or another type of file but not a FTF file
	 * @param file
	 * @return
	 */
	private String readTxtFile(File file)
	{
			try {
				FileReader fileReader = new FileReader(file);
				BufferedReader bufferedReader = new BufferedReader(fileReader);
				String inputLine;
				String result = "";
				inputLine = bufferedReader.readLine();
				
				while (inputLine != null)
				{
					result += inputLine;
					inputLine = bufferedReader.readLine();
					// questo controllo viene effettuato per evitare che anche l'ultima riga contenga il carattere di fine riga
					if (inputLine != null)
						result += "\n";
				}
				
				fileReader.close();
				bufferedReader.close();
				
				return result;
				
			} catch (FileNotFoundException e) {
				return null;
			} catch (IOException e) {
				return null;
			}
		
	}
	
	/**
	 * method to obtain counting of new empty documents
	 * @return counter
	 */
	private String getNewDocumentAppend()
	{
		if (numNewDocument == 0)
			return "";
		else
			return " " + Integer.toString(numNewDocument);
	}
	
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();
		
		if (source.equals(newDocument))
		{
			insertNewDocument();
			tabbedPane.setSelectedIndex(tabbedPane.getComponentCount() -1);
			saveLabel.setText("Last Save: " + documents.get(tabbedPane.getSelectedIndex()).getLastSave() + " Modified: No");
		}
		else if (source.equals(open))
		{
			int oldsize = documents.size();
			openDocument();
			
			// se il programma è appena stato avviato, chiude il tab New Document
			if (documents.size() > oldsize && documents.get(0).getPaneName().equalsIgnoreCase("New Document.txt") && !documents.get(0).isModified() && !documents.get(0).isAlreadySaved())
			{	
				numNewDocument--;
				tabbedPane.remove(0);
				documents.remove(0);
			}
			// updating of window title
			setTitle("TextEditor++ " + tabbedPane.getTitleAt(tabbedPane.getSelectedIndex()));
		}
		else if(source.equals(save) && documents.size() > 0)
		{
			save_click();
		}
		else if (source.equals(saveAs) && documents.size() > 0)
		{
			TxtFileChooser chooser = new TxtFileChooser(documents.get(tabbedPane.getSelectedIndex()).getPanePath());
			chooser.setMultiSelectionEnabled(false);
			chooser.setDialogTitle("Save " + tabbedPane.getTitleAt(tabbedPane.getSelectedIndex()));
			chooser.setSelectedFile(new File(tabbedPane.getTitleAt(tabbedPane.getSelectedIndex())));
			
			if (chooser.getSelectedFile().getName().toUpperCase().endsWith(".TXT"))
				chooser.setFileFilter(chooser.getChoosableFileFilters()[1]);
			else if (chooser.getSelectedFile().getName().toUpperCase().endsWith(".FTF"))
				chooser.setFileFilter(chooser.getChoosableFileFilters()[2]);
			else
				chooser.setFileFilter(chooser.getChoosableFileFilters()[0]);
			
			int result = chooser.showSaveDialog(this);
			if (result == TxtFileChooser.APPROVE_OPTION)
			{
				// procedura per assicurarsi di concatenare le giuste estensioni
				File file = chooser.getSelectedFile();
				
				String path = file.getPath();
				
				String descriptor = "";
				if (chooser.getFileFilter().getDescription().equalsIgnoreCase(chooser.getChoosableFileFilters()[2].getDescription()))
					descriptor = "FTF";
				else if (chooser.getFileFilter().getDescription().equalsIgnoreCase(chooser.getChoosableFileFilters()[1].getDescription()))
					descriptor = "TXT";
				else if (chooser.getFileFilter().getDescription().equalsIgnoreCase(chooser.getChoosableFileFilters()[3].getDescription()))
					descriptor = "FTF&TXT";
				else
					descriptor = null;
				
				if (descriptor != null && descriptor.equalsIgnoreCase("FTF") && !file.getName().toUpperCase().endsWith(".FTF"))
						path += ".ftf";
				else if (descriptor != null && descriptor.equalsIgnoreCase("TXT") && !file.getName().toUpperCase().endsWith(".TXT"))
						path += ".txt";
				else if (descriptor != null && descriptor.equalsIgnoreCase("FTF&TXT") && !(file.getName().toUpperCase().endsWith(".FTF") || file.getName().toUpperCase().endsWith(".TXT")))
						path += ".txt";
				
				file = new File(path);
				
				int risp = 0;
				if (file.exists())
					risp = JOptionPane.showOptionDialog(null, file.getName() + " already exists\nDo you want overwrite it?", "Overwrite", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
				
				if (risp == 0)
				{
					documents.get(tabbedPane.getSelectedIndex()).setPanePath(file.getPath());
					
					documents.get(tabbedPane.getSelectedIndex()).saveToFile();
					saveLabel.setText("Last Save: " + documents.get(tabbedPane.getSelectedIndex()).getLastSave() + " Modified: No");
					tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(), file.getName());
				}
			}
		}
		else if (source.equals(close))
		{
			close_click();
		}
		else if (source.equals(exit))
		{
			if (documents.size() > 0)
			{
				int result = JOptionPane.showOptionDialog(this, "Are you sure to exit without saving?", "Exit", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[] { "Save", "Don't Save" ,"Cancel"}, null);
				if (result == 0)
				{
					for (int i = documents.size() -1; i >= 0; i--)
					{
						tabbedPane.setSelectedIndex(i);
						save.doClick();
						documents.remove(i);
						tabbedPane.remove(i);
					}
					System.exit(0);
				}
				else if (result == 1)
				{
					System.exit(0);
				}
			}
			else
				System.exit(0);
		}
		else if (source.equals(copy) && documents.size() > 0)
		{
			documents.get(tabbedPane.getSelectedIndex()).copy();
		}
		else if (source.equals(insertTime) && documents.size() > 0)
		{
			new TimeDialog(documents.get(tabbedPane.getSelectedIndex()));
		}
		else if (source.getClass().getName().endsWith("JCheckBoxMenuItem"))
		{
			JCheckBoxMenuItem checkbox[] = new JCheckBoxMenuItem[6];
			checkbox[0] = classic;
			checkbox[1] = system;
			checkbox[2] = motif;
			checkbox[3] = gtk;
			checkbox[4] = windows;
			checkbox[5] = nimbus;
			
			for (int i = 0; i < checkbox.length; i++)
			{
				if (!checkbox[i].equals(source))
					checkbox[i].setSelected(false);
				else
				{
					checkbox[i].setSelected(true);
				}
			}
			
			changeSkin();
			
		}
		else if (source.equals(toPDF) && documents.size() > 0)
		{
			throw new RuntimeException("NOT SUPPORTED");
		}
		else if (source.equals(bug))
		{
			new BugDialog();
		}
		else if (source.equals(paste) && documents.size() > 0)
			documents.get(tabbedPane.getSelectedIndex()).paste(true);
		else if(source.equals(cut) && documents.size() > 0)
			documents.get(tabbedPane.getSelectedIndex()).cut(true);
		else if (source.equals(find) && documents.size() > 0)
			new FindDialog(documents.get(tabbedPane.getSelectedIndex()));
		else if (source.equals(selectAll) && documents.size() > 0)
			documents.get(tabbedPane.getSelectedIndex()).selectAll();
		else if(source.equals(format) && documents.size() > 0)
			new FormatDialog(documents.get(tabbedPane.getSelectedIndex()));
		else if (source.equals(print) && documents.size() > 0)
		{
			print_click();
		}
		else if(source.equals(license))
			new LicenseDialog();
		else if (source.equals(about))
			about_click();
		else if (source.equals(copyPopup))
			copy.doClick();
		else if (source.equals(pastePopup))
			paste.doClick();
		else if (source.equals(cutPopup))
			cut.doClick();
		else if (source.equals(selectAllPopup))
			selectAll.doClick();
		else if (source.equals(formatPopup))
			format.doClick();
		else if (source.equals(buttonNew))
			newDocument.doClick();
		else if (source.equals(buttonSave))
			save.doClick();
		else if (source.equals(buttonOpen))
			open.doClick();
		else if (source.equals(buttonCopy))
			copy.doClick();
		else if (source.equals(buttonPaste))
			paste.doClick();
		else if(source.equals(buttonCut))
			cut.doClick();
		else if (source.equals(buttonFind))
			find.doClick();
		else if (source.equals(buttonPrint))
			print.doClick();
		else if (source.equals(buttonPDF))
			toPDF.doClick();
	}
	
	/**
	 * 
	 * classto implement a listener for the tabs of tabbed panel
	 * 
	 * @author Pasquale Puzio
	 *
	 */
	class TabListener implements ChangeListener 
	{
	    public void stateChanged(ChangeEvent e) 
	    {	    	
		     if (documents.size() > 0)
		     {
		    	 String mod = "";
		    	 if (documents.get(tabbedPane.getSelectedIndex()).isModified())
		    		 mod = "Yes";
		    	 else mod = "No";
		    	 saveLabel.setText("Last Save: " + documents.get(tabbedPane.getSelectedIndex()).getLastSave() + " Modified: " + mod);
		    	 setTitle("TextEditor++ " + documents.get(tabbedPane.getSelectedIndex()).getPaneName());
		     }
		     else
		     {
		    	 saveLabel.setText("");
		     }
	    }
	 }
	
	/**
	 * 
	 * class to implement a mouse listener for the popup menu
	 * 
	 * @author Pasquale Puzio
	 *
	 */
	class PopupListener extends MouseAdapter
	{
		public void mousePressed(MouseEvent e)
		{
			if (e.isPopupTrigger()) 
			{
				   textPopup.show(e.getComponent(),e.getX(), e.getY());
			}
		}
		public void mouseReleased(MouseEvent e)
		{
			if (e.isPopupTrigger()) 
			{
				   textPopup.show(e.getComponent(),e.getX(), e.getY());
			}
		}
	}

}
