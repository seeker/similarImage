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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.LinkedList;

import javax.swing.JComponent;
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
import com.github.dozedoff.similarImage.gui.OperationsMenu;

public class DuplicateEntry extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(DuplicateEntry.class);
	private final ImageInfo imageInfo;
	private JLabel image;
	private final Path imagePath;
	private final SimilarImage parent;
	
	public DuplicateEntry(SimilarImage parent, Path imagePath, Dimension thumbDimension) {
		super();
		this.imagePath = imagePath;
		this.parent = parent;
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
		this.addMouseListener(new ClickListener());
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
	
	public void ignore() {
		ImageRecord ir = new ImageRecord(imagePath.toString(), imageInfo.getpHash());
		parent.ignoreImage(ir);
	}
	
	private void displayFullImage() {
		JPanel imagePanel = new JPanel(new MigLayout());
		JScrollPane scroll = new JScrollPane(imagePanel);
		
		JFrame imageFrame = new JFrame(imagePath.toString());
		imageFrame.setLayout(new MigLayout());
		JLabel largeImage = new JLabel("No Image");
		
		try {
			largeImage = SubsamplingImageLoader.loadAsLabel(imagePath, new Dimension(4000, 4000));
		} catch (Exception e) {
			logger.warn("Unable to load full image {} - {}", imagePath, e.getMessage());
		}
		
		imagePanel.add(largeImage);
		imageFrame.add(scroll);
		imageFrame.pack();
		imageFrame.setVisible(true);
	}
	
	class ClickListener extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent e) {
			if(e.getButton() == MouseEvent.BUTTON1){
				displayFullImage();
			}
		}
	}
}
