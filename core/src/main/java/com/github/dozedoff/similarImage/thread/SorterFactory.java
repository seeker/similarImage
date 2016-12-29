/*  Copyright (C) 2016  Nicholas Wright
    
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
package com.github.dozedoff.similarImage.thread;

import java.nio.file.Path;

import com.github.dozedoff.similarImage.db.Tag;
import com.github.dozedoff.similarImage.db.repository.FilterRepository;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.db.repository.TagRepository;
import com.github.dozedoff.similarImage.event.GuiEventBus;

/**
 * Factory class for creating sorter tasks.
 * 
 * @author Nicholas Wright
 *
 */
public class SorterFactory {
	private final ImageRepository imageRepository;
	private final FilterRepository filterRepository;
	private final TagRepository tagRepository;

	/**
	 * Create a sorter factory using repositories
	 * 
	 * @param imageRepository
	 *            access to the image datasource
	 * @param filterRepository
	 *            access to the filter datasource
	 * @param tagRepository
	 *            access to the tag datasource
	 */
	public SorterFactory(ImageRepository imageRepository, FilterRepository filterRepository,
			TagRepository tagRepository) {
		this.imageRepository = imageRepository;
		this.filterRepository = filterRepository;
		this.tagRepository = tagRepository;
	}

	/**
	 * Create a class that will search for matches of the given tag within the hamming distance, all records are
	 * considered.
	 * 
	 * @param hammingDistance
	 *            maximum distance to consider for a match
	 * 
	 * @param tag
	 *            to search for
	 * 
	 * @return Constructed instance of {@link FilterSorter}
	 */
	public Thread newFilterSorterAllImages(int hammingDistance, Tag tag) {
		return new FilterSorter(hammingDistance, tag, filterRepository, tagRepository, imageRepository);
	}

	/**
	 * Create a class that will search for matches of the given tag within the hamming distance, only records starting
	 * with the given path are considered.
	 * 
	 * @param hammingDistance
	 *            maximum distance to consider for a match
	 * @param tag
	 *            to search for
	 * @param scope
	 *            limit results to this path
	 * @return Constructed instance of {@link FilterSorter}
	 */
	public Thread newFilterSorterRestrictByPath(int hammingDistance, Tag tag, Path scope) {
		return new FilterSorter(hammingDistance, tag, filterRepository, tagRepository, imageRepository, scope);
	}

	/**
	 * Create a instance that will sort all images within the given hamming distance. Only images starting with the
	 * given path will be considered.
	 * 
	 * @param hammingDistance
	 *            maximum distance to match a hash
	 * @param path
	 *            only consider images starting with this path
	 * @return Constructed instance of {@link ImageSorter}
	 */
	public Thread newImageSorter(int hammingDistance, String path) {
		return new ImageSorter(hammingDistance, path, imageRepository, GuiEventBus.getInstance());
	}
}
