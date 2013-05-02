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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.everpeace.search.BKTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Persistence;
import com.j256.ormlite.dao.CloseableWrappedIterable;

public class SortSimilar {
	private static final Logger logger = LoggerFactory.getLogger(SortSimilar.class);
	HashMap<Long, Set<ImageRecord>> sorted = new HashMap<Long, Set<ImageRecord>>();
	CompareHammingDistance compareHamming = new CompareHammingDistance();
	LinkedList<Set<ImageRecord>> processedSets;
	
	/**
	 * Use {@link #sortHammingDistance(int, CloseableWrappedIterable)} instead.
	 * @param hammingDistance
	 * @param records
	 */
	@Deprecated
	public void sort(int hammingDistance, CloseableWrappedIterable<ImageRecord> records) {
		sortHammingDistance(hammingDistance, records);
	}
	
	public void sortHammingDistance(int hammingDistance, CloseableWrappedIterable<ImageRecord> records) {
		if(hammingDistance == 0) {
			sortExactMatch(records);
		}
		
		processedSets = new LinkedList<Set<ImageRecord>>();
		
		try {
			for (ImageRecord ir : records) {
				createSimilar(hammingDistance, ir);
			}
		} finally {
			try {
				records.close();
			} catch (SQLException e) {
				logger.warn("Failed to close ImageRecord iterator", e);
			}
		}
		
		processedSets = null;
	}
	
	private void createSimilar(int hammingDistance, ImageRecord root) {
		long pHash = root.getpHash();
		
		if(sorted.containsKey(pHash)) {
			return;		// prevent duplicates
		}
		
		createBucket(pHash, root);
		BKTree<ImageRecord> bkTree = new BKTree<ImageRecord>(compareHamming, root);
		CloseableWrappedIterable<ImageRecord> records = Persistence.getInstance().getImageRecordIterator();
		
		for (ImageRecord ir : records) {
			bkTree.insert(ir);
		}
		
		Set<ImageRecord> similar = bkTree.searchWithin(root, (double)hammingDistance);
		
		//TODO use arraylist & binary search for this
		if(! processedSets.contains(similar)) {
			createBucket(pHash, similar);
			processedSets.add(similar);
		}
	}
	
	public Set<ImageRecord> getGroup(long pHash) {
		return sorted.get(pHash);
	}
	
	public void sortExactMatch(CloseableWrappedIterable<ImageRecord> records) {
		try {
			for (ImageRecord ir : records) {
				long key = ir.getpHash();
				
				if(sorted.containsKey(key)) {
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

		for(Set<ImageRecord> irl : buckets) {
			if(irl.size() > 1) {
				duplicateGroups++;
			}
		}
		
		return duplicateGroups;
	}
	
	public LinkedList<Long> getDuplicateGroups() {
		Collection<Set<ImageRecord>> buckets = sorted.values();
		LinkedList<Long> duplicateGroups = new LinkedList<Long>();

		for(Set<ImageRecord> irl : buckets) {
			if(irl.size() > 1) {
				ImageRecord entry = irl.iterator().next(); 
				long groupNo = entry.getpHash();
				duplicateGroups.add(groupNo);
			}
		}
		
		return duplicateGroups;
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
	
	private void createBucket(long key, ImageRecord record) {
		Set<ImageRecord> value = new HashSet<ImageRecord>();
		value.add(record);
		sorted.put(key, value);
	}
	
	private void createBucket(long key, Set<ImageRecord> records) {
		sorted.put(key, records);
	}
	
	private void addToBucket(long key, ImageRecord value) {
		Set<ImageRecord> bucket = sorted.get(key);
		bucket.add(value);
	}
}
