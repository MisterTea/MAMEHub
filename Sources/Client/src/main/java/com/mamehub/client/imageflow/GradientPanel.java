package com.mamehub.client.imageflow;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

public class GradientPanel extends JPanel {
  protected BufferedImage gradientImage;
  protected Color gradientStart = new Color(110, 110, 110);
  protected Color gradientEnd = new Color(0, 0, 0);

  public GradientPanel() {
    this(new BorderLayout());
  }

  public GradientPanel(LayoutManager layout) {
    super(layout);
    addComponentListener(new GradientCacheManager());
  }

  @Override
  protected void paintComponent(Graphics g) {
    createImageCache();

    if (gradientImage != null) {
      Shape clip = g.getClip();
      Rectangle bounds = clip.getBounds();

      Image backgroundImage = gradientImage.getSubimage(bounds.x, bounds.y,
          bounds.width, bounds.height);
      g.drawImage(backgroundImage, bounds.x, bounds.y, null);
    }
  }

  protected void createImageCache() {
    int width = getWidth();
    int height = getHeight();

    if (width == 0 || height == 0) {
      return;
    }

    if (gradientImage == null || width != gradientImage.getWidth()
        || height != gradientImage.getHeight()) {

      gradientImage = new BufferedImage(width, height,
          BufferedImage.TYPE_INT_RGB);

      Graphics2D g2 = gradientImage.createGraphics();
      GradientPaint painter = new GradientPaint(0, 0, gradientEnd, 0,
          height / 2, gradientStart);
      g2.setPaint(painter);
      Rectangle2D rect = new Rectangle2D.Double(0, 0, width, height / 2.0);
      g2.fill(rect);

      painter = new GradientPaint(0, height / 2, gradientStart, 0, height,
          gradientEnd);
      g2.setPaint(painter);
      rect = new Rectangle2D.Double(0, (height / 2.0) - 1.0, width, height);
      g2.fill(rect);

      g2.dispose();
    }
  }

  private void disposeImageCache() {
    synchronized (gradientImage) {
      gradientImage.flush();
      gradientImage = null;
    }
  }

  private class GradientCacheManager implements ComponentListener {
    @Override
    public void componentResized(ComponentEvent e) {
    }

    @Override
    public void componentMoved(ComponentEvent e) {
    }

    @Override
    public void componentShown(ComponentEvent e) {
    }

    @Override
    public void componentHidden(ComponentEvent e) {
      disposeImageCache();
    }

  }
}
