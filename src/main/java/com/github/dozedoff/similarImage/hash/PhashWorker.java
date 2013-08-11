/*  Copyright (C) 2013  Nicholas Wright
    
    This file is part of similarImage - A similar image finder using pHash
    
    mmut is free software: you can redistribute it and/or modify
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
package com.github.dozedoff.similarImage.hash;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.LinkedList;

import javax.imageio.IIOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.commonj.hash.ImagePHash;
import com.github.dozedoff.commonj.util.Pair;
import com.github.dozedoff.similarImage.db.DBWriter;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.io.ImageProducer;

public class PhashWorker extends Thread {
	private final static Logger logger = LoggerFactory.getLogger(PhashWorker.class);
	private static int workerNumber = 0;
	private int localWorkerNumber;
	private final int MAX_WORK_BATCH_SIZE = 20;

	private final ImageProducer producer;
	private final DBWriter dbWriter;

	public PhashWorker(ImageProducer producer, DBWriter dbWriter) {
		this.producer = producer;
		this.dbWriter = dbWriter;
		localWorkerNumber = workerNumber;
		workerNumber++;
		this.setName("pHash worker " + localWorkerNumber);
	}

	@Override
	public void run() {
		calculateHashes();
	}

	public void stopWorker() {
		interrupt();
	}

	private void calculateHashes() {
		logger.info("{} started", this.getName());
		ImagePHash phash = new ImagePHash(32, 9);
		LinkedList<Pair<Path, BufferedImage>> work = new LinkedList<Pair<Path, BufferedImage>>();
		LinkedList<ImageRecord> newRecords = new LinkedList<ImageRecord>();

		while (!isInterrupted()) {
			try {
				if (!producer.hasWork()) {
					break;
				}

				producer.drainTo(work, MAX_WORK_BATCH_SIZE);
			} catch (InterruptedException e1) {
				interrupt();
			}

			for (Pair<Path, BufferedImage> pair : work) {
				if (isInterrupted()) {
					break;
				}

				Path path = pair.getLeft();

				try {
					BufferedImage img = pair.getRight();
					long hash = phash.getLongHash(img);

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
			newRecords = new LinkedList<ImageRecord>();

			work.clear();
		}

		logger.info("{} terminated", this.getName());
	}
}
