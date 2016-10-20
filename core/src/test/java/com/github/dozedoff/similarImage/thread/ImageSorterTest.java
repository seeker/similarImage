/*  Copyright (C) 2016  Nicholas Wright
    
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
package com.github.dozedoff.similarImage.thread;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;
import com.github.dozedoff.similarImage.event.GuiGroupEvent;
import com.github.dozedoff.similarImage.io.Statistics;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

@RunWith(MockitoJUnitRunner.class)
public class ImageSorterTest {
	private static final long HASH_ONE = 1L;
	private static final long HASH_TWO = 2L;
	private static final long HASH_THREE = 3L;
	private static final long HASH_SIX = 6L;

	private static final String TEST_PATH = "/foo/bar/";

	private static final String TEST_EXCEPTION_MESSAGE = "Testing...";

	@Mock
	private ImageRepository imageRepository;

	@Mock
	private Statistics statistics;

	private ImageSorter imageSorter;
	private Multimap<Long, ImageRecord> result;

	private static final int DISTANCE = 0;
	private Path path;

	private ImageRecord recordOneOne;
	private ImageRecord recordTwoOne;
	private ImageRecord recordTwoTwo;
	private ImageRecord recordThreeThree;
	private ImageRecord recordSixSix;

	private EventBus eventBus;

	@Before
	public void setUp() throws Exception {
		eventBus = new EventBus();
		eventBus.register(this);

		path = Files.createTempFile(ImageSorterTest.class.getSimpleName(), ".dat");

		List<ImageRecord> records = buildRecords();
		when(imageRepository.getAll()).thenReturn(records);
		when(imageRepository.startsWithPath(path)).thenReturn(Arrays.asList(recordOneOne, recordTwoOne, recordTwoTwo));
		
		result = MultimapBuilder.hashKeys().hashSetValues().build();
		imageSorter = new ImageSorter(DISTANCE, "", imageRepository, eventBus);
	}

	/**
	 * Listen to GUI group events and store the updated group information.
	 * 
	 * @param event
	 *            the group update event
	 */
	@Subscribe
	public void guiGroupEvents(GuiGroupEvent event) {
		result = event.getGroups();
	}

	private List<ImageRecord> buildRecords() {
		recordOneOne = new ImageRecord(TEST_PATH + "1", HASH_ONE);
		recordTwoOne = new ImageRecord(TEST_PATH + "2", HASH_ONE);
		recordTwoTwo = new ImageRecord(TEST_PATH + "2", HASH_TWO);
		recordThreeThree = new ImageRecord("3", HASH_THREE);
		recordSixSix = new ImageRecord("6", HASH_SIX);

		List<ImageRecord> records = new LinkedList<ImageRecord>();
		records.add(recordOneOne);
		records.add(recordTwoOne);
		records.add(recordTwoTwo);
		records.add(recordThreeThree);
		records.add(recordSixSix);

		return records;
	}

	private void runCutAndWaitForFinish() throws InterruptedException {
		imageSorter.start();
		imageSorter.join();
	}

	private ImageSorter createCUT(String path) {
		return new ImageSorter(1, path, imageRepository, eventBus);
	}

	private ImageSorter createCUT() {
		return createCUT("");
	}

	@Test
	public void testDistanceZeroRecords() throws Exception {
		runCutAndWaitForFinish();

		assertThat(result.get(HASH_ONE), containsInAnyOrder(recordOneOne, recordTwoOne));
	}

	@Test
	public void testDistanceZeroNumberOfGroups() throws Exception {
		runCutAndWaitForFinish();

		assertThat(result.get(HASH_TWO).size(), is(0));
		assertThat(result.get(HASH_THREE).size(), is(0));
		assertThat(result.get(HASH_SIX).size(), is(0));
	}

	@Test
	public void testDistanceOneGroupOne() throws Exception {
		imageSorter = createCUT();

		runCutAndWaitForFinish();

		assertThat(result.get(HASH_ONE), containsInAnyOrder(recordOneOne, recordTwoOne, recordThreeThree));
	}

	@Test
	public void testDistanceOneGroupTwo() throws Exception {
		imageSorter = createCUT();

		runCutAndWaitForFinish();

		assertThat(result.get(HASH_TWO), containsInAnyOrder(recordTwoTwo, recordThreeThree, recordSixSix));
	}

	@Test
	public void testDistanceOneGroupThree() throws Exception {
		imageSorter = createCUT();

		runCutAndWaitForFinish();

		assertThat(result.get(HASH_THREE),
				containsInAnyOrder(recordThreeThree, recordOneOne, recordTwoOne, recordTwoTwo));
	}

	@Test
	public void testDistanceOneGroupSix() throws Exception {
		imageSorter = createCUT();

		runCutAndWaitForFinish();

		assertThat(result.get(HASH_SIX), containsInAnyOrder(recordSixSix, recordTwoTwo));
	}

	@Test
	public void testNullPath() throws Exception {
		imageSorter = createCUT(null);

		runCutAndWaitForFinish();

		assertThat(result.get(HASH_ONE), containsInAnyOrder(recordOneOne, recordTwoOne, recordThreeThree));
	}

	@Test
	public void testTestPath() throws Exception {
		imageSorter = createCUT(TEST_PATH);

		runCutAndWaitForFinish();

		assertThat(result.get(HASH_THREE).size(), is(0));
	}

	@Test
	public void testSqlException() throws Exception {
		when(imageRepository.getAll()).thenThrow(new RepositoryException(TEST_EXCEPTION_MESSAGE));
		imageSorter = createCUT();
		
		runCutAndWaitForFinish();

		assertThat(result.get(HASH_ONE).size(), is(0));
	}
}
