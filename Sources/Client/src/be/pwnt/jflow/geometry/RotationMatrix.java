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

public class RotationMatrix extends Matrix {
	public RotationMatrix(double x, double y, double z) {
		super(getMatrix(x, y, z));
	}

	public RotationMatrix(RotationMatrix matrix) {
		super(matrix);
	}

	private static Matrix getMatrix(double x, double y, double z) {
		Matrix xm = new Matrix(3, 3, 1, 0, 0, 0, Math.cos(x), -Math.sin(x), 0,
				Math.sin(x), Math.cos(x));
		Matrix ym = new Matrix(3, 3, Math.cos(y), 0, Math.sin(y), 0, 1, 0,
				-Math.sin(y), 0, Math.cos(y));
		Matrix zm = new Matrix(3, 3, Math.cos(z), -Math.sin(z), 0, Math.sin(z),
				Math.cos(z), 0, 0, 0, 1);
		return xm.multiply(ym).multiply(zm);
	}
}
