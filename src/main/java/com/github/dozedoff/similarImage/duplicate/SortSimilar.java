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

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.everpeace.search.BKTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.FilterRecord;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Persistence;

public class SortSimilar {
	private static final Logger logger = LoggerFactory.getLogger(SortSimilar.class);
	private final Persistence persistence;

	HashMap<Long, Set<Bucket<Long, ImageRecord>>> sorted = new HashMap<Long, Set<Bucket<Long, ImageRecord>>>();
	CompareHammingDistance compareHamming = new CompareHammingDistance();
	LinkedList<ImageRecord> ignoredImages = new LinkedList<ImageRecord>();
	BKTree<Bucket<Long, ImageRecord>> bkTree = null;
	ArrayList<Bucket<Long, ImageRecord>> buckets;

	public SortSimilar(Persistence persistence) {
		this.persistence = persistence;
	}

	public void buildTree(List<ImageRecord> dbRecords) {
		long uniquePHashes = 0;

		try {
			uniquePHashes = persistence.distinctHashes();
		} catch (SQLException e) {
			logger.warn("Failed to get unigue pHash count: {}", e.getMessage());
		}

		logger.info("Creating bucket list with size {}", uniquePHashes);
		buckets = new ArrayList<Bucket<Long, ImageRecord>>((int) uniquePHashes);

		logger.info("Removing ignored images...");
		dbRecords.removeAll(ignoredImages);

		populateBuckets(buckets, dbRecords);

		logger.info("Building BK-Tree...");
		bkTree = BKTree.build(buckets, compareHamming);
	}

	private void populateBuckets(ArrayList<Bucket<Long, ImageRecord>> buckets, List<ImageRecord> dbRecords) {
		ArrayList<ImageRecord> dbRecords2 = new ArrayList<>(dbRecords);

		Comparator<ImageRecord> compIr = new Comparator<ImageRecord>() {
			@Override
			public int compare(ImageRecord o1, ImageRecord o2) {
				long l1 = o1.getpHash();
				long l2 = o2.getpHash();

				if (l1 < l2) {
					return -1;
				} else if (l1 == l2) {
					return 0;
				} else {
					return 1;
				}
			}
		};

		Comparator<Bucket<Long, ImageRecord>> comp = new BucketComperator();

		logger.info("Sorting dbRecords...");
		Collections.sort(dbRecords2, compIr);

		logger.info("Populating buckets...");
		for (ImageRecord ir : dbRecords2) {
			int index = Collections.binarySearch(buckets, new Bucket<Long, ImageRecord>(ir.getpHash()), comp);

			if (index < 0) {
				Bucket<Long, ImageRecord> b = new Bucket<Long, ImageRecord>(ir.getpHash(), ir);
				buckets.add(Math.abs(index + 1), b);
			} else {
				Bucket<Long, ImageRecord> b = buckets.get(index);
				b.add(ir);
			}
		}

		logger.info("Sorted {} records into {} buckets", dbRecords.size(), buckets.size());
	}

	private void checkTree(List<ImageRecord> dbRecords) {
		if (bkTree == null || buckets == null) {
			logger.info("Building BK-Tree and bucket-list with {} records...", dbRecords.size());
			buildTree(dbRecords);
		}
	}

	public void sortHammingDistance(int hammingDistance, List<ImageRecord> dBrecords) {
		clear();
		checkTree(dBrecords);

		for (ImageRecord ir : dBrecords) {
			long pHash = ir.getpHash();

			if (sorted.containsKey(pHash)) {
				return; // prevent duplicates
			}

			Set<Bucket<Long, ImageRecord>> similar = bkTree.searchWithin(new Bucket<Long, ImageRecord>(pHash), (double) hammingDistance);
			sorted.put(pHash, similar);
		}
	}

	public void sortFilter(int hammingDistance, String reason, List<ImageRecord> dBrecords, List<FilterRecord> filter) {
		clear();
		checkTree(dBrecords);

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

		for (FilterRecord fr : filter) {
			long pHash = fr.getpHash();

			Set<Bucket<Long, ImageRecord>> similar = bkTree.searchWithin(new Bucket<Long, ImageRecord>(pHash), (double) hammingDistance);
			sorted.put(pHash, similar);
		}
	}

	private void sortFilterExact(int hammingDistance, String reason) {
		List<FilterRecord> filters;

		try {
			filters = persistence.getAllFilters(reason);
			Comparator<Bucket<Long, ImageRecord>> comp = new BucketComperator();

			for (FilterRecord filter : filters) {
				long pHash = filter.getpHash();

				int index = Collections.binarySearch(buckets, new Bucket<Long, ImageRecord>(pHash), comp);

				if (index >= 0) {
					Set<Bucket<Long, ImageRecord>> set = new HashSet<>();
					set.add(buckets.get(index));
					sorted.put(pHash, set);
				}
			}
		} catch (SQLException e) {
			logger.warn("Failed to load filter records - {}", e.getMessage());
		}
	}

