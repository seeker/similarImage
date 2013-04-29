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
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.j256.ormlite.dao.CloseableWrappedIterable;

public class SortSimilar {
	private static final Logger logger = LoggerFactory.getLogger(SortSimilar.class);
	HashMap<Long, LinkedList<ImageRecord>> sorted = new HashMap<Long, LinkedList<ImageRecord>>();
	
	public void sort(int hammingDistance, CloseableWrappedIterable<ImageRecord> records) {
		
		//TODO create buckets for hamming distance
		
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
		Collection<LinkedList<ImageRecord>> buckets = sorted.values();
		int duplicateGroups = 0;

		for(LinkedList<ImageRecord> irl : buckets) {
			if(irl.size() > 1) {
				duplicateGroups++;
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
		sorted = new HashMap<Long, LinkedList<ImageRecord>>();
	}
	
	private void createBucket(long key, ImageRecord record) {
		LinkedList<ImageRecord> value = new LinkedList<ImageRecord>();
		value.add(record);
		sorted.put(key, value);
	}
	
	private void addToBucket(long key, ImageRecord value) {
		LinkedList<ImageRecord> bucket = sorted.get(key);
		bucket.add(value);
	}
}
