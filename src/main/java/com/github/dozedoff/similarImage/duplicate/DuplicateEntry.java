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
package com.github.dozedoff.similarImage.duplicate;

import java.awt.Dimension;
import java.nio.file.Path;
import java.util.LinkedList;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.commonj.image.SubsamplingImageLoader;
import com.github.dozedoff.similarImage.gui.OperationsMenu;

public class DuplicateEntry extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(DuplicateEntry.class);
	private final ImageInfo imageInfo;
	private JLabel image;
	private final Path imagePath;
	
	public DuplicateEntry(Path imagePath, Dimension thumbDimension) {
		super();
		this.imagePath = imagePath;
		this.setLayout(new MigLayout("wrap"));
		image = new JLabel("NO IMAGE");
		imageInfo = new ImageInfo(imagePath);
		
		try {
			this.image = SubsamplingImageLoader.loadAsLabel(imagePath, thumbDimension);
		} catch (Exception e) {
			logger.warn("Could not load image thumbnail for {} - {}", imageInfo.getPath(), e.getMessage());
		}
		
		add(image);
		addImageInfo();
		new OperationsMenu(this);
	}
	
	private void addImageInfo() {
		LinkedList<JComponent> components = new LinkedList<JComponent>();
		Path path = imageInfo.getPath();
		ImageInfo iInfo = new ImageInfo(path);
		components.add(new JLabel("Path: " + iInfo.getPath()));
		components.add(new JLabel("Size: " + iInfo.getSize()/1024 + " kb"));
		Dimension dim = iInfo.getDimension();
		components.add(new JLabel("Dimension: " + dim.getWidth() + "x" + dim.getHeight()));
		components.add(new JLabel("pHash: " + iInfo.getpHash()));
		components.add(new JLabel("Size per Pixel: " + iInfo.getSizePerPixel()));
		
		for (JComponent jc : components) {
			this.add(jc);
		}
	}
	
	public Path getImagePath() {
		return imagePath;
	}
	
	public ImageInfo getImageInfo() {
		return imageInfo;
	}
}
