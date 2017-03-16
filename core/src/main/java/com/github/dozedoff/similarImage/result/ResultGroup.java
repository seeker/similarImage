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
package com.github.dozedoff.similarImage.result;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.google.common.base.MoreObjects;

/**
 * A set of images that are possible duplicates.
 */
public class ResultGroup {
	private static final Logger LOGGER = LoggerFactory.getLogger(ResultGroup.class);

	private final GroupList parent;
	private final long hash;
	private List<Result> results;

	/**
	 * Create a new {@link ResultGroup} with the given {@link ImageRecord}s.
	 * 
	 * @param parent
	 *            {@link GroupList} that manages this group
	 * @param hash
	 *            this group and the {@link Result} within represent
	 * @param records
	 *            to use for the creation of the {@link Result}s
	 */
	public ResultGroup(GroupList parent, long hash, Collection<ImageRecord> records) {
		this.parent = parent;
		this.hash = hash;
		this.results = new LinkedList<Result>();

		LOGGER.trace("Building group with {} record(s)", records);

		buildResults(records);
	}

	private void buildResults(Collection<ImageRecord> records) {
		for (ImageRecord record : records) {
			results.add(new Result(this, record));
		}
	}

	/**
	 * Get the hash this group represents.
	 * 
	 * @return the hash value
	 */
	public long getHash() {
		return hash;
	}

	/**
	 * Get the {@link Result}s of this group.
	 * 
	 * @return a list of results
	 */
	public List<Result> getResults() {
		return results;
	}

	/**
	 * Remove the result from this group and notify the {@link GroupList} of the removal.
	 * 
	 * @param result
	 *            to remove
	 * @return true if the result was removed
	 */
	public boolean remove(Result result) {
		return remove(result, true);
	}

	/**
	 * Remove the result from this group and notify the {@link GroupList} of the removal if required.
	 * 
	 * @param result
	 *            to remove
	 * @param notifyParent
	 *            if the {@link GroupList} should be notified of the removal
	 * 
	 * @return true if the result was removed
	 */
	public boolean remove(Result result, boolean notifyParent) {
		LOGGER.debug("Removing {} from {}, notify parent: {}", result, this, notifyParent);

		if (notifyParent) {
			parent.remove(result);
		}

		return results.remove(result);
	}

	/**
	 * A {@link String} representation of this class.
	 * 
	 * @return shortened class state formatted as a {@link String}
	 */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(ResultGroup.class).add("parent", parent).add("hash", hash)
				.add("results", results.size()).toString();
	}

	/**
	 * Check if this group has any results.
	 * 
	 * @return true if there are results for this group
	 */
	public boolean hasResults() {
		return !results.isEmpty();
	}
}
