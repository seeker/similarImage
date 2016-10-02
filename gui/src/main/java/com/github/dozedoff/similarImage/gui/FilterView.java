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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.FilterRecord;
import com.github.dozedoff.similarImage.db.repository.FilterRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;

import net.miginfocom.swing.MigLayout;

/**
 * Display all stored {@link FilterRecord}s as {@link FilterViewRow}s.
 * 
 * @author Nicholas Wright
 *
 */
public class FilterView extends JFrame {
	private static final long serialVersionUID = 6353120196258447248L;
	private static final Logger LOGGER = LoggerFactory.getLogger(FilterView.class);

	private static final int VIEW_SIZE = 500;
	private static final String VIEW_SIZE_CONFIGURATION = "w %d!, h %d!";

	private final FilterRepository filterRepository;

	private List<FilterViewRow> rows = new LinkedList<FilterViewRow>();
	private JPanel actions;
	private JPanel rowView;

	public FilterView(FilterRepository filterRepository) {
		this.filterRepository = filterRepository;

		setLayout(new MigLayout());
		rowView = new JPanel(new MigLayout("wrap 1"));
		add(new JScrollPane(rowView), String.format(VIEW_SIZE_CONFIGURATION, VIEW_SIZE, VIEW_SIZE));

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		try {
			loadFilters();
		} catch (RepositoryException e) {
			LOGGER.error("Failed to load Filters: {}", e.toString());
		}

		setTitle("Filters");

		setUpActions();

		pack();
		setVisible(true);
	}

	private void setUpActions() {
		actions = new JPanel(new MigLayout());
		JButton deleteFilter = new JButton("Delete");
		deleteFilter.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				removeSelectedRows();
			}
		});

		actions.add(deleteFilter);
		add(actions, "dock south");
	}

	private void removeSelectedRows() {
		LOGGER.debug("Removing selected filters...");
		int removed = 0;
		Iterator<FilterViewRow> iter = rows.iterator();

		while (iter.hasNext()) {
			FilterViewRow row = iter.next();

			if (row.isSelected()) {
				try {
					filterRepository.remove(row.getFilter());
					rowView.remove(row);
					iter.remove();
					removed++;
				} catch (RepositoryException e) {
					LOGGER.warn("Failed to remove filter for {}", row.getFilter().getpHash());
				}
			}
		}

		pack();
		LOGGER.info("Removed {} filters", removed);
	}

	private void loadFilters() throws RepositoryException {
		for (FilterRecord filter : filterRepository.getAll()) {
			FilterViewRow row = new FilterViewRow(filter);
			rowView.add(row);
			rows.add(row);
		}
	}
}
