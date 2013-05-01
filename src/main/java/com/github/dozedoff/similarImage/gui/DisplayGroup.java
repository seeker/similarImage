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

import java.util.LinkedList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

public class DisplayGroup extends JFrame {
	private static final long serialVersionUID = 1L;
	JPanel imagePannel;
	List<JLabel> currentImages = new LinkedList<JLabel>(); 
	
	public DisplayGroup() {
		this.setSize(500,500);
		this.setDefaultCloseOperation(HIDE_ON_CLOSE);
		this.setLayout(new MigLayout("wrap 4"));
		this.setFocusableWindowState(false);
	}

	public void displayImages(long group, List<JLabel> images) {
		removeOldImages();
		this.dispose();
		this.setTitle("" + group);
		
		for(JLabel image : images) {
			this.add(image);
		}
		
		currentImages.addAll(images);
		
		this.validate();
		this.pack();
		this.setVisible(true);
	}
	
	private void removeOldImages() {
		for(JLabel image : currentImages) {
			this.remove(image);
		}
		
		currentImages.clear();
	}
}
