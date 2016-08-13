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
package com.github.dozedoff.similarImage.duplicate;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.awt.Dimension;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.BeforeClass;
import org.junit.Test;

public class ImageInfoTest {
	private static ImageInfo imageInfo, imageInfoInvalid;
	private static Path testImage, invalidImage;

	@BeforeClass
	public static void setUpClass() throws Exception {
		testImage = Paths.get(Thread.currentThread().getContextClassLoader().getResource("testImage.jpg").toURI());
		invalidImage = Paths.get(Thread.currentThread().getContextClassLoader().getResource("notAnImage.jpg").toURI());

		imageInfo = new ImageInfo(testImage, 42);
		imageInfoInvalid = new ImageInfo(invalidImage, 22);
	}

	@Test
	public void testGetPath() throws Exception {
		assertThat(imageInfo.getPath(), is(testImage));
	}

	@Test
	public void testGetPathInvalidImage() throws Exception {
		assertThat(imageInfoInvalid.getPath(), is(invalidImage));
	}

	@Test
	public void testGetDimension() throws Exception {
		assertThat(imageInfo.getDimension(), is(new Dimension(40, 40)));
	}

	@Test
	public void testGetDimensionInvalidImage() throws Exception {
		assertThat(imageInfoInvalid.getDimension(), is(new Dimension(0, 0)));
	}

	@Test
	public void testGetSize() throws Exception {
		assertThat(imageInfo.getSize(), is(1782L));
	}

	@Test
	public void testGetSizeInvalidImage() throws Exception {
		assertThat(imageInfoInvalid.getSize(), is(-1L));
	}

	@Test
	public void testGetSizeInvalidFile() throws Exception {
		ImageInfo info = new ImageInfo(Paths.get("foo"), 2);
		assertThat(info.getSize(), is(-1L));
	}

	@Test
	public void testGetpHash() throws Exception {
		assertThat(imageInfo.getpHash(), is(42L));
	}

	@Test
	public void testGetpHashInvalidImage() throws Exception {
		assertThat(imageInfoInvalid.getpHash(), is(0L));
	}

	@Test
	public void testGetpHashInvalidFile() throws Exception {
		ImageInfo info = new ImageInfo(Paths.get("foo"), 2);
		assertThat(info.getpHash(), is(2L));
	}

	@Test
	public void testGetSizePerPixel() throws Exception {
		assertThat(imageInfo.getSizePerPixel(), is(1.11375));
	}

	@Test
	public void testGetSizePerPixelInvalidImage() throws Exception {
		assertThat(imageInfoInvalid.getSizePerPixel(), is(0.0));
	}
}
