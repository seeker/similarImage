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

import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Persistence;
import com.github.dozedoff.similarImage.duplicate.RecordSearch;
import com.github.dozedoff.similarImage.duplicate.SortSimilar;
import com.github.dozedoff.similarImage.gui.SimilarImageView;

public class ImageSorter extends Thread {
	private static final Logger logger = LoggerFactory.getLogger(ImageSorter.class);

	private int hammingDistance = 0;
	private String path;
	private SimilarImageView gui;
	@Deprecated // dont use sort similar, use RecordSearch instead
	private SortSimilar sorter;
	private Persistence persistence;
	private static String lastPath;

	public ImageSorter(int hammingDistance, String path, SimilarImageView gui, SortSimilar sorter, Persistence persistence) {
		super();
		this.hammingDistance = hammingDistance;
		this.path = path;
		this.gui = gui;
		this.sorter = sorter;
		this.persistence = persistence;
	}

	@Override
	public void run() {
		List<ImageRecord> dBrecords = new LinkedList<ImageRecord>();

		gui.setStatus("Sorting...");

		if (path == null) {
			path = "null";
		}

		try {
			if (path.equals("null") || path.isEmpty()) {
				logger.info("Loading all records");
				dBrecords = persistence.getAllRecords();
			} else {
				logger.info("Loading records for path {}", path);
				dBrecords = persistence.filterByPath(Paths.get(path));
			}
		} catch (SQLException e) {
			logger.warn("Failed to load records - {}", e.getMessage());
		}

		RecordSearch rs = new RecordSearch(dBrecords);

		List<Long> groups = Collections.emptyList();

		if (hammingDistance == 0) {
			groups = rs.exactMatch();
		} else {
			// TODO add method in RecordSearch
			sorter.sortHammingDistance(hammingDistance, dBrecords);
		}

		sorter.removeSingleImageGroups();
		gui.setStatus("" + groups.size() + " Groups");

		gui.populateGroupList(groups);
	}
}