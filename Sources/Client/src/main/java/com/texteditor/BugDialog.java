package com.texteditor;

import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JDialog;
import javax.swing.JEditorPane;

/**
 * 
 * @author Pasquale Puzio
 *
 */
public class BugDialog extends JDialog {

  private JEditorPane pane;

  public BugDialog() {
    setTitle("Bug Report and more info");
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setResizable(false);
    setSize(300, 200);
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x, y;
    y = (screenSize.height / 2) - (getHeight() / 2);
    x = (screenSize.width / 2) - (getWidth() / 2);
    setLocation(x, y);
    createComponent();
    setVisible(true);
  }

  /**
   * method for the creation and visualization of all the components
   */
  private void createComponent() {
    pane = new JEditorPane();
    pane.setEditable(false);
    pane.setContentType("text/html");
    String url = "<p align=center>Please visit official web site of the project: <a href='http://jtexteditor.sourceforge.net'><b>jtexteditor.sourceforge.net</b></a></p><p align=center>For more information, suggestions and bug reports you can contact me at "
        + "<a href="
        + "mailto: pasquale.puzio@gmail.com"
        + ">pasquale.puzio@gmail.com</a>"
        + ".</p>"
        + "<p align=center>Thank you for the collaboration.</p>";
    pane.setText(url);
    add(pane);
  }

}
