package com.texteditor;

import java.awt.Graphics;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.util.Vector;

import javax.swing.JTextArea;

/**
 * 
 * class to print more pages of a document
 * 
 * @author Pasquale Puzio
 *
 */
public class Page implements Printable {
  private Vector testoDaStampare;

  private JTextArea textarea;

  public Page(Vector testoDaStampare, JTextArea textarea) {
    this.testoDaStampare = testoDaStampare;
    this.textarea = textarea;
  }

  @Override
  public int print(Graphics g, PageFormat pageFormat, int pageIndex)
      throws PrinterException {
    g.setColor(textarea.getForeground());
    g.setFont(textarea.getFont());

    for (int i = 1; i <= testoDaStampare.size(); i++) {
      g.drawString(testoDaStampare.get(i - 1).toString(),
          (int) pageFormat.getImageableX(), (int) pageFormat.getImageableY()
              + i * textarea.getFont().getSize());
    }

    return PAGE_EXISTS;
  }
}