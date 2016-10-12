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
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Persistence;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;
import com.github.dozedoff.similarImage.db.repository.ormlite.OrmliteImageRepository;
import com.github.dozedoff.similarImage.io.HashAttribute;
import com.github.dozedoff.similarImage.io.Statistics;
import com.j256.ormlite.dao.DaoManager;

import at.dhyan.open_imaging.GifDecoder;
import at.dhyan.open_imaging.GifDecoder.GifImage;

/**
 * Load an image and calculate the hash, then store the result in the database and as an extended attribute.
 * 
 * @author Nicholas Wright
 *
 */
public class ImageHashJob implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(ImageHashJob.class);
	private static final String EXCEPTION_STACKTRACE = "Trace for {} {}";

	private final ImageRepository imageRepository;
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
	 * @deprecated Use {@link ImageHashJob#ImageHashJob(Path, ImagePHash, ImageRepository, Statistics)} instead.
	 */
	@Deprecated
	public ImageHashJob(Path image, ImagePHash hasher, Persistence persistence, Statistics statistics) {
		this.image = image;
		this.hasher = hasher;
		this.statistics = statistics;

		try {
			this.imageRepository = new OrmliteImageRepository(
					DaoManager.createDao(persistence.getCs(), ImageRecord.class));
		} catch (RepositoryException | SQLException e) {
			throw new RuntimeException("Failed to create repository");
		}
	}

	/**
	 * Create a class that will hash an image an store the result.
	 * 
	 * @param image
	 *            to hash
	 * @param hasher
	 *            class that does the hash computation
	 * @param imageRepository
	 *            access to the image datasource
	 * @param statistics
	 *            tracking file stats
	 */
	public ImageHashJob(Path image, ImagePHash hasher, ImageRepository imageRepository, Statistics statistics) {
		this.image = image;
		this.hasher = hasher;
		this.statistics = statistics;
		this.imageRepository = imageRepository;
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
			statistics.incrementFailedFiles();
		} catch (IOException e) {
			LOGGER.warn("Failed to load file {}: {}", image, e.toString());
			statistics.incrementFailedFiles();
		} catch (RepositoryException e) {
			LOGGER.warn("Failed to query repository for {}: {}", image, e.toString());
			statistics.incrementFailedFiles();
		} catch (ArrayIndexOutOfBoundsException e) {
			LOGGER.error("Failed to process image {}: {}", image, e.toString());
			LOGGER.debug(EXCEPTION_STACKTRACE, image, e);
			statistics.incrementFailedFiles();
		}
	}

	private long processFile(Path next) throws RepositoryException, IOException {
		statistics.incrementProcessedFiles();

		Path filename = next.getFileName();
		try (InputStream bis = new BufferedInputStream(Files.newInputStream(next))) {
			if (filename != null && filename.toString().toLowerCase().endsWith(".gif")) {
				GifImage gi = GifDecoder.read(bis);

				long hash = hasher.getLongHash(gi.getFrame(0));
				imageRepository.store(new ImageRecord(next.toString(), hash));
				return hash;
			} else {

				return doHash(next, bis);
			}
		}
	}

	private long doHash(Path next, InputStream is) throws IOException, RepositoryException {
		long hash = hasher.getLongHash(is);
		imageRepository.store(new ImageRecord(next.toString(), hash));
		return hash;
	}

}
