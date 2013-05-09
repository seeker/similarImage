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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amd.aparapi.Kernel;
import com.amd.aparapi.Kernel.EXECUTION_MODE;
import com.amd.aparapi.Range;
import com.github.dozedoff.commonj.time.StopWatch;

public class GpuDctTest {
	private final static Logger logger = LoggerFactory.getLogger(GpuDctTest.class);
	private final int SAMPLE_SIZE = 2000000;
	private final int LOOP_SIZE = 5000;
	
	private float[] generateFloatArray(int size){
		float result[] = new float[size];
		
		for(int i=0; i < size; i++){
			result[i] = (float)(Math.random()*10);
		}
		return result;
	}
	
	private float[][] generateFloatGrid(int size) {
		float data[][] = new float[size][size];
		
		for(int i = 0; i < size; i++){
			for(int j = 0; j < size; j++){
				data[i][j] = (float) Math.random();
			}
		}
		
		return data;
	}
	
	private double[][] generateDoubleGrid(int size) {
		double data[][] = new double[size][size];
		
		for(int i = 0; i < size; i++){
			for(int j = 0; j < size; j++){
				data[i][j] = Math.random();
			}
		}
		
		return data;
	}
	
	private double[] initCoefficients(int size) {
		double c[] = new double[size];

		for (int i = 1; i < size; i++) {
			c[i] = 1;
		}
		c[0] = 1 / Math.sqrt(2.0);

		return c;
	}
	
	private double[] unwrap2dArray(double array[][]) {
		int size = array.length * array[0].length;
		double flatArray[] = new double[size];
		int pointer = 0;
		
		for(int i = 0; i < array.length; i++) {
			for(int j = 0; j < array[0].length; j++) {
				flatArray[pointer] = array[i][j];
				pointer++;
			}
		}
		
		return flatArray;
	}

	@Ignore
	@Test
	public void addFloatsTest() {
		final float[] floatA = generateFloatArray(SAMPLE_SIZE);
		final float[] resultGPU = new float[SAMPLE_SIZE];
		final float[] resultCPU = new float[SAMPLE_SIZE];
		
		StopWatch sw = new StopWatch();
		sw.start();
		
		for(int i=0; i < SAMPLE_SIZE; i++){
			for(int j=0; j < LOOP_SIZE; j++){
				resultCPU[i] += floatA[i];
			}
		}
		
		sw.stop();
		logger.info("CPU float add took {}", sw.getTime());
		
		sw = new StopWatch();
		
		Kernel kernel = new Kernel() {
			@Override
			public void run() {
				int i = getGlobalId();
				calc(i);
			}
			
			private void calc(int id) {
				for (int j = 0; j < LOOP_SIZE; j++) {
					resultGPU[id] += floatA[id];
				}
			}
		};
		
		Range range = Range.create(resultGPU.length);
		
		sw.start();
		kernel.execute(range);
		sw.stop();
		kernel.dispose();
		logger.info("GPU float add took {}", sw.getTime());
		
		for(int i=0; i < SAMPLE_SIZE; i++){
			assertThat(resultGPU[i], is(resultCPU[i]));
		}
	}
	
	@Test
	public void dctTest() {
		final int SIZE = 32;
		final double[] coeff = initCoefficients(SIZE);
		final double PI = Math.PI;
		double data[][] = generateDoubleGrid(SIZE);
		
		final double dataFlat[] = unwrap2dArray(data);
		final double dct[] = new double[SIZE * SIZE]; //result
		
		Kernel dctKernel = new Kernel() {
			@Override
			public void run() {
				int u = getGlobalId(0);
				int v = getGlobalId(1);
				double sum = 0.0;
				for (int i = 0; i < SIZE; i++) {
					for (int j = 0; j < SIZE; j++) {
						sum += cos(((2 * i + 1) / (2.0 * SIZE)) * u * PI)
								* cos(((2 * j + 1) / (2.0 * SIZE)) * v * PI)
								* (dataFlat[dimConversion(i, j)]);
					}
				}
				sum *= ((coeff[u] * coeff[v]) / 4.0);
				dct[dimConversion(u, v)] = sum;
			}
			
			private int dimConversion(int x, int y) {
				return x + y*SIZE;
			}
		};
		
		Range range = Range.create2D(SIZE, SIZE);
		dctKernel.setExecutionMode(EXECUTION_MODE.GPU);
		dctKernel.execute(range);
		dctKernel.dispose();
		
		ImagePHash phash = new ImagePHash(SIZE, 9);
		double cpuDct[][] = phash.applyDCT(data);
		double cpu[] = unwrap2dArray(cpuDct);
		
		int runLenght = SIZE * SIZE;
		for(int i = 0; i < runLenght; i++){
			assertThat(dct[i], is(cpu[i]));
		}
	}
	
	@Test
	public void doubleSupportTest() {
		final double test[] = new double[SAMPLE_SIZE];
		
		Kernel kernel = new Kernel() {
			 
			@Override
			public void run() {
				int i = getGlobalId();
				test[i]++;
			}
		};
		
		kernel.setExecutionMode(EXECUTION_MODE.GPU);
		kernel.execute(SAMPLE_SIZE);
		kernel.dispose();
	}
}
