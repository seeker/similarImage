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
package com.github.dozedoff.similarImage.hash;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.dozedoff.similarImage.thread.NamedThreadFactory;

@RunWith(MockitoJUnitRunner.class)
public class HashWorkerPoolTest {
	@Mock
	private PhashWorker phw;

	private static final int TEST_TIMEOUT = 5000;
	private static final int NUM_OF_JOBS = 20;

	private HashWorkerPool hashWorkerPool;
	private LinkedBlockingQueue<Runnable> queue;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		queue = new LinkedBlockingQueue<>();
		int availableProcessors = Runtime.getRuntime().availableProcessors();
		hashWorkerPool = new HashWorkerPool(availableProcessors, availableProcessors, 10, TimeUnit.SECONDS, queue, new NamedThreadFactory(
				"test"), phw);
	}

	@Test(timeout = TEST_TIMEOUT)
	public void testAfterExecute() throws Exception {
		createAndExecuteJob();

		hashWorkerPool.shutdown();
		spinLock();

		verify(phw).listenersUpdateBufferLevel();
	}

	@Test(timeout = TEST_TIMEOUT)
	public void testAfterExecuteMultiple() throws Exception {
		for (int i = 0; i < NUM_OF_JOBS; i++) {
			createAndExecuteJob();
		}

		hashWorkerPool.shutdown();
		spinLock();

		verify(phw, times(NUM_OF_JOBS)).listenersUpdateBufferLevel();
	}

	private void spinLock() {
		while (!hashWorkerPool.isTerminated()) {
			// spin
		}
	}

	private void createAndExecuteJob() {
		hashWorkerPool.execute(new Runnable() {
			@Override
			public void run() {
				// do nothing
			}
		});
	}

}
