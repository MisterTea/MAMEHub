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

package be.pwnt.jflow.shape;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.imageio.ImageIO;

import be.pwnt.jflow.Configuration;
import be.pwnt.jflow.Scene;
import be.pwnt.jflow.geometry.Point3D;
import be.pwnt.jflow.geometry.RotationMatrix;

public class Picture extends Rectangle {
	private BufferedImage image;

	private Point3D[] projectedPoints;

	public Picture(BufferedImage image) {
		super(new Point3D(0, 0, 0), new Point3D(0, 0, 0), Color.black);
		this.image = image;
		projectedPoints = new Point3D[4];
		setCoordinates(new Point3D(0, 0, 0), new Point3D(image.getWidth(),
				image.getHeight(), 0));
	}

	public Picture(URL url) throws IOException {
		this(ImageIO.read(url));
	}

	public int getWidth() {
		return image.getWidth();
	}

	public int getHeight() {
		return image.getHeight();
	}

	// XXX only works for convex 2D polygons
	@Override
	public boolean contains(Point3D point) {
		if (projectedPoints[0] == null) {
			return false;
		}
		boolean side = checkSide(point, projectedPoints[0], projectedPoints[1]);
		int i = 1;
		while (i < projectedPoints.length
				&& side == checkSide(point, projectedPoints[i],
						projectedPoints[(i + 1) % projectedPoints.length])) {
			i++;
		}
		return i == projectedPoints.length;
	}

	private static boolean checkSide(Point3D point, Point3D p1, Point3D p2) {
		double c = (point.getY() - p1.getY()) * (p2.getX() - p1.getX())
				- (point.getX() - p1.getX()) * (p2.getY() - p1.getY());
		return c < 0;
	}

	private void project(Scene scene, Dimension surfaceSize) {
		Point3D loc = getLocation();
		RotationMatrix rot = getRotationMatrix();
		List<Point3D> points = getPoints();
		Point3D[] proj = new Point3D[4];
		int i = 0;
		for (Point3D p : points) {
			Point3D pt = new Point3D(p);
			pt.rotate(rot);
			pt.translate(loc);
			proj[i] = scene.project(pt, surfaceSize);
			proj[i].setZ(pt.getZ());
			i++;
		}
		// XXX assumes too much about order
		Point3D bottomR = proj[0], bottomL = proj[1], topL = proj[2], topR = proj[3];
		if (bottomR.getX() < 0 || bottomL.getX() >= surfaceSize.getWidth()) {
			projectedPoints[0] = null;
			return;
		}
		projectedPoints[0] = topL;
		projectedPoints[1] = topR;
		projectedPoints[2] = bottomR;
		projectedPoints[3] = bottomL;
		if (topL.getX() != bottomL.getX() || topR.getX() != bottomR.getX()) {
			throw new RuntimeException();
		}
	}
	
	private void setHighQuality(boolean on, Graphics2D g) {
		if (on) {
			g.setRenderingHint(RenderingHints.KEY_RENDERING,
					RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
		} else {
			g.setRenderingHint(RenderingHints.KEY_RENDERING,
					RenderingHints.VALUE_RENDER_SPEED);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_OFF);
		}
	}

