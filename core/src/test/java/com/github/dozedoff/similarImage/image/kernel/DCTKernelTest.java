/*  Copyright (C) 2017  Nicholas Wright
    
    This file is part of similarImage - A similar image finder using pHash
    
    similarImage is free software: you can redistribute it and/or modify
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
package com.github.dozedoff.similarImage.image.kernel;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.primitives.Doubles;

public class DCTKernelTest {
	private double[][] testMatrix;
	private static final double[] EXPECTED = { 259.99999999999994, -18.221641183796056, -3.014577520672848E-14,
			-1.9048178261672375, 7.53644380168212E-15, -0.5682392223670845, -4.8358847727460265E-14,
			-0.14340782498104815, -145.7731294703686, -1.7763568394002505E-15, 1.0658141036401503E-14,
			-1.2434497875801753E-14, -3.552713678800501E-15, -3.730349362740526E-14, 2.7533531010703882E-14,
			1.5987211554602254E-14, -2.888970123978146E-14, -1.7763568394002505E-15, 0.0, 0.0, 0.0,
			-3.552713678800501E-15, 8.881784197001252E-16, 0.0, -15.238542609337976, -3.552713678800501E-15,
			1.7763568394002505E-15, 0.0, -1.7763568394002505E-15, -8.881784197001252E-15, 1.7763568394002505E-15,
			3.9968028886505635E-15, -1.25607396694702E-15, 7.105427357601002E-15, -3.552713678800501E-15,
			3.552713678800501E-15, -2.6645352591003757E-15, -4.440892098500626E-15, -8.881784197001252E-16,
			8.881784197001252E-16, -4.545913778937215, -5.329070518200751E-15, -5.329070518200751E-15,
			-1.7763568394002505E-15, -3.552713678800501E-15, -8.881784197001252E-16, 1.3322676295501878E-15,
			1.1102230246251565E-15, -4.4590625826619206E-14, -8.881784197001252E-16, 8.881784197001252E-16,
			1.7763568394002505E-15, -1.7763568394002505E-15, -8.881784197001252E-16, -5.773159728050814E-15,
			1.3322676295501878E-15, -1.1472625998482104, 4.440892098500626E-16, 0.0, -4.440892098500626E-16,
			1.3322676295501878E-15, -2.4424906541753444E-15, 6.661338147750939E-16, -3.3306690738754696E-15 };

	private DCTKernel cut;

	@Before
	public void setUp() throws Exception {
		setupMatrix();

		this.cut = new DCTKernel();
	}

	private void setupMatrix() {
		testMatrix = new double[DCTKernel.DEFAULT_MATRIX_SIZE][DCTKernel.DEFAULT_MATRIX_SIZE];
		double value = 1;

		for (int i = 0; i < testMatrix.length; i++) {
			for (int j = 0; j < testMatrix.length; j++) {
				testMatrix[i][j] = value;
				value++;
			}
		}

		assertThat(testMatrix[0][0], is(1.0));
		assertThat(testMatrix[0][7], is(8.0));
		assertThat(testMatrix[7][0], is(57.0));
		assertThat(testMatrix[7][7], is(64.0));
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testTransformDCT() throws Exception {
		double[] result = cut.transformDCT(Doubles.concat(testMatrix));

		assertArrayEquals(EXPECTED, Doubles.concat(result), 0.1);
	}
}
