/*
 * JFlow
 * Created by Tim De Pauw <http://pwnt.be/>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package be.pwnt.jflow.demo;

import java.awt.event.MouseEvent;

import javax.swing.JApplet;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import be.pwnt.jflow.JFlowPanel;
import be.pwnt.jflow.event.ShapeEvent;
import be.pwnt.jflow.event.ShapeListener;

@SuppressWarnings("serial")
public class Applet extends JApplet implements ShapeListener {
	public void init() {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					try {
						UIManager.setLookAndFeel(UIManager
								.getSystemLookAndFeelClassName());
					} catch (Exception e) {
					}
					final JFlowPanel panel = new JFlowPanel(new Configuration());
					panel.addListener(Applet.this);
					getContentPane().add(panel);
				}
			});
		} catch (Exception e) {
		}
	}

	@Override
	public void shapeClicked(ShapeEvent e) {
		MouseEvent me = e.getMouseEvent();
		if (!me.isConsumed() && me.getButton() == MouseEvent.BUTTON1
				&& me.getClickCount() == 1) {
			JOptionPane.showMessageDialog(this,
					"You clicked on " + e.getShape() + ".", "Event Test",
					JOptionPane.INFORMATION_MESSAGE);
		}
	}

	@Override
	public void shapeActivated(ShapeEvent e) {
	}

	@Override
	public void shapeDeactivated(ShapeEvent e) {
	}
}
