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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.everpeace.search.BKTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Persistence;
import com.j256.ormlite.dao.CloseableWrappedIterable;

public class SortSimilar {
	private final int SORT_WORKERS =  4;
	private final int INITIAL_CAPACITY = 16;
	private final float LOAD_FACTOR = 0.75f;
	private static final Logger logger = LoggerFactory.getLogger(SortSimilar.class);
	ConcurrentHashMap<Long, Set<ImageRecord>> sorted = new ConcurrentHashMap<Long, Set<ImageRecord>>(INITIAL_CAPACITY, LOAD_FACTOR, SORT_WORKERS+1);
	CompareHammingDistance compareHamming = new CompareHammingDistance();
	
	/**
	 * Use {@link #sortHammingDistance(int, CloseableWrappedIterable)} instead.
	 * @param hammingDistance
	 * @param records
	 */
	@Deprecated
	public void sort(int hammingDistance, CloseableWrappedIterable<ImageRecord> records) {
		if(hammingDistance == 0) {
			sortExactMatch(records);
		}else{
			sortHammingDistance(hammingDistance);
		}
	}
	
	public void sortHammingDistance(int hammingDistance) {
		
		try {
			List<ImageRecord> dBrecords = Persistence.getInstance().getAllRecords();
			ArrayList<ImageRecord> records = new ArrayList<ImageRecord>(dBrecords);
			LinkedBlockingQueue<ImageRecord> workQueue = new LinkedBlockingQueue<ImageRecord>(dBrecords);
			
			Thread workers[] = new Thread[SORT_WORKERS];
			
			for(int i = 0; i < SORT_WORKERS; i++) {
				Thread t = new SortWorker(i, hammingDistance, records, workQueue);
				workers[i] = t;
				t.start();
			}
			
			for(int i = 0; i < SORT_WORKERS; i++) {
				try {
					workers[i].join();
				} catch (InterruptedException e) {
					logger.info("Interrupted while waiting for {}", workers[i].getName());
				}
			}
		} catch (SQLException e) {
			logger.error("Failed to load records - {}", e.getMessage());
		}
		
	}
	
	private void createSimilar(int hammingDistance, ImageRecord root, List<ImageRecord> records) {
		long pHash = root.getpHash();
		
		if(sorted.containsKey(pHash)) {
			return;		// prevent duplicates
		}
		
		BKTree<ImageRecord> bkTree = new BKTree<ImageRecord>(compareHamming, root);
		
		for (ImageRecord ir : records) {
			bkTree.insert(ir);
		}
		
		Set<ImageRecord> similar = bkTree.searchWithin(root, (double)hammingDistance);
		sorted.put(pHash, similar);
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
		
		Collections.sort(duplicateGroups);
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
		sorted =  new ConcurrentHashMap<Long, Set<ImageRecord>>(INITIAL_CAPACITY, LOAD_FACTOR, SORT_WORKERS+1);
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
	
	class SortWorker extends Thread {
		private final static int BATCH_SIZE = 20;
		
		private final LinkedList<ImageRecord> work = new LinkedList<ImageRecord>();
		private final LinkedBlockingQueue<ImageRecord> workQueue;
		private final ArrayList<ImageRecord> records;
		private final int hammingDistance;
		
		public SortWorker(int workerNumber, int hammingDistance, ArrayList<ImageRecord> records, LinkedBlockingQueue<ImageRecord> workQueue) {
			super();
			this.setName("Sort worker " + workerNumber);
			this.hammingDistance = hammingDistance;
			this.workQueue = workQueue;
			this.records = records;
		}
		
		@Override
		public void run() {
			while(! workQueue.isEmpty()) {
				workQueue.drainTo(work, BATCH_SIZE);
				
				for(ImageRecord rec : work) {
					createSimilar(hammingDistance, rec, records);
				}
			}
		}
	}
}
