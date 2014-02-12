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
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.ImageRecord;

public abstract class DuplicateUtil {
	private static final Logger logger = LoggerFactory.getLogger(DuplicateUtil.class);

	private static final ImageRecordComperator irc = new ImageRecordComperator();
	private static final BucketComperator bucketComperator = new BucketComperator();

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
}
