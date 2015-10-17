package com.texteditor;

import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;

/**
 * 
 * @author Pasquale Puzio
 *
 */
public class AboutDialog extends JDialog {
  public AboutDialog() {
    setModal(false);
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setTitle("About TextEditor++");
    createComponent();
    // code to centre this dialog on the screen
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x, y;
    y = (screenSize.height / 2) - (getHeight() / 2);
    x = (screenSize.width / 2) - (getWidth() / 2);
    setLocation(x, y);
    setResizable(false);
    setVisible(true);
  }

  /**
   * method for the creation and visualization of all the components
   */
  private void createComponent() {
    ImageIcon image = new ImageIcon(getClass().getResource("images/logo.jpg"));
    setSize(image.getIconWidth(), image.getIconHeight());
    JLabel label = new JLabel(image);
    add(label);
  }
}
