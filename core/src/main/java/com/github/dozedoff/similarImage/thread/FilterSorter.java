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

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.FilterRecord;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Tag;
import com.github.dozedoff.similarImage.db.repository.FilterRepository;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;
import com.github.dozedoff.similarImage.duplicate.RecordSearch;
import com.github.dozedoff.similarImage.event.GuiEventBus;
import com.github.dozedoff.similarImage.event.GuiGroupEvent;
import com.github.dozedoff.similarImage.event.GuiStatusEvent;
import com.github.dozedoff.similarImage.util.StringUtil;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.google.common.eventbus.EventBus;

/**
 * Match the hashes corresponding the given tag to the records. This allows the user to search for similar images matching a given tag or
 * category.
 * 
 * @author Nicholas Wright
 */
public class FilterSorter extends Thread {
	private final Logger logger = LoggerFactory.getLogger(FilterSorter.class);

	private int hammingDistance;
	private Tag tag;
	private List<ImageRecord> dBrecords;
	private final FilterRepository filterRepository;
	private final ImageRepository imageRepository;
	private Path scope;

	/**
	 * Create a class that will search for matches of the given tag within the hamming distance, only records starting
	 * with the given path are considered.
	 * 
	 * @param hammingDistance
	 *            maximum distance to consider for a match
	 * @param tag
	 *            to search for
	 * @param filterRepository
	 *            filter datasource access
	 * @param imageRepository
	 *            image datasource access
	 * @param scope
	 *            limit results to this path
	 */
	public FilterSorter(int hammingDistance, Tag tag, FilterRepository filterRepository,
			ImageRepository imageRepository, Path scope) {
		this.hammingDistance = hammingDistance;

		if (tag != null) {
			this.tag = tag;
		} else {
			logger.warn("Tag was null, will use {} tag to match all instead", StringUtil.MATCH_ALL_TAGS);
			this.tag = new Tag(StringUtil.MATCH_ALL_TAGS);
		}
		this.filterRepository = filterRepository;
		this.imageRepository = imageRepository;
		this.scope = scope;

		dBrecords = Collections.emptyList();
	}

	/**
	 * Create a class that will search for matches of the given tag within the hamming distance, all records are
	 * considered.
	 * 
	 * @param hammingDistance
	 *            maximum distance to consider for a match
	 * @param tag
	 *            to search for
	 * @param filterRepository
	 *            filter datasource access
	 * @param imageRepository
	 *            image datasource access
	 */
	public FilterSorter(int hammingDistance, Tag tag, FilterRepository filterRepository,
			ImageRepository imageRepository) {
		this(hammingDistance, tag, filterRepository,  imageRepository, null);
	}

	private Multimap<Long, ImageRecord> getFilterMatches(RecordSearch recordSearch, Tag tag) {
		Multimap<Long, ImageRecord> uniqueGroups = MultimapBuilder.hashKeys().hashSetValues().build();
		List<FilterRecord> matchingFilters = Collections.emptyList();

		try {
			matchingFilters = FilterRecord.getTags(filterRepository, tag);
			logger.info("Found {} filters for tag {}", matchingFilters.size(), tag.getTag());
		} catch (RepositoryException e) {
			logger.error("Aborted tag search for {}, reason: {}", tag.getTag(), e.getMessage());
		}

		Multimap<Long, ImageRecord> parallelGroups = Multimaps.synchronizedMultimap(uniqueGroups);

		matchingFilters.parallelStream().forEach(filter -> {
			Multimap<Long, ImageRecord> match = recordSearch.distanceMatch(filter.getpHash(), hammingDistance);
			parallelGroups.putAll(filter.getpHash(), match.values());
		});

		return uniqueGroups;
	}

	@Override
	public void run() {
		EventBus guiEvents = GuiEventBus.getInstance();

		guiEvents.post(new GuiStatusEvent("Sorting..."));
		logger.info("Searching for hashes that match given filter");
		Stopwatch sw = Stopwatch.createStarted();

		RecordSearch rs = new RecordSearch();
		Multimap<Long, ImageRecord> groups = MultimapBuilder.hashKeys().hashSetValues().build();

		try {
			if (scope == null) {
				dBrecords = imageRepository.getAll();
			} else {
				dBrecords = imageRepository.startsWithPath(scope);
			}

			rs.build(dBrecords);
			groups = getFilterMatches(rs, tag);

			guiEvents.post(new GuiStatusEvent("" + groups.size() + " Groups"));
			logger.info("Found {} groups for tag {} in {}", groups.size(), tag.getTag(), sw.toString());
		} catch (RepositoryException e) {
			guiEvents.post(new GuiStatusEvent("Database error"));
			logger.warn("Failed to load from database - {}", e.getMessage());
		}

		guiEvents.post(new GuiGroupEvent(groups));
	}
}
