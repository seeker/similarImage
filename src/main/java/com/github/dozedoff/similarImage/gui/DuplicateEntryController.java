/*  Copyright (C) 2013  Nicholas Wright
    
    This file is part of similarImage - A similar image finder using pHash
    
    mmut is free software: you can redistribute it and/or modify
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
import java.nio.file.Path;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.commonj.image.SubsamplingImageLoader;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Persistence;
import com.github.dozedoff.similarImage.duplicate.ImageInfo;

public class DuplicateEntryController implements View {
	private static final Logger logger = LoggerFactory.getLogger(DuplicateEntryController.class);
	private final ImageInfo imageInfo;
	private final SimilarImageController parent;
	private Dimension thumbDimension;
	private final DuplicateEntryView view;

	public DuplicateEntryController(SimilarImageController parent, ImageInfo imageInfo, Persistence persistence, Dimension thumbDimension) {
		super();
		this.parent = parent;
		this.imageInfo = imageInfo;
		this.thumbDimension = thumbDimension;

		OperationsMenu opMenu = new OperationsMenu(this, persistence);

		view = new DuplicateEntryView(this, opMenu);

		loadThumbnail();
		addImageInfo();
	}

	private void addImageInfo() {
		view.createLable("Path: " + imageInfo.getPath());
		view.createLable("Size: " + imageInfo.getSize() / 1024 + " kb");
		Dimension dim = imageInfo.getDimension();
		view.createLable("Dimension: " + dim.getWidth() + "x" + dim.getHeight());
		view.createLable("pHash: " + imageInfo.getpHash());
		view.createLable("Size per Pixel: " + imageInfo.getSizePerPixel());
	}

	private void loadThumbnail() {
		try {
			JLabel image = SubsamplingImageLoader.loadAsLabel(this.imageInfo.getPath(), thumbDimension);
			view.setImage(image);
		} catch (Exception e) {
			logger.warn("Could not load image thumbnail for {} - {}", imageInfo.getPath(), e.getMessage());
		}
	}

	public Path getImagePath() {
		return imageInfo.getPath();
	}

	public ImageInfo getImageInfo() {
		return imageInfo;
	}

	public void ignore() {
		ImageRecord ir = new ImageRecord(getImagePath().toString(), imageInfo.getpHash());
		parent.ignoreImage(ir);
	}

	public void displayFullImage() {
		JLabel largeImage = new JLabel("No Image");

		try {
			largeImage = SubsamplingImageLoader.loadAsLabel(getImagePath(), new Dimension(4000, 4000));
		} catch (Exception e) {
			logger.warn("Unable to load full image {} - {}", getImagePath(), e.getMessage());
		}

		new FullImageView(largeImage, getImagePath());
	}

	@Override
	public JComponent getView() {
		return view.getView();
	}
}
