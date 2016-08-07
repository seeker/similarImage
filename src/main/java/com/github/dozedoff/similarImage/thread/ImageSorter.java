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
package com.github.dozedoff.similarImage.thread;

import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Persistence;
import com.github.dozedoff.similarImage.duplicate.DuplicateUtil;
import com.github.dozedoff.similarImage.duplicate.RecordSearch;
import com.github.dozedoff.similarImage.gui.SimilarImageController;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

public class ImageSorter extends Thread {
	private static final Logger logger = LoggerFactory.getLogger(ImageSorter.class);

	private static final String NULL = "null";

	private int hammingDistance;
	private String path;
	private SimilarImageController controller;
	private Persistence persistence;

	public ImageSorter(int hammingDistance, String path, SimilarImageController controller, Persistence persistence) {
		super();
		this.hammingDistance = hammingDistance;
		this.path = path;
		this.controller = controller;
		this.persistence = persistence;
	}

	@Override
	public void run() {
		logger.info("Looking for matching images...");
		Stopwatch sw = Stopwatch.createStarted();

		List<ImageRecord> dBrecords = Collections.emptyList();

		if (path == null) {
			path = NULL;
		}

		try {
			if (NULL.equals(path) || path.isEmpty()) {
				logger.info("Loading all records");
				dBrecords = persistence.getAllRecords();
			} else {
				logger.info("Loading records for path {}", path);
				dBrecords = persistence.filterByPath(Paths.get(path));
			}
		} catch (SQLException e) {
			logger.warn("Failed to load records - {}", e.getMessage());
		}

		RecordSearch rs = new RecordSearch();
		rs.build(dBrecords);

		Multimap<Long, ImageRecord> results = findAllHashesInRange(rs, dBrecords);

		DuplicateUtil.removeSingleImageGroups(results);
		DuplicateUtil.removeDuplicateSets(results);

		logger.info("Found {} similar images out of {} in {}", results.keySet().size(), dBrecords.size(), sw);
		controller.setResults(results);
	}

	private Multimap<Long, ImageRecord> findAllHashesInRange(RecordSearch rs, Collection<ImageRecord> records) {
		Multimap<Long, ImageRecord> results = MultimapBuilder.hashKeys().hashSetValues().build();

		for (ImageRecord record : records) {
			long key = record.getpHash();
			results.putAll(key, rs.distanceMatch(key, hammingDistance).values());
		}

		return results;
	}
}
