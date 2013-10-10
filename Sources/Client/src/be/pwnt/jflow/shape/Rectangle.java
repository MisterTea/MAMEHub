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

import be.pwnt.jflow.geometry.Point3D;

public class Rectangle extends Polygon {
	public Rectangle(Point3D p1, Point3D p2, Color color) {
		super(color);
		setCoordinates(p1, p2);
	}

	public void setCoordinates(Point3D p1, Point3D p2) {
		if (p1.getZ() != p2.getZ()) {
			throw new IllegalArgumentException();
		}
		removePoints();
		addPoint(p1);
		addPoint(new Point3D(p2.getX(), p1.getY(), p1.getZ()));
		addPoint(p2);
		addPoint(new Point3D(p1.getX(), p2.getY(), p1.getZ()));
	}

	@Override
	public boolean contains(Point3D point) {
		throw new UnsupportedOperationException();
	}
}
