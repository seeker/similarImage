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
package com.github.dozedoff.similarImage.db;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.commonj.util.Pair;

public final class DBWriter {
	private final static Logger logger = LoggerFactory.getLogger(DBWriter.class);

	private final int MAX_RETRY = 3;
	private final Persistence persistence;
	private final Thread dbDaemon;

	LinkedBlockingQueue<Pair<List<ImageRecord>, Integer>> pendingWrites = new LinkedBlockingQueue<Pair<List<ImageRecord>, Integer>>();

	public DBWriter(Persistence persistence) {
		this.persistence = persistence;
		dbDaemon = new DBWriterDaemon();
		dbDaemon.setDaemon(true);
		dbDaemon.start();
	}

	public void add(List<ImageRecord> records) {
		if(!pendingWrites.offer(new Pair<List<ImageRecord>, Integer>(records, 0))){
			logger.error("Failed to re-add list with {} entries", records.size());
		}
		
		logger.debug("Adding list with {} entries to queue", records.size());
	}

	public void shutdown() {
		if (dbDaemon != null) {
			dbDaemon.interrupt();
		}
	}

	private class DBWriterDaemon extends Thread {

		public DBWriterDaemon() {
			setName("DBWriter daemon");
		}

		@Override
		public void run() {
			while (!isInterrupted()) {
				try {
					Pair<List<ImageRecord>, Integer> work = pendingWrites.take();
					List<ImageRecord> records = work.getLeft();
					try {
						persistence.batchAddRecord(records);
					} catch (Exception e) {
						reQueue(work);
					}
				} catch (InterruptedException e) {
					interrupt();
				}
			}
		}

		private void reQueue(Pair<List<ImageRecord>, Integer> work) {
			int retryCount = work.getRight();
			List<ImageRecord> records = work.getLeft();

			if (retryCount >= MAX_RETRY) {
				logger.error("Giving up on adding list with {} entries", records.size());

				if (logger.isDebugEnabled()) {
					for (ImageRecord ir : records) {
						logger.debug("{} -- {}", ir.getPath(), ir.getpHash());
					}
				}
			} else {
				retryCount++;
				logger.warn("Re-adding failed list with {} entries to queue, {} attempt", records.size(), retryCount);

				if (!pendingWrites.offer(new Pair<List<ImageRecord>, Integer>(records, retryCount))) {
					logger.error("Failed to re-add list with {} entries", records.size());
				}
			}
		}
	}
}
