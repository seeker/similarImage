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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

import javax.imageio.IIOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.commonj.hash.ImagePHash;
import com.github.dozedoff.similarImage.db.BadFileRecord;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Persistence;
import com.github.dozedoff.similarImage.io.HashAttribute;
import com.github.dozedoff.similarImage.io.Statistics;

/**
 * Load an image and calculate the hash, then store the result in the database and as an extended attribute.
 * 
 * @author Nicholas Wright
 *
 */
public class ImageHashJob implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(ImageHashJob.class);
	private static final String EXCEPTION_STACKTRACE = "Trace for {} {}";

	private final Persistence persistence;
	private final Path image;
	private final ImagePHash hasher;
	private final Statistics statistics;
	private HashAttribute hashAttribute;

	/**
	 * Create a class that will hash an image an store the result.
	 * 
	 * @param image
	 *            to hash
	 * @param hasher
	 *            class that does the hash computation
	 * @param persistence
	 *            database access to store the result
	 * @param statistics
	 *            tracking file stats
	 */
	public ImageHashJob(Path image, ImagePHash hasher, Persistence persistence, Statistics statistics) {
		this.image = image;
		this.persistence = persistence;
		this.hasher = hasher;
		this.statistics = statistics;
	}

	/**
	 * Set a {@link HashAttribute} to additionally write the hash as an extended attribute.
	 * 
	 * @param hashAttribute
	 *            to use for writing extended attributes
	 */
	public final void setHashAttribute(HashAttribute hashAttribute) {
		this.hashAttribute = hashAttribute;
	}

	@Override
	public void run() {
		try {
			long hash = processFile(image);

			if (hashAttribute != null) {
				hashAttribute.writeHash(image, hash);
			}
		} catch (IIOException e) {
			LOGGER.warn("Failed to process image {} (IIO Error): {}", image, e.toString());
			LOGGER.debug(EXCEPTION_STACKTRACE, image, e);

			try {
				persistence.addBadFile(new BadFileRecord(image));
			} catch (SQLException e1) {
				LOGGER.warn("Failed to add bad file record for {} - {}", image, e.toString());
			}
			statistics.incrementFailedFiles();
		} catch (IOException e) {
			LOGGER.warn("Failed to load file {}: {}", image, e.toString());
			statistics.incrementFailedFiles();
		} catch (SQLException e) {
			LOGGER.warn("Failed to query database for {}: {}", image, e.toString());
			statistics.incrementFailedFiles();
		} catch (ArrayIndexOutOfBoundsException e) {
			LOGGER.error("Failed to process image {}: {}", image, e.toString());
			LOGGER.debug(EXCEPTION_STACKTRACE, image, e);
		}
	}

	private long processFile(Path next) throws SQLException, IOException {
		statistics.incrementProcessedFiles();
		try (InputStream bis = new BufferedInputStream(Files.newInputStream(next))) {
			long hash = hasher.getLongHash(bis);
			persistence.addRecord(new ImageRecord(next.toString(), hash));
			return hash;
		}
	}
}
