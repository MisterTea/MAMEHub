package com.mamehub.client.imageflow;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2008</p>
 *
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

public class ImageFlowItem {
  private BufferedImage image = null;
  private String label;

  private File file;

  public ImageFlowItem(String fileName, String label) {
    this(new File(fileName), label);
  }

  public ImageFlowItem(File file, String label) {
    this.file = file;
    this.label = label;
  }

  public ImageFlowItem(InputStream is, String label) throws IOException {
    this.label = label;
    loadImage(is);
  }

  public static List<ImageFlowItem> loadFromDirectory(File directory) {
    List<ImageFlowItem> list = new ArrayList<ImageFlowItem>();

    if (!directory.isDirectory()) {
      return list;
    }

    File[] files = directory.listFiles();

    for (int index = 0; index < files.length; index++) {
      if (files[index].isFile()) {
        ImageFlowItem item = new ImageFlowItem(files[index],
            files[index].getName());
        list.add(item);
      }
    }

    return list;
  }

  private void loadImage() throws FileNotFoundException, IOException {
    FileInputStream fis = new FileInputStream(file);
    loadImage(fis);
  }

  private void loadImage(InputStream is) throws FileNotFoundException,
      IOException {
    image = ImageIO.read(is);

    CrystalCaseFactory fx = CrystalCaseFactory.getInstance();
    image = fx.createReflectedPicture(fx.createCrystalCase(image));
  }

  public Image getImage() {
    if (image == null) {
      try {
        loadImage();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    return image;
  }

  public String getLabel() {
    return label;
  }

}
