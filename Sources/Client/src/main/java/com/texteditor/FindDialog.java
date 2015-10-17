package com.texteditor;

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

/**
 * 
 * class for the creation and visualization of the dialog with find and replace
 * features
 * 
 * @author Pasquale Puzio
 *
 */
public class FindDialog extends JDialog implements ActionListener {

  private JButton buttonFind, buttonReplace, buttonCancel;

  private JLabel labelFind, labelReplace;

  private JTextField textFind, textReplace;

  private TextEditorPane textPane;

  private Checkbox back, forward;

  public FindDialog(TextEditorPane pane) {
    textPane = pane;
    setTitle("Find");
    setSize(300, 200);
    this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    createComponent();
    this.setResizable(false);
    this.setModal(false);
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x, y;
    y = (screenSize.height / 2) - (getHeight() / 2);
    x = (screenSize.width / 2) - (getWidth() / 2);
    setLocation(x, y);
    this.addWindowListener(new FindDialogListener());
    this.setVisible(true);
  }

  /**
   * method for the creation and visualization of all the components
   */
  private void createComponent() {
    this.setLayout(new GridLayout(6, 2));
    labelFind = new JLabel("Find", SwingConstants.CENTER);
    this.getContentPane().add(labelFind);
    textFind = new JTextField("");
    this.getContentPane().add(textFind);
    labelReplace = new JLabel("Replace With", SwingConstants.CENTER);
    this.getContentPane().add(labelReplace);
    textReplace = new JTextField("");
    this.getContentPane().add(textReplace);
    this.getContentPane().add(new JLabel(""));
    this.getContentPane().add(new JLabel(""));
    CheckboxGroup group = new CheckboxGroup();
    back = new Checkbox("Back", group, false);
    forward = new Checkbox("Forward", group, true);
    this.getContentPane().add(back);
    this.getContentPane().add(forward);
    buttonReplace = new JButton("Replace");
    buttonReplace.addActionListener(this);
    this.getContentPane().add(buttonReplace);
    this.getContentPane().add(new JLabel(""));
    buttonFind = new JButton("Find");
    buttonFind.addActionListener(this);
    this.getContentPane().add(buttonFind);
    buttonCancel = new JButton("Cancel");
    buttonCancel.addActionListener(this);
    this.getContentPane().add(buttonCancel);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();
    if (source.equals(buttonCancel)) {
      textPane.resetFind();
      this.dispose();
    } else if (source.equals(buttonFind)) {
      if (!textFind.getText().equalsIgnoreCase("")) {
        if (back.getState())
          textPane.findBack(textFind.getText());
        else
          textPane.findForward(textFind.getText());
      } else
        JOptionPane.showMessageDialog(this,
            "You must insert some word/s in the TextField Find", "Error",
            JOptionPane.ERROR_MESSAGE);
    } else if (source.equals(buttonReplace)) {
      if (textReplace.getText().equalsIgnoreCase(""))
        JOptionPane.showMessageDialog(this,
            "You must insert some word/s in the TextField Replace", "Error",
            JOptionPane.ERROR_MESSAGE);
      else if (textPane.getSelectedText() == null) {
        JOptionPane.showMessageDialog(this, "You must select some text",
            "Error", JOptionPane.ERROR_MESSAGE);
      } else {
        textPane.replaceSelection(textReplace.getText());
      }

    }
  }

  class FindDialogListener extends WindowAdapter {
    public FindDialogListener() {
    }

    @Override
    public void windowClosing(WindowEvent e) {
      textPane.resetFind();
    }
  }
}
