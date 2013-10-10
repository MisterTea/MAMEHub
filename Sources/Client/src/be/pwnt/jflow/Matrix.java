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

import java.util.List;
import java.util.Vector;

public class Matrix {
	private List<List<Double>> values;

	public Matrix(int rows, int columns, double... values) {
		if (values.length > 0 && values.length != rows * columns) {
			throw new IllegalArgumentException();
		}
		this.values = new Vector<List<Double>>(rows);
		for (int i = 0; i < rows; i++) {
			List<Double> row = new Vector<Double>(columns);
			for (int j = 0; j < columns; j++) {
				int index = i * columns + j;
				row.add(values.length > 0 ? values[index] : 0.0);
			}
			this.values.add(row);
		}
	}

	public Matrix(Matrix matrix) {
		this.values = new Vector<List<Double>>(matrix.values.size());
		for (List<Double> row : matrix.values) {
			List<Double> r = new Vector<Double>(row.size());
			for (double value : row) {
				r.add(value);
			}
			this.values.add(r);
		}
	}

	public int getRowCount() {
		return values.size();
	}

	public int getColumnCount() {
		return values.get(0).size();
	}

	public double getValue(int row, int column) {
		return values.get(row).get(column);
	}

	public void setValue(int row, int column, double value) {
		values.get(row).set(column, value);
	}

	public Matrix add(Matrix other) {
		if (getRowCount() != other.getRowCount()
				|| getColumnCount() != other.getColumnCount()) {
			throw new IllegalArgumentException();
		}
		Matrix p = new Matrix(getRowCount(), getColumnCount());
		for (int r = 0; r < p.getRowCount(); r++) {
			for (int c = 0; c < p.getColumnCount(); c++) {
				p.setValue(r, c, getValue(r, c) + other.getValue(r, c));
			}
		}
		return p;
	}

	public Matrix subtract(Matrix other) {
		if (getRowCount() != other.getRowCount()
				|| getColumnCount() != other.getColumnCount()) {
			throw new IllegalArgumentException();
		}
		Matrix p = new Matrix(getRowCount(), getColumnCount());
		for (int r = 0; r < p.getRowCount(); r++) {
			for (int c = 0; c < p.getColumnCount(); c++) {
				p.setValue(r, c, getValue(r, c) - other.getValue(r, c));
			}
		}
		return p;
	}

	public Matrix multiply(Matrix other) {
		if (getColumnCount() != other.getRowCount()) {
			throw new IllegalArgumentException();
		}
		Matrix p = new Matrix(getRowCount(), other.getColumnCount());
		for (int r = 0; r < p.getRowCount(); r++) {
			for (int c = 0; c < p.getColumnCount(); c++) {
				double sum = 0;
				for (int i = 0; i < getColumnCount(); i++) {
					sum += getValue(r, i) * other.getValue(i, c);
				}
				p.setValue(r, c, sum);
			}
		}
		return p;
	}
}
