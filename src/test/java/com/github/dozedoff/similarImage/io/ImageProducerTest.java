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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.dozedoff.commonj.util.Pair;
import com.github.dozedoff.similarImage.db.BadFileRecord;
import com.github.dozedoff.similarImage.db.Persistence;

@RunWith(MockitoJUnitRunner.class)
public class ImageProducerTest {
	@Mock
	private Persistence persistence;
	private ImageProducer imageProducer;

	private static Path testImage = null, notAnImage = null;
	private static Path[] images;

	private static final int NUM_OF_TEST_IMAGES = 5;
	private static final int SLEEP_DELAY = 300;
	private static final int OUTPUT_QUEUE_SIZE = 100;

	@BeforeClass
	public static void setUpClass() throws URISyntaxException {
		testImage = Paths.get(Thread.currentThread().getContextClassLoader().getResource("testImage.jpg").toURI());
		notAnImage = Paths.get(Thread.currentThread().getContextClassLoader().getResource("notAnImage.jpg").toURI());
		assertThat("Could not load test image", Files.exists(testImage), is(true));

		images = new Path[NUM_OF_TEST_IMAGES];

		for (int i = 0; i < NUM_OF_TEST_IMAGES; i++) {
			images[i] = testImage;
		}
	}

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		imageProducer = new ImageProducer(OUTPUT_QUEUE_SIZE, persistence);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testClearTotal() throws Exception {
		imageProducer.addToLoad(images);
		assertThat(imageProducer.getTotal(), is(NUM_OF_TEST_IMAGES));

		imageProducer.clear();
		assertThat(imageProducer.getTotal(), is(0));
	}

	@Test(timeout = 6000)
	public void testClearProcessed() throws Exception {
		imageProducer.addToLoad(images);

		Thread.sleep(SLEEP_DELAY);
		assertThat(imageProducer.getProcessed(), is(NUM_OF_TEST_IMAGES));

		imageProducer.clear();

		assertThat(imageProducer.getProcessed(), is(0));
	}

	@Test(timeout = 6000)
	public void testBadFilesOutputQueue() throws Exception {
		when(persistence.isBadFile(any(Path.class))).thenReturn(true, true, false);
		Collection<Pair<Path, BufferedImage>> results = new LinkedList<>();

		imageProducer.addToLoad(images);
		Thread.sleep(SLEEP_DELAY);
		imageProducer.drainTo(results, OUTPUT_QUEUE_SIZE);

		assertThat(results.size(), is(3));
	}

	@Test(timeout = 6000)
	public void testBadFilesTotal() throws Exception {
		when(persistence.isBadFile(any(Path.class))).thenReturn(true, true, false);

		imageProducer.addToLoad(images);

		assertThat(imageProducer.getTotal(), is(NUM_OF_TEST_IMAGES));
	}

	@Test(timeout = 6000)
	public void testBadFilesProcessed() throws Exception {
		when(persistence.isBadFile(any(Path.class))).thenReturn(true, true, false);

		imageProducer.addToLoad(images);
		Thread.sleep(SLEEP_DELAY);

		assertThat(imageProducer.getProcessed(), is(NUM_OF_TEST_IMAGES));
	}

	@Test(timeout = 6000)
	public void testRecordedPathsTotal() throws Exception {
		when(persistence.isPathRecorded(any(Path.class))).thenReturn(true, true, false);

		imageProducer.addToLoad(images);

		assertThat(imageProducer.getTotal(), is(NUM_OF_TEST_IMAGES));
	}

	@Test(timeout = 6000)
	public void testRecordedPathsProcessed() throws Exception {
		when(persistence.isPathRecorded(any(Path.class))).thenReturn(true, true, false);

		imageProducer.addToLoad(images);
		Thread.sleep(SLEEP_DELAY);

		assertThat(imageProducer.getProcessed(), is(NUM_OF_TEST_IMAGES));
	}

	@Test
	public void testDrainTo() throws Exception {
		Collection<Pair<Path, BufferedImage>> results = new LinkedList<>();

		imageProducer.addToLoad(images);
		Thread.sleep(SLEEP_DELAY);
		imageProducer.drainTo(results, OUTPUT_QUEUE_SIZE);

		assertThat(results.size(), is(NUM_OF_TEST_IMAGES));
	}

	@Test
	public void testAddToLoadList() throws Exception {
		Collection<Pair<Path, BufferedImage>> results = new LinkedList<>();
		List<Path> list = Arrays.asList(images);

		imageProducer.addToLoad(list);
		Thread.sleep(SLEEP_DELAY);
		imageProducer.drainTo(results, OUTPUT_QUEUE_SIZE);

		assertThat(results.size(), is(NUM_OF_TEST_IMAGES));
	}

	@Test
	public void testNonExistantFile() throws Exception {
		Path path = Paths.get("foo");

		imageProducer.addToLoad(path);

		Thread.sleep(SLEEP_DELAY);

		verify(persistence, never()).addBadFile(any(BadFileRecord.class));
	}

	@Test
	public void testUnreadableImage() throws Exception {
		imageProducer.addToLoad(notAnImage);

		Thread.sleep(SLEEP_DELAY);

		verify(persistence).addBadFile(any(BadFileRecord.class));
	}
}
