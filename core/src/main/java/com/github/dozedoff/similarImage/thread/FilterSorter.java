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
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.FilterRecord;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Persistence;
import com.github.dozedoff.similarImage.duplicate.RecordSearch;
import com.github.dozedoff.similarImage.event.GuiEventBus;
import com.github.dozedoff.similarImage.event.GuiGroupEvent;
import com.github.dozedoff.similarImage.event.GuiStatusEvent;
import com.github.dozedoff.similarImage.util.StringUtil;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.eventbus.EventBus;

/**
 * Match the hashes corresponding the given tag to the records. This allows the user to search for similar images
 * matching a given tag or category.
 * 
 * @author Nicholas Wright
 */
public class FilterSorter extends Thread {
	private final Logger logger = LoggerFactory.getLogger(FilterSorter.class);

	private int hammingDistance;
	private String reason;
	private List<ImageRecord> dBrecords;
	private Persistence persistence;

	/**
	 * Create a class that will search for matches of the given tag within the hamming distance.
	 * 
	 * @param hammingDistance
	 *            maximum distance to consider for a match
	 * @param tag
	 *            to search for
	 * @param persistence
	 *            instance for database access
	 */
	public FilterSorter(int hammingDistance, String tag, Persistence persistence) {
		this.hammingDistance = hammingDistance;
		this.reason = tag;
		this.persistence = persistence;

		dBrecords = Collections.emptyList();
	}

	private Multimap<Long, ImageRecord> getFilterMatches(RecordSearch recordSearch, String sanitizedTag) {
		Multimap<Long, ImageRecord> uniqueGroups = MultimapBuilder.hashKeys().hashSetValues().build();
		List<FilterRecord> matchingFilters = Collections.emptyList();

		try {
			matchingFilters = persistence.getAllFilters(sanitizedTag);
			logger.info("Found {} filters for tag {}", matchingFilters.size(), sanitizedTag);
		} catch (SQLException e) {
			logger.error("Aborted tag search for {}, reason: {}", sanitizedTag, e.getMessage());
		}

		for (FilterRecord filter : matchingFilters) {
			Multimap<Long, ImageRecord> match = recordSearch.distanceMatch(filter.getpHash(), hammingDistance);
			uniqueGroups.putAll(filter.getpHash(), match.values());
		}

		return uniqueGroups;
	}

	@Override
	public void run() {
		EventBus guiEvents = GuiEventBus.getInstance();

		guiEvents.post(new GuiStatusEvent("Sorting..."));
		logger.info("Searching for hashes that match given filter");
		Stopwatch sw = Stopwatch.createStarted();

		RecordSearch rs = new RecordSearch();
		String sanitizedTag = StringUtil.sanitizeTag(reason);
		Multimap<Long, ImageRecord> groups = MultimapBuilder.hashKeys().hashSetValues().build();

		try {
			dBrecords = persistence.getAllRecords();
			rs.build(dBrecords);
			groups = getFilterMatches(rs, sanitizedTag);

			guiEvents.post(new GuiStatusEvent("" + groups.size() + " Groups"));
			logger.info("Found {} groups for tag {} in {}", groups.size(), sanitizedTag, sw.toString());
		} catch (SQLException e) {
			guiEvents.post(new GuiStatusEvent("Database error"));
			logger.warn("Failed to load from database - {}", e.getMessage());
		}

		guiEvents.post(new GuiGroupEvent(groups));
	}
}
