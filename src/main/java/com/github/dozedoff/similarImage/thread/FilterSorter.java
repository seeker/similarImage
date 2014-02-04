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
package com.github.dozedoff.similarImage.thread;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.FilterRecord;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Persistence;
import com.github.dozedoff.similarImage.duplicate.SortSimilar;
import com.github.dozedoff.similarImage.gui.SimilarImageGUI;

public class FilterSorter extends Thread {
	private final Logger logger = LoggerFactory.getLogger(FilterSorter.class);

	private int hammingDistance = 0;
	private String reason;
	private List<ImageRecord> dBrecords = new LinkedList<ImageRecord>();
	private List<FilterRecord> filterRecords = new LinkedList<FilterRecord>();
	private SimilarImageGUI gui;
	private Persistence persistence;
	private SortSimilar sorter;

	public FilterSorter(int hammingDistance, String reason, SimilarImageGUI gui, SortSimilar sorter, Persistence persistence) {
		this.hammingDistance = hammingDistance;
		this.reason = reason;
		this.gui = gui;
		this.persistence = persistence;
		this.sorter = sorter;
	}

	@Override
	public void run() {
		gui.setStatus("Sorting...");

		try {
			dBrecords = persistence.getAllRecords();

			if (reason == null || reason.isEmpty() || reason.equals("*")) {
				filterRecords = persistence.getAllFilters();
			} else {
				filterRecords = persistence.getAllFilters(reason);
			}
		} catch (SQLException e) {
			logger.warn("Failed to load from database - {}", e.getMessage());
		}

		sorter.sortFilter(hammingDistance, reason, dBrecords, filterRecords);
		gui.setStatus("" + sorter.getNumberOfGroups() + " Groups");
		List<Long> groups = sorter.getDuplicateGroups();
		gui.populateGroupList(groups);
	}
}