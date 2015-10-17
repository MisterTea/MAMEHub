package com.mamehub.client;

/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import static java.awt.GraphicsDevice.WindowTranslucency.TRANSLUCENT;

import java.awt.BorderLayout;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

public class NotifyFrame extends JFrame implements Runnable {
  private static final long serialVersionUID = 1L;
  private static Set<NotifyFrame> renderingFrames = new HashSet<NotifyFrame>();

  public NotifyFrame(String messageHeader, String messageBody) {
    super("TranslucentWindow");
    setUndecorated(true);
    setLayout(new BorderLayout());

    setSize(500, 50);
    setLocation(50, 50);
    setOpacityIfPossible(0.0f);
    setAlwaysOnTop(true);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setFocusableWindowState(false);
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        setFocusableWindowState(false);
      }
    });

    // Add a sample button.
    JLabel header = new JLabel(messageHeader);
    add(header, BorderLayout.NORTH);
    add(new JLabel(messageBody), BorderLayout.CENTER);
    setVisible(true);

    new Thread(this).start();
  }

  @Override
  public void run() {
    try {
      while (true) {
        synchronized (renderingFrames) {
          if (renderingFrames.isEmpty()) {
            renderingFrames.add(this);
            break;
          }
        }
        Thread.sleep(10);
      }
      long curtime = System.currentTimeMillis();
      long endtime = curtime + 5000;
      while (endtime > System.currentTimeMillis()) {
        long deltatime = System.currentTimeMillis() - curtime;
        final float opacity = (float) Math.min(
            1.0,
            Math.abs(2.0 * Math.sin(Math.PI * (deltatime)
                / ((endtime - curtime)))));
        SwingUtilities.invokeLater(new Runnable() {

          @Override
          public void run() {
            setOpacityIfPossible(opacity);
            setFocusableWindowState(false);
          }
        });
        Thread.sleep(10);
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          synchronized (renderingFrames) {
            renderingFrames.remove(NotifyFrame.this);
          }
          NotifyFrame.this.dispose();
        }
      });
    }
  }

  private void setOpacityIfPossible(float opacity) {
    // Determine if the GraphicsDevice supports translucency.
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice gd = ge.getDefaultScreenDevice();

    if (gd.isWindowTranslucencySupported(TRANSLUCENT)) {
      setOpacity(opacity);
    }
  }

  public static void main(String[] args) {
    // Determine if the GraphicsDevice supports translucency.
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice gd = ge.getDefaultScreenDevice();

    // If translucent windows aren't supported, exit.
    if (!gd.isWindowTranslucencySupported(TRANSLUCENT)) {
      System.err.println("Translucency is not supported");
      System.exit(0);
    }

    // Create the GUI on the event-dispatching thread
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        for (int a = 0; a < 10; a++) {
          NotifyFrame tw = new NotifyFrame(
              "MAMEHub Chat",
              String
                  .format(
                      "<html><body><font size='4' color='red'>%s</font><font size='4'>: %s</font></body></html>",
                      "Digitalghost",
                      "Hey Guys!Hey Guys!Hey Guys!Hey Guys!Hey Guys!Hey Guys!Hey Guys!Hey Guys!Hey Guys!Hey Guys!Hey Guys!Hey Guys!Hey Guys!Hey Guys!"
                          .substring(0, 50) + "..."));
        }
      }
    });
  }
}