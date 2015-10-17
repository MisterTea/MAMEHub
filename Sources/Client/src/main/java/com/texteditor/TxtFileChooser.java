package com.texteditor;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * 
 * class for the creation of a specific file chooser for the FTF format
 * 
 * @author Pasquale Puzio
 *
 */
public class TxtFileChooser extends JFileChooser {

  public TxtFileChooser(String path) {
    setCurrentDirectory(new File(path));
    setSize(300, 300);
    addChoosableFileFilter(new FileNameExtensionFilter("Txt Files (*.txt)",
        new String[] { "txt" }));
    addChoosableFileFilter(new FileNameExtensionFilter(
        "Formatted Text Files (*.ftf)", new String[] { "ftf" }));
    addChoosableFileFilter(new FileNameExtensionFilter(
        "All supported Files (*.txt *.ftf)", new String[] { "ftf", "txt" }));
  }

}
