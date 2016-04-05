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

/**
 * Load an image and calculate the hash, then store the result in the database.
 * 
 * @author Nicholas Wright
 *
 */
public class ImageHashJob implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(ImageHashJob.class);

	private final Persistence persistence;
	private final Path image;
	private final ImagePHash hasher;
	
	public ImageHashJob(Path image, ImagePHash hasher, Persistence persistence) {
		this.image = image;
		this.persistence = persistence;
		this.hasher = hasher;
	}

	@Override
	public void run() {
		try {
			processFile(image);
		} catch (IIOException e) {
			logger.warn("Failed to process image(IIO) - {}", e.getMessage());
			try {
				persistence.addBadFile(new BadFileRecord(image));
			} catch (SQLException e1) {
				logger.warn("Failed to add bad file record for {} - {}", image, e.getMessage());
			}
		} catch (IOException e) {
			logger.warn("Failed to load file - {}", e.getMessage());
		} catch (SQLException e) {
			logger.warn("Failed to query database - {}", e.getMessage());
		}
	}

	private void processFile(Path next) throws SQLException, IOException {
		long hash = hasher.getLongHash(new BufferedInputStream(Files.newInputStream(next)));
		persistence.addRecord(new ImageRecord(next.toString(), hash));
	}

	/**
	 * This method is now pointless.
	 */
	@Deprecated
	public int getJobSize() {
		return 0;
	}
}
