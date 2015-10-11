package com.mamehub.client;

import java.awt.Adjustable;
import java.io.IOException;
import java.io.Writer;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

public class HtmlEditorKitTextWriter extends Writer {
  StringBuilder buffer;
  private HTMLEditorKit editorKit;
  private HTMLDocument document;
  private JScrollPane scrollPane;
  private JEditorPane chatTextArea;

  public HtmlEditorKitTextWriter(HTMLDocument document,
      HTMLEditorKit editorKit, JScrollPane scrollPane, JEditorPane chatTextArea) {
    this.document = document;
    this.editorKit = editorKit;
    this.scrollPane = scrollPane;
    this.chatTextArea = chatTextArea;
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
    chatTextArea.setCaretPosition(chatTextArea.getDocument().getLength());
  }

  private void writeLine(final String line) {
    try {
      java.awt.EventQueue.invokeLater(new Runnable() {

        @Override
        public void run() {
          try {
            boolean atBottom = isAtBottom();
            editorKit.insertHTML(document, document.getLength(), line, 0, 0,
                null);
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
