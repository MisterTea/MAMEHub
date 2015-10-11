package com.mamehub.client;

import java.awt.Adjustable;
import java.io.IOException;
import java.io.Writer;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class TextAreaWriter extends Writer {
  StringBuilder buffer;
  private JTextArea textArea;
  private JScrollPane scrollPane;

  public TextAreaWriter(JTextArea textArea, JScrollPane scrollPane) {
    this.textArea = textArea;
    this.scrollPane = scrollPane;
    buffer = new StringBuilder();
  }

  @Override
  public void write(char[] cbuf, int off, int len) throws IOException {
    // Check for newlines
    for (int i = 0; i < len; i++) {
      if (cbuf[off + i] == '\n') {
        writeLine(buffer.toString() + new String(cbuf, off, i));
        buffer.setLength(0);

        off = off + i + 1;
        len = (len - i) - 1;
      }
    }
    buffer = buffer.append(cbuf, off, len);
  }

  private boolean isAtBottom() {
    // Is the last line of text the last line of text visible?
    Adjustable sb = scrollPane.getVerticalScrollBar();

    int val = sb.getValue();
    int lowest = val + sb.getVisibleAmount();
    int maxVal = sb.getMaximum();

    boolean atBottom = (maxVal <= (lowest + 50));
    return atBottom;
  }

  private void scrollToBottom() {
    textArea.setCaretPosition(textArea.getDocument().getLength());
  }

  private void writeLine(final String line) {
    try {
      java.awt.EventQueue.invokeLater(new Runnable() {

        @Override
        public void run() {
          try {
            boolean atBottom = isAtBottom();
            textArea.insert(line + "\n", textArea.getDocument().getLength());
            if (atBottom) {
              scrollToBottom();
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      });
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void flush() throws IOException {
    writeLine(buffer.toString());
    buffer.setLength(0);
  }

  @Override
  public void close() throws IOException {
  }
}
