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
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.google.common.collect.Multimap;

/**
 * Builder to build {@link ImageQueryPipeline}s.
 * 
 * @author Nicholas Wright
 *
 */
public class ImageQueryPipelineBuilder {
	private final ImageRepository imageRepository;
	private final GroupImagesStage grouper;

	private Function<Path, List<ImageRecord>> imageQuery;
	private List<Function<Multimap<Long, ImageRecord>, Multimap<Long, ImageRecord>>> postProcessing;

	/**
	 * Create a new builder that can be used to create {@link ImageQueryPipeline}.
	 * 
	 * @param imageRepository
	 *            access to the datasource for image queries
	 */
	public ImageQueryPipelineBuilder(ImageRepository imageRepository) {
		this.imageRepository = imageRepository;
		this.grouper = new GroupImagesStage();
		this.imageQuery = new ImageQueryStage(imageRepository);
		this.postProcessing = new LinkedList<>();
	}

	/**
	 * Do not include ignored images in the result.
	 * 
	 * @return instance of this builder for method chaining.
	 */
	public ImageQueryPipelineBuilder excludeIgnored() {
		this.imageQuery = new IgnoredImageQueryStage(imageRepository);
		return this;
	}

	/**
	 * Remove single image groups during post processing.
	 * 
	 * @return instance of this builder for method chaining.
	 */
	public ImageQueryPipelineBuilder removeSingleImageGroups() {
		this.postProcessing.add(new RemoveSingleImageSetStage());
		return this;
	}

	/**
	 * Remove duplicate image groups during post processing.
	 * 
	 * @return instance of this builder for method chaining.
	 */
	public ImageQueryPipelineBuilder removeDuplicateGroups() {
		this.postProcessing.add(new RemoveDuplicateSetStage());
		return this;
	}

	/**
	 * Build the {@link ImageQueryPipeline} with the configuration of this builder.
	 * 
	 * @return the configured {@link ImageQueryPipeline}
	 */
	public ImageQueryPipeline build() {
		return new ImageQueryPipeline(imageQuery, grouper, postProcessing);
	}
}
