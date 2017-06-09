/*  Copyright (C) 2017  Nicholas Wright
    
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

import java.awt.BorderLayout;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.github.dozedoff.similarImage.db.FilterRecord;

/**
 * Decorator that displays thumbnails above the passed view.
 * 
 * @author Nicholas Wright
 *
 */
public class ThumbnailDecarator implements View {
	private final View toDecorate;
	private final List<FilterRecord> thumbnails;

	public ThumbnailDecarator(View toDecorate, List<FilterRecord> filters) {
		this.toDecorate = toDecorate;
		this.thumbnails = filters;
	}

	@Override
	public JComponent getView() {
		JPanel thumbPanel = new JPanel();
		
		thumbnails.stream().filter(new Predicate<FilterRecord>() {
			@Override
			public boolean test(FilterRecord t) {
				return t.hasThumbnail();
			}
		}).forEach(new Consumer<FilterRecord>() {
			@Override
			public void accept(FilterRecord t) {
				thumbPanel.add(new JLabel(new ImageIcon(t.getThumbnail().getImageData())));
			}
		});

		JPanel decoratedPanel = new JPanel();
		decoratedPanel.setLayout(new BorderLayout());
		decoratedPanel.add(toDecorate.getView(), BorderLayout.CENTER);
		decoratedPanel.add(thumbPanel, BorderLayout.NORTH);
		
		return decoratedPanel;
	}

}
