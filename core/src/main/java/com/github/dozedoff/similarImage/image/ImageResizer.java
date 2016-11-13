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
package com.github.dozedoff.similarImage.image;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import com.github.dozedoff.commonj.util.ImageUtil;

public class ImageResizer {
	private final int size;

	/**
	 * Create a {@link ImageResizer} that will resize images to squares of the given size.
	 * 
	 * @param size
	 *            to resize to
	 */
	public ImageResizer(int size) {
		this.size = size;
	}

	/**
	 * Resize an image.
	 * 
	 * @param is
	 *            {@link InputStream} of the image
	 * @return resized image as a byte array
	 * @throws IOException
	 *             if there is an error processing the image
	 */
	public byte[] resize(InputStream is) throws IOException {

		BufferedImage originalImage = ImageUtil.readImage(is);

		BufferedImage resized = ImageUtil.resizeImage(originalImage, size, size);
		originalImage.flush();

		byte[] resizedBytes = com.github.dozedoff.similarImage.util.ImageUtil.imageToBytes(resized);
		resized.flush();

		return resizedBytes;
	}
}
