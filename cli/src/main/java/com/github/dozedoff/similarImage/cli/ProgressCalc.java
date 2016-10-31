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

import com.github.dozedoff.similarImage.io.Statistics;

/**
 * Calculates the progress of hashing according to the recorded stats.
 */
public class ProgressCalc {
	private static final double PERCENT_100 = 100;

	private final Statistics statistics;

	/**
	 * Create a new instance using the given {@link Statistics} object. The instance will be used to get data for the
	 * calculations.
	 * 
	 * @param statistics
	 *            instance to use
	 */
	public ProgressCalc(Statistics statistics) {
		this.statistics = statistics;
	}

	/**
	 * Get the total of all images processed.
	 * 
	 * @return the percentage of images processed. 100% = 100.000
	 */
	public double totalProgressPercent() {
		if (statistics.getFoundFiles() == 0) {
			return 0;
		} else {
			return (PERCENT_100 / statistics.getFoundFiles())
					* (statistics.getProcessedFiles() + statistics.getFailedFiles());
		}
	}
	
	/**
	 * Get the percentage of images that are corrupt.
	 * 
	 * @return the percentage of corrupt images. 100% = 100.000
	 */
	public double corruptPercent() {
		if (statistics.getFoundFiles() == 0) {
			return 0;
		} else {
			return (PERCENT_100 / statistics.getFoundFiles()) * statistics.getFailedFiles();
		}
	}

	/**
	 * Output the progress in a formatted string.
	 * 
	 * @return a formatted string
	 */
	@Override
	public String toString() {
		return String.format("Total progress: %.2f%%, corrupt images: %.2f%%", totalProgressPercent(),
				corruptPercent());
	}
}
