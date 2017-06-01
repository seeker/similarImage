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

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.google.common.base.MoreObjects;

/**
 * A image result for the duplicate search.
 */
public class Result {
	private static final Logger LOGGER = LoggerFactory.getLogger(Result.class);

	private final ResultGroup parentGroup;
	private final ImageRecord imageRecord;

	/**
	 * Create a new {@link Result} with the given image and a reference to the parent.
	 * 
	 * @param parentGroup
	 *            of this result
	 * @param imageRecord
	 *            this result represents
	 */
	public Result(ResultGroup parentGroup, ImageRecord imageRecord) {
		this.parentGroup = parentGroup;
		this.imageRecord = imageRecord;
	}

	/**
	 * Get the {@link ImageRecord} this result represents.
	 * 
	 * @return {@link ImageRecord} this result represents
	 */
	public ImageRecord getImageRecord() {
		return imageRecord;
	}

	/**
	 * Remove this result from the parent {@link ResultGroup}.
	 */
	public void remove() {
		LOGGER.debug("Removing result {}", this);
		parentGroup.remove(this);
	}

	/**
	 * A {@link String} representation of this class.
	 * 
	 * @return the class state formatted as a {@link String}
	 */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(Result.class).add("parent", parentGroup.getHash())
				.add("ImageRecord", imageRecord.toString()).toString();
	}

	/**
	 * Generate hash from parent {@link ResultGroup} and {@link ImageRecord}.
	 * 
	 * @return the hash of this class
	 */
	@Override
	public final int hashCode() {
		return Objects.hash(imageRecord);
	}

	/**
	 * Check if the {@link Object} is equal to this instance, based on parent {@link ResultGroup} and
	 * {@link ImageRecord}.
	 * 
	 * @param obj
	 *            instance to compare
	 * @return true if the object is an instance of {@link Result} and has identical fields
	 */
	@Override
	public final boolean equals(Object obj) {
		if (obj instanceof Result) {
			final Result other = (Result) obj;

			return Objects.equals(this.imageRecord, other.imageRecord);
		}

		return false;
	}
}
