package com.texteditor;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 
 * class for the creation and visualization of a dialog
 * with formatting features (size, style and color)
 * 
 * @author Pasquale Puzio
 *
 */
public class FormatDialog extends JDialog implements ActionListener {
	
	private JLabel labelFont, labelSize, labelStyle, labelColor;
	
	private JComboBox comboSize, comboFont, comboStyle;
	
	private JButton buttonDone, buttonCancel, buttonApply , buttonColor;
	
	private TextEditorPane textPane;
	
	public FormatDialog(TextEditorPane pane)
	{
		textPane = pane;
		setTitle("Format Options");
		setSize(320, 300);
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		createComponent();
		fontFromPane();
		this.setResizable(false);
		this.setModal(true);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int x, y;
		y = (screenSize.height/2) - (getHeight()/2);
		x = (screenSize.width/2) - (getWidth()/2);
		setLocation(x, y);
		this.setVisible(true);
	}
	
	/**
	 * method for the creation and visualization of all the components
	 */
	private void createComponent()
	{
		this.setLayout(new GridLayout(10, 2));
		Container container = this.getContentPane();
		container.add(new JLabel(""));
		container.add(new JLabel(""));
		labelFont = new JLabel("Font", JLabel.CENTER);
		container.add(labelFont);
		comboFont = new JComboBox(this.getToolkit().getFontList());
		container.add(comboFont);
		container.add(new JLabel(""));
		container.add(new JLabel(""));
		labelSize = new JLabel("Size", JLabel.CENTER);
		container.add(labelSize);
		comboSize = new JComboBox();
			comboSize.addItem(8);
			comboSize.addItem(10);
			comboSize.addItem(11);
			comboSize.addItem(12);
			comboSize.addItem(13);
			comboSize.addItem(14);
			comboSize.addItem(16);
			comboSize.addItem(18);
			comboSize.addItem(20);
			comboSize.addItem(22);
			comboSize.addItem(24);
			comboSize.addItem(28);
			comboSize.addItem(32);
		container.add(comboSize);
		container.add(new JLabel(""));
		container.add(new JLabel(""));
		labelStyle = new JLabel("Style", JLabel.CENTER);
		container.add(labelStyle);
		comboStyle = new JComboBox(new String[] {"Normale" ,"Grassetto", "Corsivo"});
		container.add(comboStyle);
		container.add(new JLabel(""));
		container.add(new JLabel(""));
		buttonColor = new JButton("Color");
		buttonColor.addActionListener(this);
		container.add(buttonColor);
		labelColor = new JLabel("");
		labelColor.setOpaque(true);
		container.add(labelColor);
		container.add(new JLabel(""));
		container.add(new JLabel(""));
		buttonDone = new JButton("Done");
		buttonDone.addActionListener(this);
		buttonCancel = new JButton("Cancel");
		buttonCancel.addActionListener(this);
		buttonApply = new JButton("Apply");
		buttonApply.addActionListener(this);
		JPanel panel = new JPanel(new GridLayout(1,2));
		panel.add(new JLabel(""));
		panel.add(buttonDone);
		container.add(panel);
		JPanel panel2 = new JPanel(new GridLayout(1,2));
		panel2.add(buttonApply);
		panel2.add(buttonCancel);
		container.add(panel2);
	}
	
	/**
	 * method to obtain settings from associated TextEditorPane
	 */
	private void fontFromPane()
	{
		comboFont.setSelectedItem(textPane.getFont().getName());
		
		comboSize.setSelectedItem(textPane.getFont().getSize());
		comboStyle.setSelectedIndex(textPane.getFont().getStyle());
		
		labelColor.setBackground(textPane.getForeground());
	}
	
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();
		if (source.equals(buttonCancel))
		{
			this.dispose();
		}
		else if (source.equals(buttonColor))
		{
			JColorChooser chooser = new JColorChooser();
			Color color = chooser.showDialog(this, "Choose the Color", labelColor.getBackground());
			if (color != null)
				labelColor.setBackground(color);
		}
		else if (source.equals(buttonDone) || source.equals(buttonApply))
		{
			textPane.setFormat(new Font(comboFont.getSelectedItem().toString() , comboStyle.getSelectedIndex() ,Integer.parseInt(comboSize.getSelectedItem().toString())), labelColor.getBackground());
			if (source.equals(buttonDone))
				this.dispose();
		}
			
	}

}