	public Set<ImageRecord> getGroup(long pHash) {
		Set<ImageRecord> resultSet = new HashSet<ImageRecord>();

		Set<Bucket<Long, ImageRecord>> bucketSet = sorted.get(pHash);

		for (Bucket<Long, ImageRecord> bu : bucketSet) {
			resultSet.addAll(bu.getBucket());
		}

		return resultSet;
	}

	public void sortExactMatch(List<ImageRecord> dbRecords) {
		clear();
		checkTree(dbRecords);

		logger.info("Checking {} buckets for size greater than 1", buckets.size());

		for (Bucket<Long, ImageRecord> b : buckets) {
			if (b.getSize() > 1) {
				Set<Bucket<Long, ImageRecord>> set = new HashSet<>();
				set.add(b);
				sorted.put(b.getId(), set);
			}
		}

		logger.info("Found {} buckets with more than 1 image", sorted.size());
	}

	public LinkedList<Long> getDuplicateGroups() {
		Set<Long> keys = sorted.keySet();
		LinkedList<Long> duplicateGroups = new LinkedList<Long>();

		for (long key : keys) {
			duplicateGroups.add(key);
		}

		mergeIdenticalSets(duplicateGroups);
		return duplicateGroups;
	}

	public void removeSingleImageGroups() {
		int startSize = sorted.size();
		LinkedList<Long> toRemove = new LinkedList<>();

		for (Long id : sorted.keySet()) {
			int total = 0;

			for (Bucket<Long, ImageRecord> b : sorted.get(id)) {
				total += b.getSize();
			}

			if (total <= 1) {
				toRemove.add(id);
			}
		}

		for (Long id : toRemove) {
			sorted.remove(id);
		}

		logger.info("Removed {} result groups with size <= 1", (startSize - sorted.size()));
	}

	private void mergeIdenticalSets(LinkedList<Long> duplicateGroups) {
		logger.info("Merging duplicate sets...");
		TreeMap<BigInteger, Set<Bucket<Long, ImageRecord>>> tree = new TreeMap<>();
		Iterator<Long> ite = duplicateGroups.iterator();
		int potentialMatch = 0, actualMatch = 0;

		while (ite.hasNext()) {
			long group = ite.next();
			Set<Bucket<Long, ImageRecord>> set = sorted.get(group);
			BigInteger hashSum = calcHashSum(set);

			if (tree.containsKey(hashSum)) {
				logger.info("Possible set match found with sum {}", hashSum);
				Set<Bucket<Long, ImageRecord>> treeSet = tree.get(hashSum);
				potentialMatch++;

				if (treeSet.equals(set)) {
					logger.info("Identical sets for hashsum {}", hashSum);
					int oldSize = totalSetSize(treeSet);
					mergeSets(treeSet, set);
					actualMatch++;

					logger.info("Merge sets, old size {}, new {}", oldSize, totalSetSize(treeSet));
				}

				ite.remove();
			} else {
				tree.put(hashSum, set);
			}
		}
		Object logData[] = { tree.size(), potentialMatch, actualMatch };
		logger.info("Final TreeMap size was {}. {} potential matches, of which {} were actual matches", logData);
	}

	private <I, T> int totalSetSize(Set<Bucket<I, T>> set) {
		int size = 0;

		for (Bucket<I, T> b : set) {
			size += b.getSize();
		}

		return size;
	}

	private BigInteger calcHashSum(Set<Bucket<Long, ImageRecord>> set) {
		BigInteger sum = new BigInteger("0");

		for (Bucket<Long, ImageRecord> b : set) {
			sum = sum.add(BigInteger.valueOf(b.getId()));
		}

		return sum;
	}

	private void mergeSets(Set<Bucket<Long, ImageRecord>> resultSet, Set<Bucket<Long, ImageRecord>> toMerge) {
		for (Bucket<Long, ImageRecord> b : resultSet) {
			Bucket<Long, ImageRecord> merge = null;

			for (Bucket<Long, ImageRecord> mergB : toMerge) {
				if (mergB.equals(b)) {
					merge = mergB;
					break;
				}
			}

			if (merge != null) {
				Set<ImageRecord> merged = new HashSet<>();
				List<ImageRecord> originalList = b.getBucket();

				merged.addAll(originalList);
				merged.addAll(merge.getBucket());

				originalList.clear();
				originalList.addAll(merged);
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
	}

	public void ignore(ImageRecord toIgnore) {
		if (!ignoredImages.contains(toIgnore)) {
			ignoredImages.add(toIgnore);
		}
	}

	public void clearIgnored() {
		ignoredImages.clear();
	}

	class BucketComperator implements Comparator<Bucket<Long, ImageRecord>> {
		@Override
		public int compare(Bucket<Long, ImageRecord> o1, Bucket<Long, ImageRecord> o2) {
			long l1 = o1.getId();
			long l2 = o2.getId();

			if (l1 < l2) {
				return -1;
			} else if (l1 == l2) {
				return 0;
			} else {
				return 1;
			}
		}
	}
}
