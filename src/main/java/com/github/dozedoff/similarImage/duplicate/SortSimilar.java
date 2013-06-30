/*  Copyright (C) 2013  Nicholas Wright
    
    This file is part of similarImage - A similar image finder using pHash
    
    mmut is free software: you can redistribute it and/or modify
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
package com.github.dozedoff.similarImage.duplicate;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.everpeace.search.BKTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.FilterRecord;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Persistence;
import com.j256.ormlite.dao.CloseableWrappedIterable;

public class SortSimilar {
	private static final Logger logger = LoggerFactory.getLogger(SortSimilar.class);
	private final Persistence persistence;

	HashMap<Long, Set<ImageRecord>> sorted = new HashMap<Long, Set<ImageRecord>>();
	CompareHammingDistance compareHamming = new CompareHammingDistance();
	LinkedList<ImageRecord> ignoredImages = new LinkedList<ImageRecord>();

	public SortSimilar(Persistence persistence) {
		this.persistence = persistence;
	}

	public void sortHammingDistance(int hammingDistance, List<ImageRecord> dBrecords) {
		clear();
		dBrecords.removeAll(ignoredImages);
		BKTree<ImageRecord> bkTree = BKTree.build(dBrecords, compareHamming);

		for (ImageRecord ir : dBrecords) {
			long pHash = ir.getpHash();

			if (sorted.containsKey(pHash)) {
				return; // prevent duplicates
			}

			Set<ImageRecord> similar = bkTree.searchWithin(ir, (double) hammingDistance);
			sorted.put(pHash, similar);
		}
	}

	public void sortFilter(int hammingDistance, String reason, List<ImageRecord> dBrecords, List<FilterRecord> filter) {
		clear();
		String logReason = reason;

		if (logReason == null || logReason.equals("")) {
			logReason = "*";
		}

		Object[] logData = { dBrecords.size(), filter.size(), logReason, hammingDistance };
		logger.info("Matching {} image records against {} filter records (reason: {}), with a distance of {}", logData);

		if (hammingDistance == 0) {
			sortFilterExact(hammingDistance, reason);
			return;
		}

		dBrecords.removeAll(ignoredImages);
		BKTree<ImageRecord> bkTree = BKTree.build(dBrecords, compareHamming);

		for (FilterRecord fr : filter) {
			long pHash = fr.getpHash();

			if (sorted.containsKey(pHash)) {
				continue; // prevent duplicates
			}

			ImageRecord query = new ImageRecord(null, pHash);

			Set<ImageRecord> similar = bkTree.searchWithin(query, (double) hammingDistance);
			sorted.put(pHash, similar);
		}
	}

	private void sortFilterExact(int hammingDistance, String reason) {
		// TODO add filtering regarding reason
		List<FilterRecord> filters;
		try {
			filters = persistence.getAllFilters(reason);

			for (FilterRecord filter : filters) {
				long pHash = filter.getpHash();

				if (!sorted.containsKey(pHash)) {
					List<ImageRecord> records = persistence.getRecords(pHash);
					sorted.put(pHash, new HashSet<ImageRecord>(records));
				}
			}
		} catch (SQLException e) {
			logger.warn("Failed to load filter records - {}", e.getMessage());
		}
	}

	public Set<ImageRecord> getGroup(long pHash) {
		return sorted.get(pHash);
	}

	public void sortExactMatch(CloseableWrappedIterable<ImageRecord> records) {
		try {
			for (ImageRecord ir : records) {
				long key = ir.getpHash();

				if (ignoredImages.contains(ir)) {
					continue;
				}

				if (sorted.containsKey(key)) {
					addToBucket(key, ir);
				} else {
					createBucket(key, ir);
				}
			}
		} finally {
			try {
				records.close();
			} catch (SQLException e) {
				logger.warn("Failed to close ImageRecord iterator", e);
			}
		}
	}

	public int getNumberOfDuplicateGroups() {
		Collection<Set<ImageRecord>> buckets = sorted.values();
		int duplicateGroups = 0;

		for (Set<ImageRecord> irl : buckets) {
			if (irl.size() > 1) {
				duplicateGroups++;
			}
		}

		return duplicateGroups;
	}

	public LinkedList<Long> getDuplicateGroups() {
		Set<Long> keys = sorted.keySet();
		LinkedList<Long> duplicateGroups = new LinkedList<Long>();

		for (long key : keys) {
			Set<ImageRecord> irs = sorted.get(key);

			if (irs.size() > 1) {
				duplicateGroups.add(key);
			}
		}

		Collections.sort(duplicateGroups);
		removeIdenticalSets(duplicateGroups);
		return duplicateGroups;
	}

	private void removeIdenticalSets(LinkedList<Long> duplicateGroups) {
		LinkedList<Set<ImageRecord>> processedRecords = new LinkedList<Set<ImageRecord>>();

		Iterator<Long> ite = duplicateGroups.iterator();

		while (ite.hasNext()) {
			long group = ite.next();
			Set<ImageRecord> set = sorted.get(group);

			if (processedRecords.contains(set)) {
				ite.remove();
			} else {
				processedRecords.add(set);
			}
		}
	}

	public int getNumberOfGroups() {
		return sorted.size();
	}

	public boolean isEmpty() {
		return sorted.isEmpty();
	}

	public void clear() {
		sorted.clear();
		sorted = new HashMap<Long, Set<ImageRecord>>();
	}

	public void ignore(ImageRecord toIgnore) {
		if (!ignoredImages.contains(toIgnore)) {
			ignoredImages.add(toIgnore);
		}
	}

	public void clearIgnored() {
		ignoredImages.clear();
	}

	private void createBucket(long key, ImageRecord record) {
		Set<ImageRecord> value = new HashSet<ImageRecord>();
		value.add(record);
		sorted.put(key, value);
	}

	private void addToBucket(long key, ImageRecord value) {
		Set<ImageRecord> bucket = sorted.get(key);
		bucket.add(value);
	}
}
