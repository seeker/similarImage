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
import java.util.function.Function;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Tag;
import com.github.dozedoff.similarImage.db.repository.FilterRepository;
import com.github.dozedoff.similarImage.duplicate.RecordSearch;
import com.github.dozedoff.similarImage.thread.TagFilter;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

/**
 * Group images based on hashes assigned to a tag.
 * 
 * @author Nicholas Wright
 *
 */
public class GroupByTagStage implements Function<Collection<ImageRecord>, Multimap<Long, ImageRecord>> {
	private final Tag tag;
	private final int hammingDistance;
	private final FilterRepository filterRepository;
	private final RecordSearch rs;

	/**
	 * Create a grouper that will only group images that match tagged hashs.
	 * 
	 * @param filterRepository
	 *            to access the filter datasource
	 * @param tag
	 *            to use for hash query
	 * @param hammingDistance
	 *            in which hashes are considered a match
	 */
	public GroupByTagStage(FilterRepository filterRepository, Tag tag, int hammingDistance) {
		this.filterRepository = filterRepository;
		this.tag = tag;
		this.hammingDistance = hammingDistance;
		this.rs = new RecordSearch();
	}

	/**
	 * Create a grouper that will only group images that match a tagged hash. Will only group exact hash matches.
	 * 
	 * @param filterRepository
	 *            to access the filter datasource
	 * @param tag
	 *            to use for hash query
	 */
	public GroupByTagStage(FilterRepository filterRepository, Tag tag) {
		this(filterRepository, tag, 0);
	}

	/**
	 * Group images based on hashes from tags.
	 * 
	 * @param t
	 *            images to group
	 * 
	 * @return a {@link Multimap} of grouped images
	 */
	@Override
	public Multimap<Long, ImageRecord> apply(Collection<ImageRecord> t) {
		Multimap<Long, ImageRecord> result = MultimapBuilder.hashKeys().hashSetValues().build();
		
		rs.build(t);
		
		TagFilter tagFilter = new TagFilter(filterRepository);
		result = tagFilter.getFilterMatches(rs, tag, hammingDistance);

		return result;
	}
}
