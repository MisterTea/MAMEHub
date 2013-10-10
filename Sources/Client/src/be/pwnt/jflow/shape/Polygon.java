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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.List;
import java.util.Vector;

import be.pwnt.jflow.Configuration;
import be.pwnt.jflow.Scene;
import be.pwnt.jflow.Shape;
import be.pwnt.jflow.geometry.Point3D;
import be.pwnt.jflow.geometry.RotationMatrix;

public abstract class Polygon extends Shape {
	private List<Point3D> points;

	private Color color;

	protected Polygon(Color color) {
		super();
		points = new Vector<Point3D>();
		setColor(color);
	}

	public List<Point3D> getPoints() {
		return points;
	}

	protected void addPoint(Point3D point) {
		points.add(point);
	}

	protected void removePoints() {
		points.clear();
	}

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	@Override
	public void paint(Graphics graphics, Scene scene, Dimension surfaceSize,
			boolean active, Configuration config) {
		if (points.isEmpty()) {
			return;
		}
		Point3D loc = getLocation();
		RotationMatrix rot = getRotationMatrix();
		int n = points.size();
		int[] x = new int[n];
		int[] y = new int[n];
		int i = 0;
		for (Point3D p : points) {
			Point3D pt = new Point3D(p);
			pt.rotate(rot);
			pt.translate(loc);
			Point3D pr = scene.project(pt, surfaceSize);
			x[i] = (int) pr.getX();
			y[i] = (int) pr.getY();
			i++;
		}
		graphics.setColor(getColor());
		graphics.drawPolygon(x, y, n);
	}
}
