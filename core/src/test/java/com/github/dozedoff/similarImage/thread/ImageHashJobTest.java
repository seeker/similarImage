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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
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
import org.mockito.junit.MockitoJUnitRunner;

import com.github.dozedoff.commonj.hash.ImagePHash;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;
import com.github.dozedoff.similarImage.io.HashAttribute;
import com.github.dozedoff.similarImage.io.Statistics;

@RunWith(MockitoJUnitRunner.class)
public class ImageHashJobTest {
	@Mock
	private ImageRepository imageRepository;

	@Mock
	private ImagePHash phw;

	@Mock
	private Statistics statistics;

	@Mock
	private HashAttribute hashAttributeMock;

	private ImageHashJob imageLoadJob;

	private static Path testImage;

	@BeforeClass
	public static void setUpClass() throws Exception {
		testImage = Paths.get(Thread.currentThread().getContextClassLoader().getResource("testImage.jpg").toURI());
	}

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		imageLoadJob = new ImageHashJob(testImage, phw, imageRepository, statistics);
	}

	@Test
	public void testRunAddFile() throws Exception {
		imageLoadJob.run();

		verify(imageRepository).store(new ImageRecord(testImage.toString(), 0));
	}

	@Test
	public void testRunIIOException() throws Exception {
		when(phw.getLongHash(any(InputStream.class))).thenThrow(IIOException.class);

		imageLoadJob.run();

		verify(statistics).incrementFailedFiles();
	}

	@Test
	public void testWriteExtendedAttributes() throws Exception {
		imageLoadJob.setHashAttribute(hashAttributeMock);
		imageLoadJob.run();

		verify(hashAttributeMock).writeHash(testImage, 0);
	}

	@Test
	public void testDoNotWriteExtendedAttributes() throws Exception {
		Mockito.doThrow(RepositoryException.class).when(imageRepository).store(any(ImageRecord.class));
		imageLoadJob.setHashAttribute(hashAttributeMock);
		imageLoadJob.run();

		verify(hashAttributeMock, never()).writeHash(testImage, 0);
	}
}
