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
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.commonj.hash.ImagePHash;
import com.github.dozedoff.commonj.util.Pair;
import com.github.dozedoff.similarImage.db.DBWriter;
import com.github.dozedoff.similarImage.thread.ImageHashJob;

public class PhashWorker {
	private final static Logger logger = LoggerFactory.getLogger(PhashWorker.class);

	private final DBWriter dbWriter;
	private ThreadPoolExecutor tpe;
	private LinkedBlockingQueue<Runnable> jobQueue;
	private ImagePHash phash;
	private boolean shuttingDown = false;

	public PhashWorker(DBWriter dbWriter) {
		this.dbWriter = dbWriter;

		phash = new ImagePHash(32, 9);
		jobQueue = new LinkedBlockingQueue<>();
		tpe = new ThreadPoolExecutor(1, Runtime.getRuntime().availableProcessors(), 10, TimeUnit.SECONDS, jobQueue);
	}

	public void toHash(List<Pair<Path, BufferedImage>> data) {
		if (shuttingDown) {
			logger.error("Could not add job during shutdown");
		}

		ImageHashJob job = new ImageHashJob(data, dbWriter, phash);
		tpe.execute(job);
	}

	public void shutdown() {
		this.shuttingDown = true;
		tpe.shutdown();
	}
}
