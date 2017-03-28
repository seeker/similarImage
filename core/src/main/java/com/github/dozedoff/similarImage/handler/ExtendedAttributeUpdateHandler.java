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
package com.github.dozedoff.similarImage.handler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.commonj.hash.ImagePHash;
import com.github.dozedoff.similarImage.io.HashAttribute;

/**
 * Reads extended attributes from files. If the data is missing or invalid, the hash will be calculated and stored.
 * 
 * @author Nicholas Wright
 *
 */
public class ExtendedAttributeUpdateHandler implements HashHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ExtendedAttributeUpdateHandler.class);

	private final HashAttribute hashAttribute;
	private final ImagePHash hasher;

	/**
	 * Create a instance that can read an write extended attributes, as well as calculate hashes using the provided
	 * classes.
	 * 
	 * @param hashAttribute
	 *            used to read and write extended attributes
	 * @param hasher
	 *            used to calculate file hashes
	 */
	public ExtendedAttributeUpdateHandler(HashAttribute hashAttribute, ImagePHash hasher) {
		this.hashAttribute = hashAttribute;
		this.hasher = hasher;
	}

	/**
	 * Read the extended attributes from the file and update them if needed.
	 * 
	 * @param file
	 *            to check
	 * @return true if this handler could handle the file
	 */
	@Override
	public boolean handle(Path file) {
		if (!hashAttribute.areAttributesValid(file)) {
			try {
				long hash = hasher.getLongHash(Files.newInputStream(file));
				hashAttribute.writeHash(file, hash);
			} catch (IOException e) {
				LOGGER.warn("Failed to hash {}, {}", file, e.toString());
				return false;
			}
		}

		return true;
	}
}
