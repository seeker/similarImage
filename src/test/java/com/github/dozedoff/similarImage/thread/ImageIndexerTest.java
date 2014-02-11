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

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.dozedoff.similarImage.gui.SimilarImageView;
import com.github.dozedoff.similarImage.hash.PhashWorker;
import com.github.dozedoff.similarImage.io.ImageProducer;

@RunWith(MockitoJUnitRunner.class)
public class ImageIndexerTest {
	@Mock
	private PhashWorker phw;

	@Mock
	private SimilarImageView gui;

	@Mock
	private ImageProducer producer;

	private ImageIndexer imageIndexer;

	private Path testDirectory;
	private static final int NUM_OF_FILES = 5;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		testDirectory = Files.createTempDirectory("ImageIndexerTest");
		createTempFiles(testDirectory);

		imageIndexer = new ImageIndexer(testDirectory.toString(), gui, producer, phw);
	}

	private void createTempFiles(Path tempDir) throws IOException {
		for (int i = 0; i < NUM_OF_FILES; i++) {
			Files.createTempFile(testDirectory, "Test File", ".jpg");
		}
	}

	@Test
	public void testRun() throws Exception {
		imageIndexer.run();

		verify(gui).setTotalFiles(eq(NUM_OF_FILES));
		verify(producer).addToLoad(anyListOf(Path.class));
	}

	@Test
	public void testRunInvalidPath() throws Exception {
		imageIndexer = new ImageIndexer("foo", gui, producer, phw);

		imageIndexer.run();

		verify(gui, never()).setTotalFiles(anyInt());
		verify(producer).addToLoad(anyListOf(Path.class));
	}

	@Test
	public void testKillAll() throws Exception {
		imageIndexer.killAll();

		verify(phw).clear();
		verify(producer).clear();
	}

}
