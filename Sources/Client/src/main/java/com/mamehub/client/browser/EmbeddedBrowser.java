/*
 * Christopher Deckers (chrriis@nextencia.net)
 * http://www.nextencia.net
 *
 * See the file "readme.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package com.mamehub.client.browser;

import java.awt.BorderLayout;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import com.mamehub.client.Utils;

import chrriis.common.UIUtils;
import chrriis.dj.nativeswing.swtimpl.ApplicationMessageHandler;
import chrriis.dj.nativeswing.swtimpl.NativeInterface;
import chrriis.dj.nativeswing.swtimpl.NativeInterfaceListener;
import chrriis.dj.nativeswing.swtimpl.components.JWebBrowser;

public class EmbeddedBrowser extends JFrame {
	private static final long serialVersionUID = 1L;
	private boolean finished = false;

	/**
	 * @author Christopher Deckers
	 */
	public static class EmbeddedBrowserPanel extends JPanel {
		private static final long serialVersionUID = 1L;
		final JWebBrowser webBrowser;

		public EmbeddedBrowserPanel(String url) {
			super(new BorderLayout());
			JPanel webBrowserPanel = new JPanel(new BorderLayout());
			webBrowser = new JWebBrowser();
			webBrowser.navigate(url);
			webBrowser.setMenuBarVisible(false);
			webBrowser.setBarsVisible(false);
			//webBrowser.setHTMLContent("");
			webBrowserPanel.add(webBrowser, BorderLayout.CENTER);
			add(webBrowserPanel, BorderLayout.CENTER);
		}
	}

	public EmbeddedBrowser(final String url) {
		super(url);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		getContentPane().add(new EmbeddedBrowserPanel(url), BorderLayout.CENTER);
		setSize(800, 600);
		setLocationByPlatform(true);
		setVisible(true);
		Utils.windows.add(this);
		addWindowListener(new WindowListener(){
			@Override
			public void windowActivated(WindowEvent e) {
				System.out.println("WINDOW ACTIVATED");
			}

			@Override
			public void windowClosed(WindowEvent e) {
				finished = true;
				System.out.println("WINDOW CLOSED");
				Utils.removeWindow(EmbeddedBrowser.this);
			}

			@Override
			public void windowClosing(WindowEvent e) {
				System.out.println("WINDOW CLOSING");
			}

			@Override
			public void windowDeactivated(WindowEvent e) {
				System.out.println("WINDOW DEACTIVATED");
			}

			@Override
			public void windowDeiconified(WindowEvent e) {
			}

			@Override
			public void windowIconified(WindowEvent e) {
			}

			@Override
			public void windowOpened(WindowEvent e) {
				System.out.println("WINDOW OPENED");
			}
		});
	}

	public void waitUntilClosed() {
		while(!finished) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("CLOSED");
	}

	public static class BrowserMessageHandler implements ApplicationMessageHandler {

		@Override
		public void handleQuit() {
			System.out.println("GOT QUIT");
		}

	}

	public static class BrowserInterfaceListener implements NativeInterfaceListener {

		@Override
		public void nativeInterfaceClosed() {
			System.out.println("Interface closed");
		}

		@Override
		public void nativeInterfaceInitialized() {
			System.out.println("Interface initialized");

		}

		@Override
		public void nativeInterfaceOpened() {
			System.out.println("Interface opened");
		}

	}

	/* Standard main method to try that test as a standalone application. */
	public static void main(String[] args) {
		NativeInterface.setApplicationMessageHandler(new BrowserMessageHandler());
		NativeInterface.addNativeInterfaceListener(new BrowserInterfaceListener());
		NativeInterface.open();

		UIUtils.setPreferredLookAndFeel();
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new EmbeddedBrowser("file:///Users/jgauci/0000.webm");
			}
		});

		NativeInterface.runEventPump();
	}
}
