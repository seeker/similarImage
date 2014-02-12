/*  Copyright (C) 2014  Nicholas Wright
    
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
package com.github.dozedoff.similarImage.io;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.github.dozedoff.similarImage.thread.ImageLoadJob;
import com.github.dozedoff.similarImage.thread.NamedThreadFactory;

public class ImageProducerPoolTest {
	private ImageProducerPool ipp;
	private ImageProducer ip;

	private static final int TEST_TIMEOUT = 5000;
	private static final int NUM_OF_JOBS = 20;
	private static final int JOB_SIZE = 5;

	@Before
	public void setUp() throws Exception {
		ip = mock(ImageProducer.class);
		int availableProcessors = Runtime.getRuntime().availableProcessors();

		ipp = new ImageProducerPool(availableProcessors, availableProcessors, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
				new NamedThreadFactory("test"), ip);

	}

	@Test(timeout = TEST_TIMEOUT)
	public void testAfterExecute() throws Exception {
		createAndExecuteJob();

		ipp.shutdown();
		spinLock();

		verify(ip).listenersUpdateTotalProgress();
		verify(ip).addToProcessed(JOB_SIZE);
	}

	@Test(timeout = TEST_TIMEOUT)
	public void testAfterExecuteMultiple() throws Exception {
		for (int i = 0; i < NUM_OF_JOBS; i++) {
			createAndExecuteJob();
		}

		ipp.shutdown();
		spinLock();

		verify(ip, times(NUM_OF_JOBS)).listenersUpdateTotalProgress();
		verify(ip, times(NUM_OF_JOBS)).addToProcessed(JOB_SIZE);
	}

	private void spinLock() {
		while (!ipp.isTerminated()) {
			// spin
		}
	}

	private void createAndExecuteJob() {
		ImageLoadJob ilj = mock(ImageLoadJob.class);
		when(ilj.getJobSize()).thenReturn(JOB_SIZE);
		ipp.execute(ilj);
	}
}
