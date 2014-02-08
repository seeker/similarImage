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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.github.dozedoff.commonj.hash.ImagePHash;
import com.github.dozedoff.commonj.util.Pair;
import com.github.dozedoff.similarImage.db.DBWriter;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.io.ImageProducerObserver;

public class PhashWorkerTest {
	private PhashWorker phw;
	private static Path testImage;

	private static final int NUM_OF_TEST_IMAGES = 5;

	@Mock
	private DBWriter dbWriter;

	@BeforeClass
	public static void setUpClass() throws Exception {
		testImage = Paths.get(Thread.currentThread().getContextClassLoader().getResource("testImage.jpg").toURI());
	}

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		phw = new PhashWorker(dbWriter);
	}

	@After
	public void tearDown() throws Exception {
		phw.forceShutdown();
	}

	private List<Pair<Path, BufferedImage>> createWork(int amount) throws IOException {
		InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("testImage.jpg");
		BufferedImage bi = ImageIO.read(is);
		bi = ImagePHash.resize(bi, 32, 32);

		LinkedList<Pair<Path, BufferedImage>> work = new LinkedList<>();

		for (int i = 0; i < amount; i++) {
			work.add(new Pair<Path, BufferedImage>(testImage, bi));
		}

		return work;
	}

	@Test(timeout = 2000)
	public void testHashImage() throws Exception {
		phw.toHash(createWork(1));
		phw.gracefulShutdown();
		hashSpinLock();

		verify(dbWriter).add(anyListOf(ImageRecord.class));
		// TODO needs more accurate test
	}

	@Test(timeout = 2000)
	public void testHashImageWhenShutdown() throws Exception {
		List<Pair<Path, BufferedImage>> work = createWork(NUM_OF_TEST_IMAGES);

		phw.gracefulShutdown();
		phw.toHash(work);

		hashSpinLock();

		verify(dbWriter, never()).add(anyListOf(ImageRecord.class));
	}

	@Test(timeout = 2000)
	public void testStopWorker() throws Exception {
		List<Pair<Path, BufferedImage>> work = createWork(NUM_OF_TEST_IMAGES);
		phw.toHash(work);

		phw.forceShutdown();

		verify(dbWriter, never()).add(anyListOf(ImageRecord.class));
	}

	@Test(timeout = 5000)
	public void testBufferLevel() throws IOException {
		List<Pair<Path, BufferedImage>> work = createWork(NUM_OF_TEST_IMAGES);
		ImageProducerObserver ipo = mock(ImageProducerObserver.class);

		phw.addGuiUpdateListener(ipo);
		phw.toHash(work);
		phw.gracefulShutdown();

		hashSpinLock();

		verify(ipo, times(2)).bufferLevelChanged(anyInt());
	}

	private void hashSpinLock() {
		while (!phw.isTerminated()) {
			// spin spin
		}
	}

	@Test(timeout = 2000)
	public void testClear() throws Exception {
		List<Pair<Path, BufferedImage>> work = createWork(10000);
		phw.toHash(work);
		phw.gracefulShutdown();
		phw.clear();

		hashSpinLock();
	}

	@Test
	public void testSetPoolSize() {
		phw.setPoolSize(1);
		assertThat(phw.getPoolSize(), is(1));

		phw.setPoolSize(4);
		assertThat(phw.getPoolSize(), is(4));
	}

	@Test
	public void testSetPoolSizeZero() {
		int poolSize = phw.getPoolSize();
		assertThat(poolSize, is(not(0))); // guard

		phw.setPoolSize(0);

		assertThat(phw.getPoolSize(), is(poolSize));
	}

	@Test
	public void testSetPoolSizeNegative() {
		int poolSize = phw.getPoolSize();
		assertThat(poolSize, is(not(-1))); // guard

		phw.setPoolSize(-1);

		assertThat(phw.getPoolSize(), is(poolSize));
	}

	@Test
	public void testSelectHashPoolSizeZero() {
		assertThat(phw.selectHashPoolSize(0), is(1));
	}

	@Test
	public void testSelectHashPoolSizeNegative() {
		assertThat(phw.selectHashPoolSize(-2), is(1));
	}

	@Test
	public void testSelectHashPoolSizeDualCore() {
		assertThat(phw.selectHashPoolSize(2), is(1));
	}

	@Test
	public void testSelectHashPoolSizeQuadCore() {
		assertThat(phw.selectHashPoolSize(4), is(3));
	}

	@Test
	public void testSelectHashPoolSizeQuadCoreWithHyperThreading() {
		assertThat(phw.selectHashPoolSize(8), is(7));
	}
}
