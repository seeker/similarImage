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

import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.never;
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
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.github.dozedoff.commonj.hash.ImagePHash;
import com.github.dozedoff.commonj.util.Pair;
import com.github.dozedoff.similarImage.db.DBWriter;
import com.github.dozedoff.similarImage.db.ImageRecord;

public class PhashWorkerTest {
	private PhashWorker phw;
	private static Path testImage;

	private static final int NUM_OF_TEST_IMAGES = 10000;

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
		phw.shutdown();
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

	@Ignore
	@Test
	public void testHashImage() throws Exception {
		phw.toHash(createWork(1));
		phw.shutdown();

		verify(dbWriter).add(anyListOf(ImageRecord.class));
		// TODO needs more accurate test
	}

	@Ignore
	@Test(timeout = 6000)
	public void testStopWorker() throws Exception {
		List<Pair<Path, BufferedImage>> work = createWork(NUM_OF_TEST_IMAGES);
		phw.toHash(work);

		phw.shutdown();

		verify(dbWriter, never()).add(anyListOf(ImageRecord.class));
	}
}
