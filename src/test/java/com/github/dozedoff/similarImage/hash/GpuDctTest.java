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

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amd.aparapi.Kernel;
import com.amd.aparapi.Range;
import com.github.dozedoff.commonj.time.StopWatch;

public class GpuDctTest {
	private final static Logger logger = LoggerFactory.getLogger(GpuDctTest.class);
	private final int SAMPLE_SIZE = 2000000;
	private final int LOOP_SIZE = 5000;
	
	@Before
	public void setUp() throws Exception {
	}

	private float[] generateFloatArray(int size){
		float result[] = new float[size];
		
		for(int i=0; i < size; i++){
			result[i] = (float)(Math.random()*10);
		}
		return result;
	}

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
				for (int j = 0; j < LOOP_SIZE; j++) {
					resultGPU[i] += floatA[i];
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
}
