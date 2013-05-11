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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

import javax.imageio.IIOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Persistence;
import com.github.dozedoff.similarImage.gui.IGUIevent;

public class PhashWorker extends Thread {
	private final static Logger logger = LoggerFactory.getLogger(PhashWorker.class);
	private static int workerNumber = 0;
	private int localWorkerNumber;
	private final int MAX_WORK_BATCH_SIZE = 20;
	private boolean stop = false;
	private IGUIevent guiEvent;
	
	LinkedBlockingQueue<Path> imagePaths;
	
	public PhashWorker(LinkedBlockingQueue<Path> imagePaths, IGUIevent guiEvent) {
		this.imagePaths = imagePaths;
		localWorkerNumber = workerNumber;
		workerNumber++;
		this.setName("pHash worker " + localWorkerNumber);
		this.guiEvent = guiEvent;
	}
	
	@Override
	public void run() {
		calculateHashes(imagePaths);
	}
	
	public void stopWorker() {
		this.stop = true;
	}
	
	private void calculateHashes(LinkedBlockingQueue<Path> imagePaths) {
		logger.info("{} started", this.getName());
		Persistence persistence = Persistence.getInstance();
		ImagePHash phash = new ImagePHash(32,9);
		LinkedList<Path> work = new LinkedList<Path>();
		LinkedList<ImageRecord> newRecords = new LinkedList<ImageRecord>();
		
		while(!stop) {
			if(imagePaths.isEmpty()) {
				logger.info("No more work, {} terminating...", this.getName());
				break;
			}
			
			imagePaths.drainTo(work, MAX_WORK_BATCH_SIZE);
			
			for (Path path : work) {
				if(stop) {
					break;
				}
				
				try {
					if (persistence.isPathRecorded(path)) {
						continue;
					}

					InputStream is = loadData(path);
					long hash = phash.getLongHash(is);
					is.close();

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
			try {
				Persistence.getInstance().batchAddRecord(newRecords);
				newRecords.clear();
			} catch (Exception e) {
				logger.warn("Batch add failed - {}", e.getMessage());
			}
			
			guiEvent.progressUpdate(work.size());
			work.clear();
		}
		
		logger.info("{} terminated", this.getName());
	}
	
	private InputStream loadData(Path path) throws IOException {
		byte[] data = Files.readAllBytes(path);
		return new ByteArrayInputStream(data);
	}
}
