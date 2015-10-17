package com.texteditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

/**
 * 
 * class for the creation and management of the text
 * 
 * @author Pasquale Puzio
 *
 */
public class TextEditorPane extends JTextArea implements Serializable {

  /**
   * if document has been saved then it's true else it's false
   */
  private boolean modified = false;

  /**
   * if document is a new empty document then it's true else it's false
   */
  private boolean alreadySaved;

  /**
   * date of last saving
   */
  private Date saved;

  private int start = 0;

  private int end = 0;

  private String prevWord;

  private String panePath = System.getProperty("user.home")
      + System.getProperty("file.separator") + "Documenti";

  private String paneName;

  /**
   * @param m
   *          reference frame
   */
  public TextEditorPane() {
    this.setText("");
    this.setEnabled(true);
    this.setBackground(Color.WHITE);
    this.setForeground(Color.BLACK);
    this.setWrapStyleWord(true);
    this.setLineWrap(true);
    this.setTabSize(3);
    setKeyListener();
  }

  /**
   * method to set already saved state
   * 
   * @param arg
   *          a boolean
   */
  public void setAlreadySaved(boolean arg) {
    alreadySaved = arg;
  }

  public void setKeyListener() {
    KeyTextListener listener = new KeyTextListener();
    this.addKeyListener(listener);
  }

  public void setMouseListener(MouseListener e) {
    this.addMouseListener(e);
  }

  /**
   * method for the text-search in the panel
   * 
   * @param word
   *          the word to search
   */
  public void findForward(String word) {
    if (prevWord == null)
      prevWord = word;

    if (!prevWord.equalsIgnoreCase(word)) {
      prevWord = word;
      start = 0;
      end = word.length();
    }

    if ((start == 0 && end == 0)) {
      start = 0;
      end = word.length();
    }

    if (end > this.getText().length())
      end = this.getText().length();

    String temp = "";
    boolean flag = false;

    while (end < this.getText().length() && !flag) {
      temp = this.getText().substring(start, end);
      if (temp.equalsIgnoreCase(word))
        flag = true;
      else {
        start++;
        end++;
      }
    }

    if (flag) {
      this.setSelectionStart(start);
      this.setSelectionEnd(end);
      start++;
      end++;
    } else if (this.getText().substring(start, end).equalsIgnoreCase(word)) {
      this.setSelectionStart(start);
      this.setSelectionEnd(end);
    } else {
      JOptionPane.showMessageDialog(this, word
          + " doesn't exist in this document", "Sorry",
          JOptionPane.INFORMATION_MESSAGE);
    }

  }

  /**
   * method to verify if clipboard content is empty
   * 
   * @param arg
   *          a boolean
   */
  public void paste(boolean arg) {
    String text = getText();
    paste();
    if (arg && !text.equalsIgnoreCase(getText()))
      modified = true;
  }

  /**
   * method to set a modification of the content
   * 
   * @param arg
   *          a boolean
   */
  public void cut(boolean arg) {
    cut();
    if (arg)
      modified = true;
  }

  /**
   * method for the text-search in reverse direction
   * 
   * @param word
   *          word to search
   */
  public void findBack(String word) {
    if (prevWord == null)
      prevWord = word;

    if (!prevWord.equalsIgnoreCase(word)) {
      prevWord = word;
      start = this.getText().length() - word.length();
      end = this.getText().length();
    }

    if ((start == 0 && end == 0)) {
      start = this.getText().length() - word.length();
      end = this.getText().length();
    }

    String temp = "";
    boolean flag = false;

    while (start > 0 && !flag) {
      temp = this.getText().substring(start, end);
      if (temp.equalsIgnoreCase(word))
        flag = true;
      else {
        start--;
        end--;
      }
    }

    if (flag) {
      this.setSelectionStart(start);
      this.setSelectionEnd(end);
      start--;
      end--;
    } else if (this.getText().substring(start, end).equalsIgnoreCase(word)) {
      this.setSelectionStart(start);
      this.setSelectionEnd(end);
    } else {
      JOptionPane.showMessageDialog(this, word
          + " doesn't exist in this document", "Sorry",
          JOptionPane.INFORMATION_MESSAGE);
    }
  }

