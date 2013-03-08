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

package be.pwnt.jflow;

import java.awt.Dimension;
import java.awt.Graphics;

import be.pwnt.jflow.geometry.Point3D;
import be.pwnt.jflow.geometry.RotationMatrix;

public abstract class Shape {
	private Point3D location;

	private RotationMatrix rotationMatrix;

	public Shape() {
		setLocation(new Point3D(0, 0, 0));
		setRotationMatrix(new RotationMatrix(0, 0, 0));
	}

	public Point3D getLocation() {
		return location;
	}

	public void setLocation(Point3D location) {
		this.location = location;
	}

	public RotationMatrix getRotationMatrix() {
		return rotationMatrix;
	}

	public void setRotationMatrix(RotationMatrix rotationMatrix) {
		this.rotationMatrix = rotationMatrix;
	}

	public abstract boolean contains(Point3D point);

	public abstract void paint(Graphics graphics, Scene scene,
			Dimension surfaceSize, boolean active, Configuration config);
}