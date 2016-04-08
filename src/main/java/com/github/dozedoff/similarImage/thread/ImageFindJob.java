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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scan the given directory and all sub-directories for image files and check
 * them against the database. Found files that have not been hashed will be
 * added as {@link ImageHashJob}.
 * 
 * @author Nicholas Wright
 *
 */
public class ImageFindJob implements Runnable {
	private final Logger logger = LoggerFactory.getLogger(ImageFindJob.class);
	private final String searchPath;
	private final ImageFindJobVisitor visitor;

	public ImageFindJob(String searchPath, ImageFindJobVisitor visitor) {
		this.searchPath = searchPath;
		this.visitor = visitor;
	}

	@Override
	public void run() {
		logger.info("Scanning {} for images...", searchPath);

		try {
			Files.walkFileTree(Paths.get(searchPath), visitor);
		} catch (IOException e) {
			logger.error("Failed to walk file tree", e);
		}

		logger.info("Finished scanning for images in {}, found {} images", searchPath, visitor.getFileCount());
	}
}
