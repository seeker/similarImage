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
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.commonj.hash.ImagePHash;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.io.HashAttribute;
import com.github.dozedoff.similarImage.io.Statistics;
import com.github.dozedoff.similarImage.thread.ImageHashJob;

/**
 * Creates hashing jobs for files using the given hasher.
 * 
 * @author Nicholas Wright
 *
 */
public class HashingHandler implements HashHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(HashingHandler.class);

	private final ImageRepository imageRepository;
	private final ImagePHash hasher;
	private final Statistics statistics;
	private final HashAttribute hashAttribute;
	private final ExecutorService threadPool;

	/**
	 * Setup the handler so it can hash files and update the database.
	 * 
	 * @param threadPool
	 *            used to execute hashing jobs
	 * 
	 * @param hasher
	 *            class that does the hash computation
	 * @param imageRepository
	 *            access to the image datasource
	 * @param statistics
	 *            tracking file stats
	 * @param hashAttribute
	 *            used to store hashes as extended attributes
	 */
	public HashingHandler(ExecutorService threadPool, ImagePHash hasher, ImageRepository imageRepository,
			Statistics statistics, HashAttribute hashAttribute) {
		this.hasher = hasher;
		this.statistics = statistics;
		this.hashAttribute = hashAttribute;
		this.threadPool = threadPool;
		this.imageRepository = imageRepository;
	}

	/**
	 * Create a new {@link ImageHashJob} and execute it.
	 * 
	 * @param file
	 *            the image to hash
	 * @return this handler will always return true
	 */
	@Override
	public boolean handle(Path file) {
		LOGGER.trace("Handling {} with {}", file, HashingHandler.class.getSimpleName());

		ImageHashJob job = new ImageHashJob(file, hasher, imageRepository, statistics);
		job.setHashAttribute(hashAttribute);
		threadPool.execute(job);
		return true;
	}
}
