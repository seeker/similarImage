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
package com.github.dozedoff.similarImage.learning;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;

import com.aparapi.Kernel;
import com.aparapi.Range;
import com.aparapi.natives.NativeLoader;

public class AparapiLearning {
	private static final int TEST_SIZE = 10000000;
	private static double[] testData = new double[TEST_SIZE];

	@BeforeClass
	public static void setupClass() throws Exception {
		for (int i = 0; i < TEST_SIZE; i++) {
			testData[i] = Math.random();
		}

		NativeLoader.load();
	}

	private double[] testDataCopy() {
		return Arrays.copyOf(testData, TEST_SIZE);
	}

	@Test
	public void testSinViaKernel() throws Exception {
		double[] data = testDataCopy();
		double[] result = new double [TEST_SIZE];
		
		Kernel kernel = new Kernel() {
		    @Override
		    public void run() {
		        int i = getGlobalId();
				result[i] = sin(data[i]);
		    }
		};

		Range range = Range.create(result.length);
		
		
		kernel.execute(range);
		kernel.dispose();
	}
}
