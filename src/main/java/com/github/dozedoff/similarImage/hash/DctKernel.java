/*  Copyright (C) 2013  Nicholas Wright
    
    This file is part of similarImage - A similar image finder using pHash
    
    mmut is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.github.dozedoff.similarImage.hash;

import com.amd.aparapi.Kernel;

public class DctKernel extends Kernel {
	private final double PI = Math.PI;
	private final double[] coeff;
	private final int size;
	private final double data[];
	private final double dct[];
	
	
	
	public DctKernel(double[] coeff, int size, double[] data, double[] dct) {
		this.coeff = coeff;
		this.size = size;
		this.data = data;
		this.dct = dct;
	}

	public void run() {
		int u = getGlobalId(0);
		int v = getGlobalId(1);
		double sum = 0.0;
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				sum += cos(((2 * i + 1) / (2.0 * size)) * u * PI)
						* cos(((2 * j + 1) / (2.0 * size)) * v * PI)
						* (data[dimConversion(i, j)]);
			}
		}
		sum *= ((coeff[u] * coeff[v]) / 4.0);
		dct[dimConversion(u, v)] = sum;
	}
	
	private int dimConversion(int x, int y) {
		return x + y*size;
	}
}
