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
package com.github.dozedoff.similarImage.thread;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.FilterRecord;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Tag;
import com.github.dozedoff.similarImage.db.repository.FilterRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;
import com.github.dozedoff.similarImage.duplicate.RecordSearch;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;

/**
 * Filter the result set so it only contains hashes from tagged images that are in range.
 * 
 * @author Nicholas Wright
 *
 */
public class TagFilter {
	private static final Logger LOGGER = LoggerFactory.getLogger(TagFilter.class);

	private final FilterRepository filterRepository;

	/**
	 * Create a new filter instance with the filter repository to find hashes for tags.
	 * 
	 * @param filterRepository
	 *            the repository to use for hash queries
	 */
	public TagFilter(FilterRepository filterRepository) {
		this.filterRepository = filterRepository;
	}

	/**
	 * Query the the records for matches against tagged hashes.
	 * 
	 * @param recordSearch
	 *            the images to filter
	 * @param tagToMatch
	 *            hashes that have this tag will be used for filtering
	 * @param hammingDistance
	 *            all hashes within the distance of the query hash will be considered a match
	 * @return a {@link Multimap} containing the search results
	 */
	public Multimap<Long, ImageRecord> getFilterMatches(RecordSearch recordSearch, Tag tagToMatch,
			int hammingDistance) {
		Multimap<Long, ImageRecord> uniqueGroups = MultimapBuilder.hashKeys().hashSetValues().build();
		List<FilterRecord> matchingFilters = Collections.emptyList();

		try {
			matchingFilters = FilterRecord.getTags(filterRepository, tagToMatch);
			LOGGER.info("Found {} filters for tag {}", matchingFilters.size(), tagToMatch.getTag());
		} catch (RepositoryException e) {
			LOGGER.error("Failed to query hashes for tag {}, reason: {}, cause: {}", tagToMatch.getTag(), e.toString(),
					e.getCause());
		}

		Multimap<Long, ImageRecord> parallelGroups = Multimaps.synchronizedMultimap(uniqueGroups);

		matchingFilters.parallelStream().forEach(filter -> {
			Multimap<Long, ImageRecord> match = recordSearch.distanceMatch(filter.getpHash(), hammingDistance);
			parallelGroups.putAll(filter.getpHash(), match.values());
		});

		return uniqueGroups;
	}
}
