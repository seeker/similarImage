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
import java.util.List;
import java.util.function.Function;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

/**
 * Pipeline for modular image queries.
 * 
 * @author Nicholas Wright
 *
 */
public class ImageQueryPipeline implements Function<Path, Multimap<Long, ImageRecord>> {
	private final Function<Path, List<ImageRecord>> imageQueryStage;
	private final Function<Collection<ImageRecord>, Multimap<Long, ImageRecord>> imageGrouper;
	private Collection<Function<Multimap<Long, ImageRecord>, Multimap<Long, ImageRecord>>> postProcessingStages;
	
	/**
	 * Create a new pipeline that uses the given stages for processing queries.
	 * 
	 * @param imageQueryStage
	 *            how images will be queried from a datasource
	 * @param imageGrouper
	 *            how images will be grouped
	 * @param postProcessingStages
	 *            stages for performing post-processing
	 */
	public ImageQueryPipeline(Function<Path, List<ImageRecord>> imageQueryStage,
			Function<Collection<ImageRecord>, Multimap<Long, ImageRecord>> imageGrouper,
			Collection<Function<Multimap<Long, ImageRecord>, Multimap<Long, ImageRecord>>> postProcessingStages) {
		this.imageQueryStage = imageQueryStage;
		this.imageGrouper = imageGrouper;
		this.postProcessingStages = postProcessingStages;
	}

	/**
	 * Query images with the path and apply all pipeline stages.
	 * 
	 * @param path
	 *            to limit the images by scope, if null, all images will be used
	 * 
	 * @return images grouped by hash
	 */
	@Override
	public Multimap<Long, ImageRecord> apply(Path path) {
		List<ImageRecord> images = imageQueryStage.apply(path);
		Multimap<Long, ImageRecord> groups = imageGrouper.apply(images);
		return postProcessing(groups);
	}

	private Multimap<Long, ImageRecord> postProcessing(Multimap<Long, ImageRecord> groups) {
		Multimap<Long, ImageRecord> step = groups;

		for (Function<Multimap<Long, ImageRecord>, Multimap<Long, ImageRecord>> ppStage : postProcessingStages) {
			step = ppStage.apply(step);
		}

		return step;
	}

	/**
	 * Returns the post-processing stages of this pipeline. The
	 * 
	 * @return an immutable list of the post-processing stages
	 */
	public Collection<Function<Multimap<Long, ImageRecord>, Multimap<Long, ImageRecord>>> getPostProcessingStages() {
		return ImmutableList.copyOf(postProcessingStages);
	}
}
