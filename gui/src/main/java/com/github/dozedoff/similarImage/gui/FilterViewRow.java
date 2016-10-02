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

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;

import com.github.dozedoff.similarImage.db.FilterRecord;
import com.github.dozedoff.similarImage.db.Tag;
import com.github.dozedoff.similarImage.db.Thumbnail;

import net.miginfocom.swing.MigLayout;

/**
 * Display one {@link FilterRecord}.
 * 
 * @author Nicholas Wright
 *
 */
public class FilterViewRow extends JComponent {
	private static final long serialVersionUID = -7521419392503926391L;

	private final FilterRecord filter;
	private JCheckBox selected;

	/**
	 * Create a new {@link FilterViewRow} to display the {@link FilterRecord}s information.
	 * 
	 * @param filter
	 *            to display
	 */
	public FilterViewRow(FilterRecord filter) {
		this.filter = filter;

		setUpGui();
	}

	private void setUpGui() {
		setLayout(new MigLayout());
		createSelectionCheckbox();
		createThumbnail();
		createTagLabel(filter.getTag());
		createHashLabel();
	}

	private void createSelectionCheckbox() {
		selected = new JCheckBox();
		add(selected);
	}

	private void createTagLabel(Tag tag) {
		add(new JLabel(tag.toString()));
	}

	private void createThumbnail() {
		JLabel thumb = null;

		if (filter.hasThumbnail()) {
			Thumbnail thumbnail = filter.getThumbnail();
			
			thumb = new JLabel(new ImageIcon(thumbnail.getImageData()));
		}else {
			thumb = new JLabel("No Thumb");
		}
		
		add(thumb, thumbnailSize());
	}

	private String thumbnailSize() {
		StringBuilder sb = new StringBuilder();
		sb.append("width ");
		sb.append(Thumbnail.THUMBNAIL_SIZE);
		sb.append("!");

		return sb.toString();
	}

	private void createHashLabel() {
		JLabel hash = new JLabel(String.valueOf(filter.getpHash()));
		add(hash);
	}

	/**
	 * Check if this {@link FilterViewRow} has been selected.
	 * 
	 * @return true if selected
	 */
	public boolean isSelected() {
		return selected.isSelected();
	}

	/**
	 * Get the {@link FilterRecord} represented by this view.
	 * 
	 * @return {@link FilterRecord} of this component
	 */
	public FilterRecord getFilter() {
		return filter;
	}
}
