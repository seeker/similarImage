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
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;

/**
 * Stage to get {@link ImageRecord}s without ignored images.
 * 
 * @author Nicholas Wright
 *
 */
public class IgnoredImageQueryStage implements Function<Path, List<ImageRecord>> {
	private static final Logger LOGGER = LoggerFactory.getLogger(IgnoredImageQueryStage.class);

	private final ImageRepository imageRepository;

	/**
	 * Create a new stage to get non-ignored images based on path.
	 * 
	 * @param imageRepository
	 *            datasource to query for images
	 */
	public IgnoredImageQueryStage(ImageRepository imageRepository) {
		this.imageRepository = imageRepository;
	}

	/**
	 * Query non-ignored images for the given path.
	 * 
	 * @param path
	 *            path to limit query. If null or empty, all images will be returned.
	 * 
	 * @return a list of images, without ignored images
	 */
	@Override
	public List<ImageRecord> apply(Path path) {
		List<ImageRecord> result = Collections.emptyList();

		try {
			if (path == null || Paths.get("").equals(path)) {
				result = imageRepository.getAllWithoutIgnored();
			} else {
				result = imageRepository.getAllWithoutIgnored(path);
			}
		} catch (RepositoryException e) {
			LOGGER.error("Failed to query non-ignored images: {}, cause: {}", e.toString(), e.getCause());
		}

		return result;
	}
}
