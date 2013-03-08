package com.texteditor;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;

/**
 * 
 * class for the creation and visualization of a dialog
 * with "insert date and time" feature
 * 
 * @author Pasquale Puzio
 *
 */
public class TimeDialog extends JDialog implements ActionListener {
	
	private TextEditorPane textPane;
	
	private JLabel timeLabel;
	
	private JComboBox comboTime;
	
	private JButton okButton, cancelButton;
	
	public TimeDialog (TextEditorPane arg)
	{
		textPane = arg;
		setTitle("Insert Date & Time");
		setSize(350, 150);
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		createComponent();
		this.setResizable(false);
		this.setModal(true);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int x, y;
		y = (screenSize.height/2) - (getHeight()/2);
		x = (screenSize.width/2) - (getWidth()/2);
		setLocation(x, y);
		this.setVisible(true);
	}
	
	private void createComponent()
	{
		timeLabel = new JLabel("Select a format", JLabel.CENTER);
		comboTime = new JComboBox(new String[] {"dd/MM/yy HH:mm:ss", "dd/MM/yy HH:mm" , "dd/MM HH:mm:ss", "dd/MM HH:mm", "dd/MM/yy", "HH:mm:ss", "HH:mm"});
		okButton = new JButton("OK");
			okButton.addActionListener(this);
		cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(this);
		this.setLayout(new GridLayout(4,2));
		this.add(new JLabel());
		this.add(new JLabel());
		this.add(timeLabel);
		this.add(comboTime);
		this.add(new JLabel());
		this.add(new JLabel());
		this.add(okButton);
		this.add(cancelButton);
	}
	
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();
		if (source.equals(okButton))
		{
			Date time = Calendar.getInstance().getTime();
			SimpleDateFormat sdf = new SimpleDateFormat(comboTime.getSelectedItem().toString());
			String formatted = sdf.format(time);
			String text = textPane.getText();
			int position = textPane.getCaretPosition();
			textPane.setText(text.substring(0, position) + formatted + text.substring(position, text.length()));
			this.dispose();
		}
		else if (source.equals(cancelButton))
			this.dispose();
	}

}
