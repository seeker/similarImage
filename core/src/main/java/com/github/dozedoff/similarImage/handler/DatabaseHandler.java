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
package com.github.dozedoff.similarImage.handler;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;
import com.github.dozedoff.similarImage.io.Statistics;

/**
 * Handler that queries the database for hashes.
 * 
 * @author Nicholas Wright
 *
 */
public class DatabaseHandler implements HashHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(HashingHandler.class);

	private final ImageRepository imageRepository;
	private Statistics statistics;

	/**
	 * Setup the handler so it can query the database.
	 * 
	 * @param imageRepository
	 *            used to access the image datasource
	 * @param statistics
	 *            for stats tracking
	 */
	public DatabaseHandler(ImageRepository imageRepository, Statistics statistics) {
		this.statistics = statistics;
		this.imageRepository = imageRepository;
	}

	/**
	 * Check the database for the file.
	 * 
	 * @param file
	 *            the image to query the database for
	 * @return true if the file is found in the database
	 */
	@Override
	public boolean handle(Path file) {
		LOGGER.trace("Handling {} with {}", file, ExtendedAttributeHandler.class.getSimpleName());
		try {
			if (isInDatabase(file)) {
				LOGGER.trace("{} was found in the database");
				statistics.incrementSkippedFiles();
				statistics.incrementProcessedFiles();
				return true;
			}
		} catch (RepositoryException e) {
			LOGGER.error("Failed to check the database for {} ({})", file, e.toString());
		}

		return false;
	}

	private boolean isInDatabase(Path path) throws RepositoryException {
		return imageRepository.getByPath(path) != null;
	}
}
