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
package com.github.dozedoff.similarImage.gui;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.duplicate.ImageInfo;
import com.github.dozedoff.similarImage.result.Result;
import com.github.dozedoff.similarImage.util.ImageUtil;
import com.google.common.cache.LoadingCache;

public class ResultPresenter {
	private static final Logger logger = LoggerFactory.getLogger(ResultPresenter.class);

	private final LoadingCache<Result, BufferedImage> thumbnailCache;

	private final ImageInfo imageInfo;
	private ResultView view;
	private final Result result;

	/**
	 * Creates a new {@link ResultPresenter} to present the given {@link Result}.
	 * 
	 * @param result
	 *            represented by this presenter instance
	 * @param thumbnailCache
	 *            cache to speed up loading thumbnails
	 */
	public ResultPresenter(Result result, LoadingCache<Result, BufferedImage> thumbnailCache) {
		this.result = result;
		ImageRecord ir = result.getImageRecord();
		this.imageInfo = new ImageInfo(Paths.get(ir.getPath()), ir.getpHash());
		this.thumbnailCache = thumbnailCache;
	}

	private void addImageInfo() {
		view.createLable("Path: " + imageInfo.getPath());
		view.createLable("Size: " + imageInfo.getSize() / 1024 + " kb");
		Dimension dim = imageInfo.getDimension();
		view.createLable("Dimension: " + (int) dim.getWidth() + "x" + (int) dim.getHeight());
		view.createLable("pHash: " + imageInfo.getpHash());
		view.createLable("Size per Pixel: " + imageInfo.getSizePerPixel());
	}

	private void loadThumbnail() {
		try {
			logger.debug("{} in cache: {}", imageInfo.getPath(),
					thumbnailCache.getIfPresent(result));
			BufferedImage resized = thumbnailCache.get(result);
			JLabel image = imageAsLabel(resized);
			view.setImage(image);
		} catch (Exception e) {
			logger.warn("Could not load image thumbnail for {} - {}", imageInfo.getPath(), e.getMessage());
		}
	}

	private static JLabel imageAsLabel(Image image) {
		return new JLabel(new ImageIcon(image), JLabel.CENTER);
	}

	public Path getImagePath() {
		return imageInfo.getPath();
	}

	public void displayFullImage() {
		JLabel largeImage = new JLabel("No Image");

		try {
			BufferedImage bi = ImageUtil.loadImage(getImagePath());
			largeImage = imageAsLabel(bi);
		} catch (Exception e) {
			logger.warn("Unable to load full image {} - {}", getImagePath(), e.getMessage());
		}

		view.displayFullImage(largeImage, getImagePath());
	}

	public void setView(ResultView view) {
		this.view = view;

		loadThumbnail();
		addImageInfo();
	}
}
