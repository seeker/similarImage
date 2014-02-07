/*  Copyright (C) 2014  Nicholas Wright
    
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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.IIOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.commonj.hash.ImagePHash;
import com.github.dozedoff.commonj.util.Pair;
import com.github.dozedoff.similarImage.db.DBWriter;
import com.github.dozedoff.similarImage.db.ImageRecord;

public class ImageHashJob implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(ImageHashJob.class);

	private List<Pair<Path, BufferedImage>> work;
	private DBWriter dbWriter;
	private ImagePHash phash;

	public ImageHashJob(List<Pair<Path, BufferedImage>> work, DBWriter dbWriter, ImagePHash phash) {
		this.work = work;
		this.dbWriter = dbWriter;
		this.phash = phash;
	}

	@Override
	public void run() {
		LinkedList<ImageRecord> newRecords = new LinkedList<ImageRecord>();

		for (Pair<Path, BufferedImage> pair : work) {

			Path path = pair.getLeft();

			try {
				BufferedImage img = pair.getRight();
				long hash = phash.getLongHashScaledImage(img);

				ImageRecord record = new ImageRecord(path.toString(), hash);
				newRecords.add(record);
			} catch (IIOException iioe) {
				logger.warn("Unable to process image {} - {}", path, iioe.getMessage());
			} catch (IOException e) {
				logger.warn("Could not load file {} - {}", path, e.getMessage());
			} catch (SQLException e) {
				logger.warn("Database operation failed", e);
			} catch (Exception e) {
				logger.warn("Failed to hash image {} - {}", path, e.getMessage());
			}
		}

		dbWriter.add(newRecords);
		logger.debug("{} records added to DBWriter", newRecords.size());
	}
}
