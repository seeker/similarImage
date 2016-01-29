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

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.IIOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.commonj.time.StopWatch;
import com.github.dozedoff.similarImage.db.BadFileRecord;
import com.github.dozedoff.similarImage.db.Persistence;
import com.github.dozedoff.similarImage.hash.PhashWorker;

public class ImageLoadJob implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(ImageLoadJob.class);

	private List<Path> files;
	private Persistence persistence;
	private PhashWorker phw;
	private LinkedList<Path> output;

	private StopWatch sw;

	public ImageLoadJob(List<Path> files, PhashWorker phw, Persistence persistence) {
		this.files = files;
		this.phw = phw;
		this.persistence = persistence;

		output = new LinkedList<>();
	}

	@Override
	public void run() {
		if (logger.isDebugEnabled()) {
			sw = new StopWatch();
			sw.start();
		}

		for (Path p : files) {
			try {
				processFile(p);
			} catch (IIOException e) {
				logger.warn("Failed to process image(IIO) - {}", e.getMessage());
				try {
					persistence.addBadFile(new BadFileRecord(p));
				} catch (SQLException e1) {
					logger.warn("Failed to add bad file record for {} - {}", p, e.getMessage());
				}
			} catch (IOException e) {
				logger.warn("Failed to load file - {}", e.getMessage());
			} catch (SQLException e) {
				logger.warn("Failed to query database - {}", e.getMessage());
			}
		}

		if (logger.isDebugEnabled()) {
			sw.stop();
			logger.debug("Loaded {} files in {}", files.size(), sw.getTime());
			sw.reset();
			sw.start();
		}

		phw.toHash(output);

		if (logger.isDebugEnabled()) {
			sw.stop();
			logger.debug("Waited {} to queue loaded files in hash worker", sw.getTime());
		}
	}

	private void processFile(Path next) throws SQLException, IOException {
		if (persistence.isBadFile(next) || persistence.isPathRecorded(next)) {
			return;
		}

		output.add(next);
	}

	public int getJobSize() {
		return files.size();
	}
}
