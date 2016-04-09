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
import static org.mockito.Mockito.verify;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.IIOException;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.dozedoff.commonj.hash.ImagePHash;
import com.github.dozedoff.similarImage.db.BadFileRecord;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Persistence;
import com.github.dozedoff.similarImage.io.Statistics;

@RunWith(MockitoJUnitRunner.class)
public class ImageHashJobTest {
	@Mock
	private Persistence persistence;

	@Mock
	private ImagePHash phw;

	@Mock
	private Statistics statistics;

	private ImageHashJob imageLoadJob;

	private static Path testImage;

	@BeforeClass
	public static void setUpClass() throws Exception {
		testImage = Paths.get(Thread.currentThread().getContextClassLoader().getResource("testImage.jpg").toURI());
	}

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		imageLoadJob = new ImageHashJob(testImage, phw, persistence, statistics);
	}

	@Test
	public void testRunAddFile() throws Exception {
		imageLoadJob.run();

		verify(persistence).addRecord(new ImageRecord(testImage.toString(), 0));
	}

	@Test
	public void testRunIIOException() throws Exception {
		Mockito.doThrow(IIOException.class).when(persistence).addRecord(any(ImageRecord.class));
		imageLoadJob.run();

		verify(persistence).addBadFile(any(BadFileRecord.class));
	}
}