  /**
   * reset the text-search attributes
   */
  public void resetFind() {
    start = 0;
    end = 0;
    prevWord = null;
  }

  /**
   * method to set panel name
   * 
   * @param name
   *          new panel name
   */
  public void setPaneName(String name) {
    paneName = name;
  }

  /**
   * method to set panel path
   * 
   * @param name
   *          new panel path
   */
  public void setPanePath(String path) {
    panePath = (new File(path).getParent());
    setPaneName(new File(path).getName());
  }

  /**
   * method to save the panel content in a file
   * 
   * @return if there is an error return false else return true
   */
  public boolean saveToFile() {
    File outFile = new File(panePath + System.getProperty("file.separator")
        + paneName);

    if (paneName.toUpperCase().endsWith(".TXT")
        || !paneName.toUpperCase().endsWith(".FTF")) {
      ArrayList<String> lines = getLinesToSave(getText());
      try {
        FileWriter outFileWriter = new FileWriter(outFile);
        PrintWriter printWriter = new PrintWriter(outFileWriter);

        for (int i = 0; i < lines.size(); i++) {
          printWriter.println(lines.get(i));
        }

        printWriter.close();
        alreadySaved = true;
        modified = false;
        updateLastSave();
        return true;
      } catch (IOException e) {
        return false;
      }
    } else if (paneName.toUpperCase().endsWith(".FTF")) {
      try {
        FileOutputStream outFileStream = new FileOutputStream(outFile);
        ObjectOutputStream outObjectStream = new ObjectOutputStream(
            outFileStream);

        alreadySaved = true;
        modified = false;
        updateLastSave();
        outObjectStream.writeObject(this);

        outObjectStream.close();
        return true;

      } catch (FileNotFoundException e) {
        return false;
      } catch (IOException e) {
        System.out.println("NOOOO");
        return false;
      }
    } else
      return false;
  }

  public boolean isAlreadySaved() {
    return alreadySaved;
  }

  public boolean isModified() {
    return modified;
  }

  public void setFormat(Font font, Color color) {
    this.setFont(font);
    this.setForeground(color);
    modified = true;
  }

  public String getPaneName() {
    return paneName;
  }

  public String getPanePath() {
    return panePath;
  }

  /**
   * method to update last saving
   */
  public void updateLastSave() {
    saved = Calendar.getInstance().getTime();
  }

  public String getLastSave() {
    if (saved != null) {
      SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm:ss");
      return (sdf.format(saved));
    } else
      return "none";
  }

