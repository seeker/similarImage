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
package com.github.dozedoff.similarImage.io;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

public class Statistics {
	private final AtomicInteger foundFiles = new AtomicInteger();
	private final AtomicInteger processedFiles = new AtomicInteger();
	private final AtomicInteger failedFiles = new AtomicInteger();
	private final AtomicInteger skippedFiles = new AtomicInteger();

	private LinkedList<StatisticsChangedListener> statisticsChangedListners = new LinkedList<>();

	public enum StatisticsEvent {
		FOUND_FILES, PROCESSED_FILES, FAILED_FILES, SKIPPED_FILES
	}

	public int getFoundFiles() {
		return foundFiles.get();
	}

	public void incrementFoundFiles() {
		dispatchEvent(StatisticsEvent.FOUND_FILES, foundFiles.incrementAndGet());
	}

	public int getProcessedFiles() {
		return processedFiles.get();
	}

	public void incrementProcessedFiles() {
		dispatchEvent(StatisticsEvent.PROCESSED_FILES, processedFiles.incrementAndGet());
	}

	public int getFailedFiles() {
		return failedFiles.get();
	}

	public void incrementFailedFiles() {
		dispatchEvent(StatisticsEvent.FAILED_FILES, failedFiles.incrementAndGet());
	}

	public int getSkippedFiles() {
		return skippedFiles.get();
	}

	public void incrementSkippedFiles() {
		dispatchEvent(StatisticsEvent.SKIPPED_FILES, skippedFiles.incrementAndGet());
	}

	public void reset() {
		foundFiles.set(0);
		failedFiles.set(0);
		processedFiles.set(0);
		skippedFiles.set(0);
	}

	public void addStatisticsListener(StatisticsChangedListener listener) {
		statisticsChangedListners.add(listener);
	}

	public void removeStatisticsListener(StatisticsChangedListener listener) {
		statisticsChangedListners.remove(listener);
	}

	private void dispatchEvent(StatisticsEvent event, int newValue) {
		for (StatisticsChangedListener listener : statisticsChangedListners) {
			listener.statisticsChangedEvent(event, newValue);
		}
	}
}
