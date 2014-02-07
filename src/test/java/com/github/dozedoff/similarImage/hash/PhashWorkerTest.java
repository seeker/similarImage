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

import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.verify;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.github.dozedoff.similarImage.db.DBWriter;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Persistence;
import com.github.dozedoff.similarImage.io.ImageProducer;

public class PhashWorkerTest {
	private PhashWorker phw;
	private static Path testImage;
	private ImageProducer ip;

	private static final long SLEEP_TIME = 300L;
	private static final int NUM_OF_TEST_IMAGES = 10000;

	@Mock
	private Persistence persistence;

	@Mock
	private DBWriter dbWriter;

	@BeforeClass
	public static void setUpClass() throws Exception {
		testImage = Paths.get(Thread.currentThread().getContextClassLoader().getResource("testImage.jpg").toURI());
	}

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		ip = new ImageProducer(100, persistence, true);
		phw = new PhashWorker(dbWriter);
	}

	@After
	public void tearDown() throws Exception {
		phw.shutdown();
	}

	@Test(timeout = 6000)
	public void testHashImage() throws Exception {
		ip.addToLoad(testImage);

		Thread.sleep(SLEEP_TIME);

		verify(dbWriter).add(anyListOf(ImageRecord.class));
		// TODO needs more accurate test
	}

	@Test(timeout = 6000)
	public void testStopWorker() throws Exception {
		List<Path> work = Collections.nCopies(NUM_OF_TEST_IMAGES, testImage);
		ip.addToLoad(work);

		Thread.sleep(SLEEP_TIME);

		verify(dbWriter, atMost(100)).add(anyListOf(ImageRecord.class));
	}
}
