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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.IIOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.dozedoff.commonj.hash.ImagePHash;
import com.github.dozedoff.commonj.util.Pair;
import com.github.dozedoff.similarImage.db.DBWriter;
import com.github.dozedoff.similarImage.db.ImageRecord;

@RunWith(MockitoJUnitRunner.class)
public class ImageHashJobTest {
	private final static String TEST_MSG = "This is a test";

	@Mock
	private DBWriter dbWriter;

	@Mock
	private ImagePHash phash;

	private List<Pair<Path, BufferedImage>> work;

	private ImageHashJob imageHashJob;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		work = new LinkedList<>();
		work.add(new Pair<Path, BufferedImage>(Paths.get(""), new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY)));

		imageHashJob = new ImageHashJob(work, dbWriter, phash);
	}

	@Test
	public void testRun() throws Exception {
		imageHashJob.run();

		verify(dbWriter).add(anyListOf(ImageRecord.class));
	}

	@Test
	public void testRunIIOException() throws Exception {
		when(phash.getLongHashScaledImage(any(BufferedImage.class))).thenThrow(new IIOException(TEST_MSG));

		imageHashJob.run();

		verify(dbWriter).add(anyListOf(ImageRecord.class));
	}

	@Test
	public void testRunIOException() throws Exception {
		when(phash.getLongHashScaledImage(any(BufferedImage.class))).thenThrow(new IOException(TEST_MSG));

		imageHashJob.run();

		verify(dbWriter).add(anyListOf(ImageRecord.class));
	}

	@Test
	public void testRunSQLException() throws Exception {
		when(phash.getLongHashScaledImage(any(BufferedImage.class))).thenThrow(new SQLException(TEST_MSG));

		imageHashJob.run();

		verify(dbWriter).add(anyListOf(ImageRecord.class));
	}

	@Test
	public void testRunException() throws Exception {
		when(phash.getLongHashScaledImage(any(BufferedImage.class))).thenThrow(new Exception(TEST_MSG));

		imageHashJob.run();

		verify(dbWriter).add(anyListOf(ImageRecord.class));
	}

}
