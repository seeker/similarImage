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

import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import uk.co.timwise.wraplayout.WrapLayout;

/**
 * Display {@link View}s in a wrapped layout.
 * 
 * @author Nicholas Wright
 *
 */
public class ResultGroupView {
	private JPanel content = new JPanel();
	private JScrollPane scroll = new JScrollPane(content);

	/**
	 * Setup for displaying duplicate images.
	 */
	public ResultGroupView(ResultGroupPresenter presenter) {

		presenter.setView(this);
	}

	public void displayImages(String title, List<View> duplicates) {
		content = new JPanel(new WrapLayout(WrapLayout.LEFT));

		for (View entry : duplicates) {
			content.add(entry.getView());
		}

		scroll = new JScrollPane(content);
	}
}
