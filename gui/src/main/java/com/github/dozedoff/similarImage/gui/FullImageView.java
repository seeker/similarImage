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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;

/**
 * Displays images in a separate window.
 * 
 * @author Nicholas Wright
 *
 */
public class FullImageView {
	/**
	 * Build a simple frame with scrollbars to display an image.
	 * 
	 * @param largeImage
	 *            the image to display
	 * @param path
	 *            path to the image, used for title
	 */
	public FullImageView(JLabel largeImage, Path path) {
		JPanel imagePanel = new JPanel(new MigLayout());
		JScrollPane scroll = new JScrollPane(imagePanel);

		JFrame imageFrame = new JFrame(path.toString());
		imageFrame.setLayout(new MigLayout());

		imagePanel.add(largeImage);
		imageFrame.add(scroll);

		largeImage.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					imageFrame.dispose();
				}
			}
		});

		imageFrame.pack();
		imageFrame.setVisible(true);
	}
}
