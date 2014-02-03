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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.commonj.image.SubsamplingImageLoader;
import com.github.dozedoff.similarImage.app.SimilarImage;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Persistence;
import com.github.dozedoff.similarImage.duplicate.ImageInfo;

public class DuplicateEntry extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(DuplicateEntry.class);
	private final ImageInfo imageInfo;
	private JLabel image;
	private final SimilarImage parent;

	private final DuplicateEntryView view;

	public DuplicateEntry(SimilarImage parent, ImageInfo imageInfo, Persistence persistence, Dimension thumbDimension) {
		super();
		this.parent = parent;
		this.imageInfo = imageInfo;

		view = new DuplicateEntryView(this);

		this.setLayout(new MigLayout("wrap"));
		image = new JLabel("NO IMAGE");

		try {
			this.image = SubsamplingImageLoader.loadAsLabel(this.imageInfo.getPath(), thumbDimension);
		} catch (Exception e) {
			logger.warn("Could not load image thumbnail for {} - {}", imageInfo.getPath(), e.getMessage());
		}

		add(image);
		addImageInfo();
		new OperationsMenu(this, persistence);
		this.addMouseListener(new ClickListener());
	}

	private void addImageInfo() {
		view.createLable("Path: " + imageInfo.getPath());
		view.createLable("Size: " + imageInfo.getSize() / 1024 + " kb");
		Dimension dim = imageInfo.getDimension();
		view.createLable("Dimension: " + dim.getWidth() + "x" + dim.getHeight());
		view.createLable("pHash: " + imageInfo.getpHash());
		view.createLable("Size per Pixel: " + imageInfo.getSizePerPixel());
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

	private void displayFullImage() {
		JPanel imagePanel = new JPanel(new MigLayout());
		JScrollPane scroll = new JScrollPane(imagePanel);

		JFrame imageFrame = new JFrame(getImagePath().toString());
		imageFrame.setLayout(new MigLayout());
		JLabel largeImage = new JLabel("No Image");

		try {
			largeImage = SubsamplingImageLoader.loadAsLabel(getImagePath(), new Dimension(4000, 4000));
		} catch (Exception e) {
			logger.warn("Unable to load full image {} - {}", getImagePath(), e.getMessage());
		}

		imagePanel.add(largeImage);
		imageFrame.add(scroll);
		imageFrame.pack();
		imageFrame.setVisible(true);
	}

	class ClickListener extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getButton() == MouseEvent.BUTTON1) {
				displayFullImage();
			}
		}
	}
}
