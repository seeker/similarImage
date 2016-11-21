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
package com.github.dozedoff.similarImage.cli;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

/**
 * Calculates the progress of hashing according to the recorded stats.
 */
public class ProgressCalc {
	private static final String METRIC_NAMESPACE_FILE = "file";
	public static final String METRIC_NAME_FOUND = MetricRegistry.name(ProgressCalc.class, METRIC_NAMESPACE_FILE, "found");
	public static final String METRIC_NAME_PROCESSED = MetricRegistry.name(ProgressCalc.class, METRIC_NAMESPACE_FILE, "processed");
	public static final String METRIC_NAME_FAILED = MetricRegistry.name(ProgressCalc.class, METRIC_NAMESPACE_FILE, "failed");
	public static final String METRIC_NAME_FILES_PER_SECOND = MetricRegistry.name(ProgressCalc.class, METRIC_NAMESPACE_FILE,
			"filesPerSecond");

	private static final double PERCENT_100 = 100;

	private final Counter foundFiles;
	private final Counter processedFiles;
	private final Counter failedFiles;
	private final Meter filesPerSecond;

	/**
	 * Create a new instance using the given {@link MetricRegistry} object. The instance will be used to get data for the calculations.
	 * 
	 * @param metrics
	 *            instance to use
	 */
	public ProgressCalc(MetricRegistry metrics) {
		this.foundFiles = metrics.counter(METRIC_NAME_FOUND);
		this.processedFiles = metrics.counter(METRIC_NAME_PROCESSED);
		this.failedFiles = metrics.counter(METRIC_NAME_FAILED);
		this.filesPerSecond = metrics.meter(METRIC_NAME_FILES_PER_SECOND);
	}

	/**
	 * Get the total of all images processed.
	 * 
	 * @return the percentage of images processed. 100% = 100.000
	 */
	public double totalProgressPercent() {
		if (foundFiles.getCount() == 0) {
			return 0;
		} else {
			return (PERCENT_100 / foundFiles.getCount()) * (processedFiles.getCount() + failedFiles.getCount());
		}
	}
	
	/**
	 * Get the percentage of images that are corrupt.
	 * 
	 * @return the percentage of corrupt images. 100% = 100.000
	 */
	public double corruptPercent() {
		if (foundFiles.getCount() == 0) {
			return 0;
		} else {
			return (PERCENT_100 / foundFiles.getCount()) * failedFiles.getCount();
		}
	}

	/**
	 * Output the progress in a formatted string.
	 * 
	 * @return a formatted string
	 */
	@Override
	public String toString() {
		return String.format("Total progress: %.2f%%, corrupt images: %.2f%%, files per second processed: %.2f", totalProgressPercent(),
				corruptPercent(),
				filesPerSecond.getMeanRate());
	}
}
