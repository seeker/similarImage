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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.commonj.time.StopWatch;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Persistence;

public class PhashWorker extends Thread {
	private final static Logger logger = LoggerFactory.getLogger(PhashWorker.class);
	private static int workerNumber = 0;
	
	List<Path> imagePaths;
	List<Long> phashes;
	
	public PhashWorker(List<Path> imagePaths, List<Long> phashes) {
		this.imagePaths = imagePaths;
		this.phashes = phashes;
		this.setName("pHashWorker " + workerNumber);
		workerNumber++;
	}
	
	@Override
	public void run() {
		calculateHashes(imagePaths, phashes);
	}
	
	private void calculateHashes(List<Path> imagePaths, List<Long> phashes) {
		logger.info("Hashing files...");
		StopWatch sw = new StopWatch();
		Persistence persistence = Persistence.getInstance();
		
		sw.start();
		ImagePHash phash = new ImagePHash(32,9);
		
		for(Path path : imagePaths) {
			try {
				if(persistence.isPathRecorded(path)) {
					continue;
				}
				
				InputStream is = new BufferedInputStream(Files.newInputStream(path, StandardOpenOption.READ));
				long hash = phash.getLongHash(is);
				
				ImageRecord record = new ImageRecord(path.toString(), hash);
				
				persistence.addRecord(record);
			} catch (IOException e) {
				logger.warn("Could not load file {}", path, e);
			} catch (SQLException e) {
				logger.warn("Database operation failed", e);
			}
		}
		sw.stop();
		
		logger.info("Finished hashing images, duration: {}", sw.getTime());
	}
}
