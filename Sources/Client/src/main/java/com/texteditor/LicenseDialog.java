package com.texteditor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

/**
 * 
 * @author Pasquale Puzio
 *
 */
public class LicenseDialog extends JDialog {

  private static final String LICENSE = "license/gpl.txt";

  private static final Dimension DIALOG_SIZE = new Dimension(500, 600);

  private JButton okButton, printButton;

  private JTextArea license;

  public LicenseDialog() {
    setTitle("License");
    setSize(DIALOG_SIZE);
    // code to centre this dialog on the screen
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x, y;
    y = (screenSize.height / 2) - (getHeight() / 2);
    x = (screenSize.width / 2) - (getWidth() / 2);
    setLocation(x, y);
    setModal(true);
    setResizable(false);
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    createComponent();
    this.setVisible(true);
  }

  private void createComponent() {
    license = new JTextArea();
    license.setWrapStyleWord(true);
    license.setLineWrap(true);
    license.setEditable(false);

    // code to read the license from the defined input file
    String result = "";
    try {
      InputStream is = getClass().getResourceAsStream(LICENSE);
      InputStreamReader isr = new InputStreamReader(is);
      BufferedReader br = new BufferedReader(isr);
      String inputLine;

      inputLine = br.readLine();

      while (inputLine != null) {
        result += inputLine + "\n";
        inputLine = br.readLine();
      }

      is.close();
      isr.close();
      br.close();
    } catch (IOException e) {
      result = "Errore durante la lettura del file";
    }
    ;

    license.setText(result);
    license.setCaretPosition(0);
    JScrollPane scroll = new JScrollPane(license);
    add(scroll, BorderLayout.CENTER);
    okButton = new JButton("OK");
    // definizione dell'azione associata alla pressione di okButton
    okButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dispose();
      }
    });

    printButton = new JButton("Print");
    // definizione dell'azione associata alla pressione di okButton
    printButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int risp = JOptionPane.showOptionDialog(null,
            "Are you sure to print the license?", "Print",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
            null, null);

        if (risp == 0) {
          // code to print the license
          PrinterJob pj = PrinterJob.getPrinterJob();
          pj.setJobName("License");

          Paper foglio = new Paper();
          PageFormat formato = pj.defaultPage();
          formato.setPaper(foglio);

          Book pages = new Book();
          ArrayList<String> lines = getLines(license.getText());
          int n = (int) (pj.defaultPage().getImageableHeight() - pj
              .defaultPage().getImageableY()) / license.getFont().getSize();
          Vector v = new Vector();
          for (int i = 0; i < lines.size(); i++) {
            v.add(lines.get(i));
            if (i % n == 0 && i != 0) {
              pages.append(new Page(v, license), formato);
              v = new Vector();
            }
          }
          pages.append(new Page(v, license), pj.defaultPage());
          pj.setPageable(pages);
          int copies = 1;

          try {
            if (pj.printDialog()) {
              copies = pj.getCopies();
              try {
                if (copies == 1)
                  pj.print();
                else {
                  for (int i = 1; i <= pj.getCopies(); i++) {
                    pj.setJobName("License" + String.valueOf(i));
                    pj.print();
                  }
                }
              } catch (PrinterException e1) {
                JOptionPane.showMessageDialog(null,
                    "Error in the printing of the license", "Print Error",
                    JOptionPane.ERROR_MESSAGE);
              }
            }
          } catch (Exception e1) {
            try {
              pj.print();
            } catch (PrinterException e2) {
              JOptionPane.showMessageDialog(null,
                  "Error in the printing of the license", "Print Error",
                  JOptionPane.ERROR_MESSAGE);
            }
          }
        }
      }

      private ArrayList<String> getLines(String atext) {
        // procedura per scindere la righe
        String text = atext + "\n";
        char[] letters = text.toCharArray();
        ArrayList<String> lines = new ArrayList<String>();
        String temp = "";
        int i = 0;
        while (i < letters.length) {
          if (String.valueOf(letters[i]).hashCode() == 10) {
            lines.add(temp);
            temp = "";
          } else {
            temp = temp.concat(String.valueOf(letters[i]));
          }
          i++;
        }
        return lines;
      }
    });

    JPanel panel = new JPanel(new GridLayout(1, 3));
    panel.add(printButton);
    panel.add(new JLabel());
    panel.add(okButton);
    add(panel, BorderLayout.SOUTH);
  }

}
