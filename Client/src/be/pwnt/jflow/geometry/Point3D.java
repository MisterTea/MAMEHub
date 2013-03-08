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

package be.pwnt.jflow.geometry;

import be.pwnt.jflow.Matrix;

public class Point3D extends Matrix {
	public Point3D(double x, double y, double z) {
		super(3, 1, x, y, z);
	}

	public Point3D(double x, double y) {
		this(x, y, 0);
	}

	public Point3D(Matrix matrix) {
		super(3, 1);
		init(matrix);
	}

	private void init(Matrix matrix) {
		setX(matrix.getValue(0, 0));
		setY(matrix.getValue(1, 0));
		setZ(matrix.getValue(2, 0));
	}

	public double getX() {
		return getValue(0, 0);
	}

	public void setX(double x) {
		setValue(0, 0, x);
	}

	public double getY() {
		return getValue(1, 0);
	}

	public void setY(double y) {
		setValue(1, 0, y);
	}

	public double getZ() {
		return getValue(2, 0);
	}

	public void setZ(double z) {
		setValue(2, 0, z);
	}

	public void translate(Point3D distance) {
		init(add(distance));
	}

	public void rotate(RotationMatrix matrix) {
		init(matrix.multiply(this));
	}

	@Override
	public String toString() {
		return getX() + "," + getY() + "," + getZ();
	}
}
