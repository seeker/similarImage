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
package com.github.dozedoff.similarImage.duplicate;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageInfo {
	private final static Logger logger = LoggerFactory.getLogger(ImageInfo.class);
	private Path path;
	private Dimension dimension = new Dimension();
	private long size = -1;
	private long pHash = 0;
	private double sizePerPixel = 0;

	public ImageInfo(Path path, long pHash) {
		this.path = path;
		this.pHash = pHash;

		getImageData();
	}

	private void getImageData() {
		InputStream is = null;
		try {
			is = new BufferedInputStream(Files.newInputStream(path));
			BufferedImage img = ImageIO.read(is);

			if (img == null) {
				logger.warn("Failed to process image {}", path);
				this.pHash = 0;
				return;
			}

			dimension.setSize(img.getWidth(), img.getHeight());
			size = Files.size(path);
			calculateSpp();
		} catch (IOException e) {
			logger.warn("Unable to get info for file {} - {}", path, e.getMessage());
		} finally {
			try {
				if (is != null) {
					is.close();
				}
			} catch (IOException e) {
				logger.warn("Unable to close input stream - {}", e.getMessage());
			}
		}
	}

	private void calculateSpp() {
		double height = dimension.getHeight();
		double width = dimension.getWidth();

		double area = height * width;

		if (area == 0) {
			return;
		}

		sizePerPixel = size / area;
	}

	public Path getPath() {
		return path;
	}

	public Dimension getDimension() {
		return dimension;
	}

	public long getSize() {
		return size;
	}

	public long getpHash() {
		return pHash;
	}

	public double getSizePerPixel() {
		return sizePerPixel;
	}
}
