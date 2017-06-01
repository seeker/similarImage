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
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.duplicate.ImageInfo;
import com.github.dozedoff.similarImage.result.Result;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import at.dhyan.open_imaging.GifDecoder;

public class ResultPresenter {
	private static final Logger logger = LoggerFactory.getLogger(ResultPresenter.class);

	private static final LoadingCache<Result, BufferedImage> thumbnailCache = CacheBuilder.newBuilder()
			.softValues()
			.recordStats()
			.build(new CacheLoader<Result, BufferedImage>() {
				@Override
				public BufferedImage load(Result key) throws Exception {
					BufferedImage bi = loadImage(Paths.get(key.getImageRecord().getPath()));
					return Scalr.resize(bi, Method.AUTOMATIC, 500);
				}
			});

	private final ImageInfo imageInfo;
	private ResultView view;
	private final Result result;

	/**
	 * Creates a new {@link ResultPresenter} to present the given {@link Result}.
	 * 
	 * @param result
	 *            represented by this presenter instance
	 */
	public ResultPresenter(Result result) {
		this.result = result;
		ImageRecord ir = result.getImageRecord();
		this.imageInfo = new ImageInfo(Paths.get(ir.getPath()), ir.getpHash());
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
			logger.info("{} in cache (Hit:{}): {}", imageInfo.getPath(), thumbnailCache.stats(),
					thumbnailCache.getIfPresent(result));
			BufferedImage resized = thumbnailCache.get(result);
			JLabel image = imageAsLabel(resized);
			view.setImage(image);
		} catch (Exception e) {
			logger.warn("Could not load image thumbnail for {} - {}", imageInfo.getPath(), e.getMessage());
		}
	}

	private static BufferedImage loadImage(Path path) throws IOException {
		try (InputStream is = new BufferedInputStream(Files.newInputStream(path))) {
			BufferedImage bi;

			try {
				bi = ImageIO.read(is);
			} catch (ArrayIndexOutOfBoundsException e) {
				bi = GifDecoder.read(is).getFrame(0);
			}

			return bi;
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
			BufferedImage bi = loadImage(getImagePath());
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
