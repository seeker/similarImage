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

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Tag;
import com.github.dozedoff.similarImage.db.repository.FilterRepository;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.google.common.collect.Multimap;

/**
 * Builder to build {@link ImageQueryPipeline}s.
 * 
 * @author Nicholas Wright
 *
 */
public class ImageQueryPipelineBuilder {
	private static final Logger LOGGER = LoggerFactory.getLogger(ImageQueryPipelineBuilder.class);

	private final ImageRepository imageRepository;
	private final FilterRepository filterRepository;

	private Function<Path, List<ImageRecord>> imageQuery;
	private List<Function<Multimap<Long, ImageRecord>, Multimap<Long, ImageRecord>>> postProcessing;
	private int hammingDistance;
	private Function<Collection<ImageRecord>, Multimap<Long, ImageRecord>> imageGrouper;

	/**
	 * Create a new builder that can be used to create {@link ImageQueryPipeline}.
	 * 
	 * @param imageRepository
	 *            access to the datasource for image queries
	 * @param filterRepository
	 *            access to the datasource for filter queries
	 */
	public ImageQueryPipelineBuilder(ImageRepository imageRepository, FilterRepository filterRepository) {
		this.imageRepository = imageRepository;
		this.filterRepository = filterRepository;
		this.imageQuery = new ImageQueryStage(imageRepository);
		this.postProcessing = new LinkedList<>();
		this.hammingDistance = 0;
	}

	/**
	 * Do not include ignored images in the result.
	 * 
	 * @return instance of this builder for method chaining
	 */
	public ImageQueryPipelineBuilder excludeIgnored() {
		this.imageQuery = new IgnoredImageQueryStage(imageRepository);
		return this;
	}

	/**
	 * Remove single image groups during post processing.
	 * 
	 * @return instance of this builder for method chaining
	 */
	public ImageQueryPipelineBuilder removeSingleImageGroups() {
		this.postProcessing.add(new RemoveSingleImageSetStage());
		return this;
	}

	/**
	 * Remove duplicate image groups during post processing.
	 * 
	 * @return instance of this builder for method chaining
	 */
	public ImageQueryPipelineBuilder removeDuplicateGroups() {
		this.postProcessing.add(new RemoveDuplicateSetStage());
		return this;
	}

	/**
	 * Set the hamming distance for the query.
	 * 
	 * @param distance
	 *            hashes within this distance count as a match
	 * 
	 * @return instance of this builder for method chaining
	 * @throws IllegalArgumentException
	 *             if the distance is negative
	 */
	public ImageQueryPipelineBuilder distance(int distance) throws IllegalArgumentException {
		if (distance < 0) {
			throw new IllegalArgumentException("Distance must be 0 or greater");
		}

		this.hammingDistance = distance;
		return this;
	}

	/**
	 * Group images by hashes that are tagged with the given tag.
	 * 
	 * @param tag
	 *            to query for
	 * @return instance of this builder for method chaining
	 */
	public ImageQueryPipelineBuilder groupByTag(Tag tag) {
		this.imageGrouper = new GroupByTagStage(filterRepository, tag, hammingDistance);
		return this;
	}

	/**
	 * Group matches for every image.
	 * 
	 * @return instance of this builder for method chaining
	 */
	public ImageQueryPipelineBuilder groupAll() {
		this.imageGrouper = new GroupImagesStage(hammingDistance);
		return this;
	}

	/**
	 * Build the {@link ImageQueryPipeline} with the configuration of this builder.
	 * 
	 * @return the configured {@link ImageQueryPipeline}
	 */
	public ImageQueryPipeline build() {
		if (imageGrouper == null) {
			imageGrouper = new GroupImagesStage(hammingDistance);
			LOGGER.warn("No image group stage set, using {}", imageGrouper.getClass().getSimpleName());
		}

		return new ImageQueryPipeline(imageQuery, imageGrouper, postProcessing);
	}

	/**
	 * Create a new {@link ImageQueryPipeline}.
	 * 
	 * @param imageRepository
	 *            to use for image queries
	 * @param filterRepository
	 *            to use for filter queries
	 * @return a new {@link ImageQueryPipelineBuilder} instance
	 */
	public static ImageQueryPipelineBuilder newBuilder(ImageRepository imageRepository,
			FilterRepository filterRepository) {
		return new ImageQueryPipelineBuilder(imageRepository, filterRepository);
	}
}
