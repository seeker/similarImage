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
package com.github.dozedoff.similarImage.gui;

import java.awt.Dimension;
import java.nio.file.Path;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.commonj.image.SubsamplingImageLoader;

public class FullImage {
	private static final Logger logger = LoggerFactory.getLogger(FullImage.class);

	public FullImage(Path path) {
		JPanel imagePanel = new JPanel(new MigLayout());
		JScrollPane scroll = new JScrollPane(imagePanel);

		JFrame imageFrame = new JFrame(path.toString());
		imageFrame.setLayout(new MigLayout());
		JLabel largeImage = new JLabel("No Image");

		try {
			largeImage = SubsamplingImageLoader.loadAsLabel(path, new Dimension(4000, 4000));
		} catch (Exception e) {
			logger.warn("Unable to load full image {} - {}", path, e.getMessage());
		}

		imagePanel.add(largeImage);
		imageFrame.add(scroll);
		imageFrame.pack();
		imageFrame.setVisible(true);
	}

}
