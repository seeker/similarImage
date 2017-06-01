/*  Copyright (C) 2017  Nicholas Wright
    
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
package com.github.dozedoff.similarImage.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.junit.Before;
import org.junit.Test;

public class ImageUtilTest {
	private static final int IMAGE_SIZE = 40;
	private static final int IMAGE_DATA_LENGTH = 333;

	private BufferedImage jpgImage;
	private BufferedImage gifImage;
	private Path jpgPath;
	private Path gifPath;

	@Before
	public void setUp() throws Exception {
		jpgPath = Paths.get(Thread.currentThread().getContextClassLoader().getResource("testImage.jpg").toURI());
		gifPath = Paths.get(Thread.currentThread().getContextClassLoader().getResource("testImage.gif").toURI());

		jpgImage = ImageIO.read(Files.newInputStream(jpgPath));
		gifImage = ImageIO.read(Files.newInputStream(gifPath));
	}

	@Test
	public void testBytesToImage() throws Exception {

		BufferedImage image = ImageUtil.bytesToImage(ImageUtil.imageToBytes(jpgImage));

		assertThat(image.getHeight(), is(IMAGE_SIZE));
		assertThat(image.getWidth(), is(IMAGE_SIZE));
	}

	@Test
	public void testImageToBytesArraySize() throws Exception {
		byte[] data = ImageUtil.imageToBytes(jpgImage);

		assertThat(data.length, is(IMAGE_DATA_LENGTH));
	}

	@Test
	public void testImageToBytesHash() throws Exception {
		byte[] data = ImageUtil.imageToBytes(jpgImage);

		MessageDigest md = MessageDigest.getInstance("SHA");
		assertThat(Arrays.toString(md.digest(data)),
				is("[8, -11, -62, -80, -39, 126, -72, 17, -110, 2, -24, -86, 56, 4, -73, 6, -54, -47, 85, 100]"));
	}

	@Test
	public void testLoadImageJpg() throws Exception {
		BufferedImage image = ImageUtil.loadImage(jpgPath);

		assertThat(image.getHeight(), is(IMAGE_SIZE));
		assertThat(image.getWidth(), is(IMAGE_SIZE));
	}

	@Test
	public void testLoadImageGif() throws Exception {
		BufferedImage image = ImageUtil.loadImage(gifPath);

		assertThat(image.getHeight(), is(IMAGE_SIZE));
		assertThat(image.getWidth(), is(IMAGE_SIZE));
	}
}
