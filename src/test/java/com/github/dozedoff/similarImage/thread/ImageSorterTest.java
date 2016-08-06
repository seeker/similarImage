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
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Persistence;
import com.github.dozedoff.similarImage.duplicate.SortSimilar;
import com.github.dozedoff.similarImage.gui.SimilarImageController;
import com.github.dozedoff.similarImage.gui.SimilarImageView;
import com.github.dozedoff.similarImage.io.Statistics;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

@RunWith(MockitoJUnitRunner.class)
public class ImageSorterTest {
	private static final long HASH_ONE = 1L;
	private static final long HASH_TWO = 2L;
	private static final long HASH_THREE = 3L;
	private static final long HASH_SIX = 6L;

	private static final String TEST_PATH = "/foo/bar/";

	private static final String TEST_EXCEPTION_MESSAGE = "Testing...";

	private SimilarImageController controllerMock;

	@Mock
	private SimilarImageView gui;

	@Mock
	private Persistence persistence;

	@Mock
	private SortSimilar sorter;

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

	@Before
	public void setUp() throws Exception {
		path = Files.createTempFile(ImageSorterTest.class.getSimpleName(), ".dat");

		List<ImageRecord> records = buildRecords();
		when(persistence.getAllRecords()).thenReturn(records);
		when(persistence.filterByPath(path)).thenReturn(Arrays.asList(recordOneOne, recordTwoOne, recordTwoTwo));
		
		result = MultimapBuilder.hashKeys().hashSetValues().build();

		controllerMock = new SimilarImageController(persistence, Executors.newCachedThreadPool(), statistics);
		imageSorter = new ImageSorter(DISTANCE, "", controllerMock, persistence);
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

	@Test
	public void testDistanceZeroRecords() throws Exception {
		runCutAndWaitForFinish();

		assertThat(controllerMock.getGroup(HASH_ONE), containsInAnyOrder(recordOneOne, recordTwoOne));
	}

	@Test
	public void testDistanceZeroNumberOfGroups() throws Exception {
		runCutAndWaitForFinish();

		assertThat(controllerMock.getGroup(HASH_TWO).size(), is(0));
		assertThat(controllerMock.getGroup(HASH_THREE).size(), is(0));
		assertThat(controllerMock.getGroup(HASH_SIX).size(), is(0));
	}

	@Test
	public void testDistanceOneGroupOne() throws Exception {
		imageSorter = new ImageSorter(1, "", controllerMock, persistence);

		runCutAndWaitForFinish();

		assertThat(controllerMock.getGroup(HASH_ONE), containsInAnyOrder(recordOneOne, recordTwoOne, recordThreeThree));
	}

	@Test
	public void testDistanceOneGroupTwo() throws Exception {
		imageSorter = new ImageSorter(1, "", controllerMock, persistence);

		runCutAndWaitForFinish();

		assertThat(controllerMock.getGroup(HASH_TWO), containsInAnyOrder(recordTwoTwo, recordThreeThree, recordSixSix));
	}

	@Test
	public void testDistanceOneGroupThree() throws Exception {
		imageSorter = new ImageSorter(1, "", controllerMock, persistence);

		runCutAndWaitForFinish();

		assertThat(controllerMock.getGroup(HASH_THREE), containsInAnyOrder(recordThreeThree, recordOneOne, recordTwoOne, recordTwoTwo));
	}

	@Test
	public void testDistanceOneGroupSix() throws Exception {
		imageSorter = new ImageSorter(1, "", controllerMock, persistence);

		runCutAndWaitForFinish();

		assertThat(controllerMock.getGroup(HASH_SIX), containsInAnyOrder(recordSixSix, recordTwoTwo));
	}

	@Test
	public void testNullPath() throws Exception {
		imageSorter = new ImageSorter(DISTANCE, null, controllerMock, persistence);

		runCutAndWaitForFinish();

		assertThat(controllerMock.getGroup(HASH_ONE), containsInAnyOrder(recordOneOne, recordTwoOne));
	}

	@Test
	public void testTestPath() throws Exception {
		imageSorter = new ImageSorter(1, TEST_PATH, controllerMock, persistence);

		runCutAndWaitForFinish();

		assertThat(controllerMock.getGroup(HASH_THREE).size(), is(0));
	}

	@Test
	public void testSqlException() throws Exception {
		when(persistence.getAllRecords()).thenThrow(new SQLException(TEST_EXCEPTION_MESSAGE));
		imageSorter = new ImageSorter(1, "", controllerMock, persistence);
		
		runCutAndWaitForFinish();

		assertThat(controllerMock.getGroup(HASH_ONE).size(), is(0));
	}
}
