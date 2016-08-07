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

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

public abstract class DuplicateUtil {
	private static final Logger logger = LoggerFactory.getLogger(DuplicateUtil.class);

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

	/**
	 * Remove results of queries with the same set of resulting hashes.
	 * 
	 * @param records
	 *            to scan and merge if needed
	 */
	public static void removeDuplicateSets(Multimap<Long, ImageRecord> records) {
		logger.info("Checking {} groups for duplicates", records.keySet().size());
		Stopwatch sw = Stopwatch.createStarted();
		Set<Collection<ImageRecord>> uniqueRecords = new HashSet<Collection<ImageRecord>>(records.keySet().size());

		Iterator<Collection<ImageRecord>> recordIter = records.asMap().values().iterator();
		long removedGroups = 0;

		while (recordIter.hasNext()) {
			Collection<ImageRecord> next = recordIter.next();

			if (!uniqueRecords.add(next)) {
				recordIter.remove();
				removedGroups++;
			}
		}

		logger.info("Checked groups in {}, removed {} identical groups", sw, removedGroups);
	}

	protected static final BigInteger hashSum(Collection<Long> hashes) {
		BigInteger hashSum = BigInteger.ZERO;

		for (Long hash : hashes) {
			hashSum = hashSum.add(BigInteger.valueOf(hash));
		}

		return hashSum;
	}
}
