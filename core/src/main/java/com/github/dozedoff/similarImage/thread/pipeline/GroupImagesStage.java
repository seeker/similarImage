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
package com.github.dozedoff.similarImage.thread.pipeline;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.duplicate.RecordSearch;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

/**
 * Stage to group images by hash.
 * 
 * @author Nicholas Wright
 *
 */
public class GroupImagesStage implements Function<Collection<ImageRecord>, Multimap<Long, ImageRecord>> {
	private static final Logger LOGGER = LoggerFactory.getLogger(GroupImagesStage.class);

	private final RecordSearch rs;
	private int hammingDistance;

	/**
	 * Groups images by hashes that are a exact match, i.e. have a hamming distance of 0;
	 */
	public GroupImagesStage() {
		this(0);
	}

	/**
	 * Groups images by hashes that are within the given hamming distance;
	 * 
	 * @param hammingDistance
	 *            group all images within this distance
	 */
	public GroupImagesStage(int hammingDistance) {
		this.hammingDistance = hammingDistance;
		this.rs = new RecordSearch();
	}

	/**
	 * Group images by hash. The group will contain a distinct set of images.
	 * 
	 * @param toGroup
	 *            imagese to group
	 * @return a {@link Multimap} of grouped images
	 */
	@Override
	public Multimap<Long, ImageRecord> apply(Collection<ImageRecord> toGroup) {
		Multimap<Long, ImageRecord> resultMap = MultimapBuilder.hashKeys().hashSetValues().build();
		rs.build(toGroup);

		Stopwatch sw = Stopwatch.createStarted();
		toGroup.forEach(new Consumer<ImageRecord>() {
			@Override
			public void accept(ImageRecord t) {
				resultMap.putAll(t.getpHash(), rs.distanceMatch(t.getpHash(), hammingDistance).values());
			}
		});

		LOGGER.info("Built result map with {} pairs in {}, using hamming distance {}", resultMap.size(), sw,
				hammingDistance);

		return resultMap;
	}
}
