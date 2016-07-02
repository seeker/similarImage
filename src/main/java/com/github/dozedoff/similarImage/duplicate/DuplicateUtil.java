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
package com.github.dozedoff.similarImage.duplicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

public abstract class DuplicateUtil {
	private static final Logger logger = LoggerFactory.getLogger(DuplicateUtil.class);

	private static final ImageRecordComperator irc = new ImageRecordComperator();
	private static final BucketComperator bucketComperator = new BucketComperator();

	/**
	 * Use {@link DuplicateUtil#groupByHash(Collection)} instead.
	 */
	@Deprecated
	public static ArrayList<Bucket<Long, ImageRecord>> sortIntoBuckets(List<ImageRecord> dbRecords) {
		ArrayList<ImageRecord> dbRecords2 = new ArrayList<>(dbRecords);
		ArrayList<Bucket<Long, ImageRecord>> buckets = new ArrayList<>(dbRecords.size());

		logger.info("Sorting records...");
		Collections.sort(dbRecords2, irc);

		logger.info("Populating buckets...");
		for (ImageRecord ir : dbRecords2) {
			int index = Collections.binarySearch(buckets, new Bucket<Long, ImageRecord>(ir.getpHash()), bucketComperator);

			if (index < 0) {
				Bucket<Long, ImageRecord> b = new Bucket<Long, ImageRecord>(ir.getpHash(), ir);
				buckets.add(Math.abs(index + 1), b);
			} else {
				Bucket<Long, ImageRecord> b = buckets.get(index);
				b.add(ir);
			}
		}

		logger.info("Sorted {} records into {} buckets", dbRecords.size(), buckets.size());

		buckets.trimToSize();

		return buckets;
	}

	/**
	 * Group records by hash using a one to many map.
	 * 
	 * @param dbRecords
	 *            records to sort.
	 * @return a one to many map with the hash vales as the key.
	 */
	public static Multimap<Long, ImageRecord> groupByHash(Collection<ImageRecord> dbRecords) {
		Multimap<Long, ImageRecord> groupedByHash = MultimapBuilder.hashKeys().hashSetValues().build();

		logger.info("Grouping records by hash...");

		for (ImageRecord ir : dbRecords) {
			groupedByHash.put(ir.getpHash(), ir);
		}

		logger.info("{} records, in {} groups", dbRecords.size(), groupedByHash.keySet().size());

		return groupedByHash;
	}
	
	/**
	 * Create a map that only contains groups with more than one image.
	 * 
	 * @return new map containing only multi-image groups.
	 */
	public static void removeSingleImageGroups(Multimap<Long, ImageRecord> sourceGroups) {
		Iterator<Entry<Long, Collection<ImageRecord>>> iter = sourceGroups.asMap().entrySet().iterator();
		// TODO add logging
		while (iter.hasNext() ) {
			Entry<Long, Collection<ImageRecord>> group = iter.next();
			if (group.getValue().size() <= 1) {
				iter.remove();
			}
		}
	}

	/**
	 * Converts a multimap to buckets. This serves as an adapter to the legacy
	 * data structure.
	 * 
	 * @param multimap
	 *            to convert
	 * @return Bucket representation of the mutimap
	 */
	public static Set<Bucket<Long, ImageRecord>> multimapToBucketSet(Multimap<Long, ImageRecord> multimap) {
		Set<Bucket<Long, ImageRecord>> buckets = new HashSet<>();

		for (Long key : multimap.keySet()) {
			buckets.add(new Bucket<Long, ImageRecord>(key, multimap.get(key)));
		}

		return buckets;
	}

	/**
	 * Converts a Bucket set to a multimap. This serves as an adapter to the
	 * legacy data structure.
	 * 
	 * @param buckets
	 *            to convert
	 * @return Multimap representation of the Bucket set
	 */
	public static Multimap<Long, ImageRecord> bucketSetToMultimap(Set<Bucket<Long, ImageRecord>> buckets) {
		Multimap<Long, ImageRecord> multimap = MultimapBuilder.hashKeys().hashSetValues().build();

		for (Bucket<Long, ImageRecord> bucket : buckets) {
			multimap.putAll(bucket.getId(), bucket.getBucket());
		}

		return multimap;
	}
}
