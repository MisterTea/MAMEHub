package com.texteditor;

import java.io.File;

/**
 * 
 * @author Pasquale Puzio
 * 
 * @version 1.0.0
 * 
 * TextEditor++ versione 1.0.0
 * 
 * licenza GPL2 - progetto Open Source
 *
 */
public class TextEditor
{

	public static void main(String[] args) 
	{
			MainFrame frame = new MainFrame();
			if (args.length > 0)
			{
				for (int i = 0; i < args.length; i++)
				{
					frame.addDocument(new File(args[i]));
				}
			}
			else
			{
				frame.insertNewDocument();
			}
			
			frame.setVisible(true);
	}
}