	// FIXME only works if same x (no horizontal distortion)
	@Override
	public void paint(Graphics graphics, Scene scene, Dimension surfaceSize,
			boolean active, Configuration config) {
		Graphics2D g = (Graphics2D) graphics;
		Stroke defaultStroke = g.getStroke();
		Stroke activeStroke = (config.activeShapeBorderWidth > 0
				&& config.activeShapeBorderColor != null ? new BasicStroke(
				config.activeShapeBorderWidth) : null);
		project(scene, surfaceSize);
		Point3D topL = projectedPoints[0];
		Point3D topR = projectedPoints[1];
		Point3D bottomR = projectedPoints[2];
		Point3D bottomL = projectedPoints[3];
		double x0 = topL.getX();
		double x1 = topR.getX();
		double y0 = Math.min(topL.getY(), topR.getY());
		double heightLeft = bottomL.getY() - topL.getY();
		double heightRight = bottomR.getY() - topR.getY();
		int w = (int) Math.round(x1 - x0);
		if (w <= 0) {
			projectedPoints[0] = null;
			return;
		}
		double dt = topR.getY() - topL.getY();
		boolean mirror = (dt < 0);
		if (mirror) {
			dt = -dt;
		}
		int l = (int) Math.round(x0);
		int r = (int) Math.round(x0 + w);
		int[] xPoints = new int[] { l, r, r, l };
		int[] yPoints = new int[] { (int) Math.round(topL.getY()),
				(int) Math.round(topR.getY()),
				(int) Math.round(bottomR.getY()),
				(int) Math.round(bottomL.getY()) };
		// reflection
		if (config.reflectionOpacity > 0) {
			setHighQuality(false, g);
			for (int x = 0; x < w; x++) {
				double d = 1.0 * x / w;
				int xo = (int) Math.round(d * image.getWidth());
				int xt = (int) Math.round(x0 + x);
				double colY = dt * (mirror ? 1 - d : d);
				double colH = heightLeft + (heightRight - heightLeft) * d;
				int ryt0 = (int) Math.round(y0 + colY + colH);
				int ryt1 = (int) Math.round(y0 + colY + colH + colH);
				g.drawImage(image, xt, ryt1, xt + 1, ryt0, xo, 0, xo + 1,
						image.getHeight(), null);
			}
			int[] ryPoints = new int[] {
					(int) Math.round(topL.getY() + heightLeft),
					(int) Math.round(topR.getY() + heightRight),
					(int) Math.round(bottomR.getY() + heightRight),
					(int) Math.round(bottomL.getY() + heightLeft) };
			if (active) {
				if (config.activeShapeOverlayColor != null) {
					g.setColor(config.activeShapeOverlayColor);
					g.fillPolygon(xPoints, ryPoints, 4);
				}
				// FIXME outer border doesn't receive overlay, so disable this
				// if (activeStroke != null) {
				// g.setColor(config.activeShapeBorderColor);
				// g.setStroke(activeStroke);
				// g.drawPolygon(xPoints, ryPoints, 4);
				// g.setStroke(defaultStroke);
				// }
			}
			g.setColor(getOverlayColor(1 - config.reflectionOpacity, config));
			g.fillPolygon(xPoints, ryPoints, 4);
		}
		setHighQuality(config.highQuality, g);
		// image
		int h = w;
		g.drawImage(image,
				(int)Math.round(x0),
				(int)Math.round(y0),
				w,
				h,
				null);
		double z = constrain(
				-topL.getZ() - (topR.getZ() - topL.getZ()) * 0.5f, 0,
				Double.MAX_VALUE);
		float shadeOpacity = (float) constrain(config.shadingFactor * z, 0,
				1);
		if (shadeOpacity > 0) {
			g.setColor(getOverlayColor(shadeOpacity, config));
			g.fillRect(
					(int)Math.round(x0),
					(int)Math.round(y0),
					w,
					h
					);
		}
		/*
		for (int x = 0; x < w; x++) {
			double d = 1.0 * x / w;
			int xo = (int) Math.round(d * image.getWidth());
			int xt = (int) Math.round(x0 + x);
			double colY = dt * (mirror ? 1 - d : d);
			double colH = heightLeft + (heightRight - heightLeft) * d;
			double z = constrain(
					-topL.getZ() - (topR.getZ() - topL.getZ()) * d, 0,
					Double.MAX_VALUE);
			double ys = colY;
			double ym = colY + colH;
			int yt = (int) Math.round(y0 + ys);
			int yb = (int) Math.round(y0 + ym);
			g.drawImage(image, xt, yt, xt + 1, yb, xo, 0, xo + 1,
					image.getHeight(), null);
			float shadeOpacity = (float) constrain(config.shadingFactor * z, 0,
					1);
			if (shadeOpacity > 0) {
				g.setColor(getOverlayColor(shadeOpacity, config));
				g.drawLine(xt, yt, xt, yb + yb);
			}
		}
		*/
		// shade
		/*
		if (config.shadingFactor > 0) {
			double aL = constrain(topL.getZ() * config.shadingFactor, 0, 1);
			double aR = constrain(topR.getZ() * config.shadingFactor, 0, 1);
			Color cL = getOverlayColor(aL, config);
			Color cR = getOverlayColor(aR, config);
			Paint oldPaint = g.getPaint();
			g.setPaint(new GradientPaint((float) Math.round(x0), 0f, cL,
					(float) Math.round(x0 + w - 1), 0f, cR));
			g.fillPolygon(xPoints, yPoints, 4);
			g.setPaint(oldPaint);
		}
		*/
		// border
		if (active) {
			if (config.activeShapeOverlayColor != null) {
				g.setColor(config.activeShapeOverlayColor);
				g.fillRect(
						(int)Math.round(x0),
						(int)Math.round(y0),
						w,
						h
						);
			}
			if (activeStroke != null) {
				g.setColor(config.activeShapeBorderColor);
				g.setStroke(activeStroke);
				g.drawRect(
						(int)Math.round(x0),
						(int)Math.round(y0),
						w,
						h
						);
				g.setStroke(defaultStroke);
			}
		}
	}

	private static Color getOverlayColor(double opacity, Configuration config) {
		Color base = config.backgroundColor;
		return new Color(base.getRed(), base.getGreen(), base.getBlue(),
				(int) Math.round(base.getAlpha() * opacity));
	}

	private static double constrain(double a, double min, double max) {
		return a < min ? min : (a > max ? max : a);
	}
}
