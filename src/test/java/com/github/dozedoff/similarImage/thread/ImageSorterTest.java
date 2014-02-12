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
package com.github.dozedoff.similarImage.thread;

import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Persistence;
import com.github.dozedoff.similarImage.duplicate.SortSimilar;
import com.github.dozedoff.similarImage.gui.SimilarImageView;

@RunWith(MockitoJUnitRunner.class)
public class ImageSorterTest {
	@Mock
	private SimilarImageView gui;

	@Mock
	private Persistence persistence;

	@Mock
	private SortSimilar sorter;

	private ImageSorter imageSorter;

	private static final int DISTANCE = 2;
	private static Path path;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		path = Files.createTempFile(ImageSorterTest.class.getSimpleName(), ".dat");

		imageSorter = new ImageSorter(DISTANCE, path.toString(), gui, sorter, persistence);
	}

	@Test
	public void testRun() throws Exception {
		imageSorter.run();

		verify(persistence).filterByPath(path);
		verify(sorter).buildTree(anyListOf(ImageRecord.class));
		verify(sorter).sortHammingDistance(eq(DISTANCE), anyListOf(ImageRecord.class));
		verify(sorter).removeSingleImageGroups();
		verify(gui).populateGroupList(anyListOf(Long.class));
	}

	@Test
	public void testRunDistanceZero() throws Exception {
		imageSorter = new ImageSorter(0, path.toString(), gui, sorter, persistence);

		imageSorter.run();

		verify(persistence).filterByPath(path);
		verify(sorter).buildTree(anyListOf(ImageRecord.class));
		verify(sorter).sortExactMatch(anyListOf(ImageRecord.class));
		verify(sorter).removeSingleImageGroups();
		verify(gui).populateGroupList(anyListOf(Long.class));
	}

	@Test
	public void testRunNullPath() throws Exception {
		imageSorter = new ImageSorter(DISTANCE, null, gui, sorter, persistence);

		imageSorter.run();

		verify(persistence).getAllRecords();
		verify(sorter).buildTree(anyListOf(ImageRecord.class));
		verify(sorter).sortHammingDistance(eq(DISTANCE), anyListOf(ImageRecord.class));
		verify(sorter).removeSingleImageGroups();
		verify(gui).populateGroupList(anyListOf(Long.class));
	}

	@Test
	public void testRunEmptyPath() throws Exception {
		imageSorter = new ImageSorter(DISTANCE, "", gui, sorter, persistence);

		imageSorter.run();

		verify(persistence).getAllRecords();
		verify(sorter).buildTree(anyListOf(ImageRecord.class));
		verify(sorter).sortHammingDistance(eq(DISTANCE), anyListOf(ImageRecord.class));
		verify(sorter).removeSingleImageGroups();
		verify(gui).populateGroupList(anyListOf(Long.class));
	}

	@Test
	public void testRunSQLException() throws Exception {
		when(persistence.getAllRecords()).thenThrow(new SQLException("This is a test"));
		imageSorter = new ImageSorter(DISTANCE, "", gui, sorter, persistence);

		imageSorter.run();

		verify(persistence).getAllRecords();
		verify(sorter).buildTree(anyListOf(ImageRecord.class));
		verify(sorter).sortHammingDistance(eq(DISTANCE), anyListOf(ImageRecord.class));
		verify(sorter).removeSingleImageGroups();
		verify(gui).populateGroupList(anyListOf(Long.class));
	}

	@Test
	public void testReRunSamePath() throws Exception {
		imageSorter = new ImageSorter(DISTANCE, "", gui, sorter, persistence);
		imageSorter.run();

		setUp();

		imageSorter = new ImageSorter(DISTANCE, "", gui, sorter, persistence);
		imageSorter.run();

		verify(sorter, never()).buildTree(anyListOf(ImageRecord.class));
	}
}
