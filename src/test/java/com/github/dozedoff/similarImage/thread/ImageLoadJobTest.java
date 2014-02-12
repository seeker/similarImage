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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.dozedoff.similarImage.db.BadFileRecord;
import com.github.dozedoff.similarImage.db.Persistence;
import com.github.dozedoff.similarImage.hash.PhashWorker;

@RunWith(MockitoJUnitRunner.class)
public class ImageLoadJobTest {
	@Mock
	private List<Path> files;

	@Mock
	private Persistence persistence;

	@Mock
	private PhashWorker phw;

	private ImageLoadJob imageLoadJob;

	private static Path testImage, notAnImage;
	private static String ERROR_MSG = "This is a test";

	@BeforeClass
	public static void setUpClass() throws Exception {
		testImage = Paths.get(Thread.currentThread().getContextClassLoader().getResource("testImage.jpg").toURI());
		notAnImage = Paths.get(Thread.currentThread().getContextClassLoader().getResource("notAnImage.jpg").toURI());
	}

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		List<Path> files = new LinkedList<>();
		files.add(testImage);

		imageLoadJob = new ImageLoadJob(files, phw, persistence);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testRun() throws Exception {
		imageLoadJob.run();

		verify(persistence).isBadFile(testImage);
		verify(persistence).isPathRecorded(testImage);
		verify(phw).toHash(anyList());
	}

	@Test
	public void testRunBadFile() throws Exception {
		when(persistence.isBadFile(testImage)).thenReturn(true);

		imageLoadJob.run();

		verify(persistence).isBadFile(testImage);
	}

	@Test
	public void testRunRecordedFile() throws Exception {
		when(persistence.isPathRecorded(testImage)).thenReturn(true);

		imageLoadJob.run();

		verify(persistence).isPathRecorded(testImage);
	}

	@Test
	public void testRunBadImage() throws Exception {
		LinkedList<Path> files = new LinkedList<>();
		files.add(notAnImage);

		imageLoadJob = new ImageLoadJob(files, phw, persistence);

		imageLoadJob.run();

		verify(persistence).isBadFile(notAnImage);
		verify(persistence).isPathRecorded(notAnImage);
		verify(persistence).addBadFile(Mockito.any(BadFileRecord.class));
	}

	@Test
	public void testRunBadImageSQLException() throws Exception {
		Mockito.doThrow(new SQLException(ERROR_MSG)).when(persistence).addBadFile(Mockito.any(BadFileRecord.class));

		LinkedList<Path> files = new LinkedList<>();
		files.add(notAnImage);

		imageLoadJob = new ImageLoadJob(files, phw, persistence);

		imageLoadJob.run();

		verify(persistence).isBadFile(notAnImage);
		verify(persistence).isPathRecorded(notAnImage);
		verify(persistence).addBadFile(Mockito.any(BadFileRecord.class));
	}

	@Test
	public void testRunBadFileSQLException() throws Exception {
		when(persistence.isBadFile(testImage)).thenThrow(new SQLException(ERROR_MSG));

		imageLoadJob.run();

		verify(persistence).isBadFile(testImage);
		verify(persistence, never()).addBadFile(Mockito.any(BadFileRecord.class));
	}

	@Test
	public void testRunIOerror() throws Exception {
		LinkedList<Path> files = new LinkedList<>();
		Path noPath = Paths.get("");

		files.add(noPath);

		imageLoadJob = new ImageLoadJob(files, phw, persistence);

		imageLoadJob.run();

		verify(persistence).isBadFile(noPath);
		verify(persistence).isPathRecorded(noPath);
		verify(persistence, never()).addBadFile(Mockito.any(BadFileRecord.class));
	}

	@Test
	public void testGetJobSize() throws Exception {
		assertThat(imageLoadJob.getJobSize(), is(1));
	}
}
