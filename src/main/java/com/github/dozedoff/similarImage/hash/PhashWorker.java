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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.commonj.hash.ImagePHash;
import com.github.dozedoff.commonj.util.Pair;
import com.github.dozedoff.similarImage.db.DBWriter;
import com.github.dozedoff.similarImage.io.ImageProducerObserver;
import com.github.dozedoff.similarImage.thread.ImageHashJob;
import com.github.dozedoff.similarImage.thread.NamedThreadFactory;

public class PhashWorker {
	private final static Logger logger = LoggerFactory.getLogger(PhashWorker.class);

	private final DBWriter dbWriter;
	private ThreadPoolExecutor tpe;
	private LinkedBlockingQueue<Runnable> jobQueue;
	private ImagePHash phash;
	private int maxQueueSize = 200;
	private static final int POOL_TIMEOUT = 60;
	private int hashPoolSize = 1;

	private LinkedList<ImageProducerObserver> guiUpdateListeners;

	public PhashWorker(DBWriter dbWriter, int maxQueueSize) {
		this(dbWriter);
	}

	public void setPoolSize(int poolSize) {
		this.hashPoolSize = poolSize;
		this.tpe.setCorePoolSize(poolSize);
		this.tpe.setMaximumPoolSize(poolSize);
	}

	public int getPoolSize() {
		return hashPoolSize;
	}

	public PhashWorker(DBWriter dbWriter) {
		this.dbWriter = dbWriter;

		phash = new ImagePHash(32, 9);
		jobQueue = new LinkedBlockingQueue<>(maxQueueSize);
		int processors = Runtime.getRuntime().availableProcessors();

		if (processors > 1) {
			hashPoolSize = processors - 1;
		}

		this.tpe = new HashWorkerPool(hashPoolSize, hashPoolSize, POOL_TIMEOUT, TimeUnit.SECONDS, jobQueue, new NamedThreadFactory(
				PhashWorker.class.getSimpleName()), this);
		this.tpe.allowCoreThreadTimeOut(true);

		guiUpdateListeners = new LinkedList<>();
	}

	public void toHash(List<Pair<Path, BufferedImage>> data) {
		if (tpe.isShutdown()) {
			logger.error("Cannot add jobs to a pool that has been shutdown");
			return;
		}

		try {
			ImageHashJob job = new ImageHashJob(data, dbWriter, phash);
			jobQueue.put(job);
			tpe.prestartCoreThread();
			listenersUpdateBufferLevel();
		} catch (InterruptedException e) {
			logger.info("Interrupted while adding job to queue");
		}
	}

	public void clear() {
		jobQueue.clear();
		logger.info("Job queue cleared");
	}

	public void shutdown() {
		tpe.shutdownNow();
		dbWriter.shutdown();
	}

	void listenersUpdateBufferLevel() {
		for (ImageProducerObserver listener : guiUpdateListeners) {
			listener.bufferLevelChanged(jobQueue.size());
		}
	}

	public void addGuiUpdateListener(ImageProducerObserver listener) {
		this.guiUpdateListeners.add(listener);
	}

	public void removeGuiUpdateListener(ImageProducerObserver listener) {
		this.guiUpdateListeners.remove(listener);
	}
}
