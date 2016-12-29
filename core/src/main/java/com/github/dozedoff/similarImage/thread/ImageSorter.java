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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;
import com.github.dozedoff.similarImage.duplicate.DuplicateUtil;
import com.github.dozedoff.similarImage.duplicate.RecordSearch;
import com.github.dozedoff.similarImage.event.GuiGroupEvent;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.eventbus.EventBus;

public class ImageSorter extends Thread {
	private static final Logger LOGGER = LoggerFactory.getLogger(ImageSorter.class);

	private static final String NULL = "null";

	private int hammingDistance;
	private String path;
	private final ImageRepository imageRepository;
	private final EventBus guiEventBus;

	/**
	 * Create a instance that will sort all images within the given hamming distance. Only images starting with the given path will be
	 * considered.
	 * 
	 * @param hammingDistance
	 *            maximum distance to match a hash
	 * @param path
	 *            only consider images starting with this path
	 * @param imageRepository
	 *            access to the image datasource
	 * @param guiEventBus
	 *            {@link EventBus} for gui update events
	 */
	public ImageSorter(int hammingDistance, String path, ImageRepository imageRepository, EventBus guiEventBus) {
		super();
		this.hammingDistance = hammingDistance;
		this.path = path;
		this.imageRepository = imageRepository;
		this.guiEventBus = guiEventBus;
	}

	@Override
	public void run() {
		LOGGER.info("Looking for matching images...");
		Stopwatch sw = Stopwatch.createStarted();

		List<ImageRecord> dBrecords = Collections.emptyList();

		if (path == null) {
			path = NULL;
		}

		try {
			if (NULL.equals(path) || path.isEmpty()) {
				LOGGER.info("Loading all records");
				dBrecords = imageRepository.getAll();
			} else {
				LOGGER.info("Loading records for path {}", path);
				dBrecords = imageRepository.startsWithPath(Paths.get(path));
			}
		} catch (RepositoryException e) {
			LOGGER.warn("Failed to load records - {}", e.getMessage());
		}

		RecordSearch rs = new RecordSearch();
		rs.build(dBrecords);

		Multimap<Long, ImageRecord> results = findAllHashesInRange(rs, dBrecords);

		DuplicateUtil.removeSingleImageGroups(results);
		DuplicateUtil.removeDuplicateSets(results);

		LOGGER.info("Found {} similar images out of {} in {}", results.keySet().size(), dBrecords.size(), sw);
		this.guiEventBus.post(new GuiGroupEvent(results));
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