  /**
   * method to print panel content
   */
  public void printPane() {
    JDialog option = new JDialog();
    option.setTitle("Print");
    option.add(new JLabel("Printing...", SwingConstants.CENTER), BorderLayout.CENTER);
    option.setSize(200, 100);
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x, y;
    y = (screenSize.height / 2) - (option.getHeight() / 2);
    x = (screenSize.width / 2) - (option.getWidth() / 2);
    option.setLocation(x, y);
    option.setResizable(false);
    option.setModal(false);
    option.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    option.setAlwaysOnTop(true);
    option.setVisible(true);

    PrinterJob pj = PrinterJob.getPrinterJob();
    pj.setJobName(this.getPaneName());

    Paper foglio = new Paper();
    PageFormat formato = pj.defaultPage();
    formato.setPaper(foglio);

    Book pages = new Book();
    ArrayList<String> lines = getLinesToPrint(this.getText());
    int n = (int) (pj.defaultPage().getImageableHeight() - pj.defaultPage()
        .getImageableY()) / getFont().getSize();
    Vector v = new Vector();
    for (int i = 0; i < lines.size(); i++) {
      v.add(lines.get(i));
      if (i % n == 0 && i != 0) {
        pages.append(new Page(v, this), formato);
        v = new Vector();
      }
    }
    pages.append(new Page(v, this), formato);
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
              pj.setJobName(this.getPaneName() + String.valueOf(i));
              pj.print();
            }
          }
        } catch (PrinterException e) {
          JOptionPane.showMessageDialog(null,
              "Error in the printing of the document", "Print Error",
              JOptionPane.ERROR_MESSAGE);
        }
      }
    } catch (Exception e) {
      try {
        pj.print();
      } catch (PrinterException e1) {
        JOptionPane.showMessageDialog(null,
            "Error in the printing of the document", "Print Error",
            JOptionPane.ERROR_MESSAGE);
      }
    }

    option.dispose();
  }

  /**
   * method to obtain from the panel all the lines to save
   * 
   * @param atext
   *          panel content
   * @return an ArrayList with all the lines of panel content
   */
  private ArrayList<String> getLinesToSave(String atext) {
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

  /**
   * method to obtain from the panel all the lines to print
   * 
   * @param atext
   *          panel content
   * @return an ArrayList with all the lines of panel content
   */
  private ArrayList<String> getLinesToPrint(String atext) {
    // procedura per scindere la righe
    String text = atext + "\n";
    char[] letters = text.toCharArray();
    ArrayList<String> lines = new ArrayList<String>();
    String temp = "";
    double width = PrinterJob.getPrinterJob().defaultPage().getImageableWidth();
    int i = 0;
    while (i < letters.length) {
      if (getFontMetrics(getFont()).charsWidth(temp.toCharArray(), 0,
          temp.toCharArray().length)
          + getFontMetrics(getFont()).charWidth(letters[i]) > width) {
        lines.add(temp);
        temp = "";
        temp += letters[i];
      } else if (String.valueOf(letters[i]).hashCode() == 10) {
        lines.add(temp);
        temp = "";
      } else {
        temp = temp.concat(String.valueOf(letters[i]));
      }
      i++;
    }
    return lines;
  }

  /**
   * class to implement a keyboard listener
   * 
   * @author Pasquale Puzio
   *
   */
  class KeyTextListener extends KeyAdapter {
    public KeyTextListener() {
    }

    @Override
    public void keyPressed(KeyEvent e) {
      int code = e.getKeyCode();

      if (controlPress(code))
        modified = true;
    }

    private boolean controlPress(int code) {
      if (code != KeyEvent.VK_ALT && code != KeyEvent.VK_ALT_GRAPH
          && code != KeyEvent.VK_CAPS_LOCK && code != KeyEvent.VK_CONTROL
          && code != KeyEvent.VK_DOWN && code != KeyEvent.VK_LEFT
          && code != KeyEvent.VK_UP && code != KeyEvent.VK_RIGHT
          && code != KeyEvent.VK_WINDOWS && code != KeyEvent.VK_SHIFT
          && code != KeyEvent.VK_END && code != KeyEvent.VK_PAGE_DOWN
          && code != KeyEvent.VK_PAGE_UP && code != KeyEvent.VK_PRINTSCREEN
          && code != KeyEvent.VK_ESCAPE && code != KeyEvent.VK_INSERT
          && code != KeyEvent.VK_PAUSE && code != KeyEvent.VK_NUM_LOCK
          && code != KeyEvent.VK_F1 && code != KeyEvent.VK_F2
          && code != KeyEvent.VK_F3 && code != KeyEvent.VK_F4
          && code != KeyEvent.VK_F5 && code != KeyEvent.VK_F6
          && code != KeyEvent.VK_F7 && code != KeyEvent.VK_F8
          && code != KeyEvent.VK_F9 && code != KeyEvent.VK_F10
          && code != KeyEvent.VK_F11 && code != KeyEvent.VK_F12 && code != 36
          && code != 0 && code != 525)
        return true;
      else
        return false;
    }
  }
}
