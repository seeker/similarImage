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

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import com.github.dozedoff.similarImage.io.HashAttribute;
import com.github.dozedoff.similarImage.io.Statistics;

/**
 * {@link FileVisitor} to check the progress of hashing for a given hash algorithm.
 */
public class ProgressVisitor extends SimpleFileVisitor<Path> {
	private final Statistics statistics;
	private final HashAttribute hashAttribute;

	/**
	 * Create a new visitor for the given hash. Results will be stored in the passed {@link Statistics} instance.
	 * 
	 * @param statistics
	 *            used to store results
	 * @param hashAttribute
	 *            for hash to check progress for.
	 */
	public ProgressVisitor(Statistics statistics, HashAttribute hashAttribute) {
		this.statistics = statistics;
		this.hashAttribute = hashAttribute;
	}

	/**
	 * Read the extended attributes from the file and record its processing state.
	 * 
	 * @param file
	 *            current file to read
	 * @param attrs
	 *            of the file, not used
	 * @throws IOException
	 *             if there is a problem accessing the file
	 * @return always continue processing files
	 */
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		statistics.incrementFoundFiles();

		if (hashAttribute.isCorrupted(file)) {
			statistics.incrementFailedFiles();
		} else if (hashAttribute.areAttributesValid(file)) {
			statistics.incrementProcessedFiles();
		}

		return FileVisitResult.CONTINUE;
	}
}
