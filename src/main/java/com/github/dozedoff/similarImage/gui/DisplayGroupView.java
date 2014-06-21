/*  Copyright (C) 2013  Nicholas Wright
    
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

import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;

public class DisplayGroupView {
	private JFrame view;
	private JPanel content = new JPanel();
	private JScrollPane scroll = new JScrollPane(content);

	public DisplayGroupView() {
		view = new JFrame();
		view.setSize(500, 500);
		view.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		view.setFocusableWindowState(false);
	}

	public void displayImages(long group, List<View> duplicates) {
		view.remove(scroll);
		view.dispose();
		view.setTitle("" + group);
		content = new JPanel(new MigLayout("wrap 4"));

		for (View entry : duplicates) {
			content.add(entry.getView());
		}

		scroll = new JScrollPane(content);
		view.add(scroll);

		view.validate();
		view.pack();
		view.repaint();
		view.setVisible(true);
	}
}
