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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.management.InvalidAttributeValueException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read and write hash values as extended attributes. Does minimal validation.
 * 
 * @author Nicholas Wright
 *
 */
public class HashAttribute {
	private static final Logger LOGGER = LoggerFactory.getLogger(HashAttribute.class);
	
	private static final long TIMESTAMP_TOLERANCE = 10;
	private static final int HEXADECIMAL_RADIX = 16;

	private final String hashNamespace;
	private static final String HASH_TIMESTAMP_NAME = ExtendedAttribute.SIMILARIMAGE_NAMESPACE + ".hash_timestamp";

	/**
	 * Create a class that can be used to read and write hashes as extended attributes.
	 * 
	 * @param hashName
	 *            name used to identify the hash
	 */
	public HashAttribute(String hashName) {
		hashNamespace = ExtendedAttribute.createName(hashName);
	}

	/**
	 * Checks that the file has a hash and that the timestamp still matches the one recorded with the hash.
	 * 
	 * @param path
	 *            of the file to check
	 * @return true if a hash is found and the timestamp matches
	 */
	public boolean areAttributesValid(Path path) {
		try {
			return ExtendedAttribute.isExtendedAttributeSet(path, hashNamespace) && verifyTimestamp(path);
		} catch (IOException e) {
			LOGGER.error("Failed to check hash for {} ({})", path, e.toString());
		}

		return false;
	}

	private boolean verifyTimestamp(Path path) throws IOException {
		if (!ExtendedAttribute.isExtendedAttributeSet(path, HASH_TIMESTAMP_NAME)) {
			LOGGER.error("{} does not have a timestamp", path);
			return false;
		}

		long fileModifiedTime = Files.getLastModifiedTime(path).toMillis();
		long storedTimestamp = Long.parseUnsignedLong(
				ExtendedAttribute.readExtendedAttributeAsString(path, HASH_TIMESTAMP_NAME));

		if (storedTimestamp > fileModifiedTime + TIMESTAMP_TOLERANCE) {
			LOGGER.warn("The file modification time of {} is newer than the Timestamp", path);
		}

		if (storedTimestamp < fileModifiedTime - TIMESTAMP_TOLERANCE) {
			LOGGER.warn("{} has been modified since the hash was recorded", path);
		}

		return Math.abs(fileModifiedTime - storedTimestamp) <= TIMESTAMP_TOLERANCE;
	}

	/**
	 * Read the hash stored for the file.
	 * 
	 * @param path
	 *            to the file to read from
	 * @return the hash for the file
	 * @throws InvalidAttributeValueException
	 *             if the hash and/or timestamp are not set or invalid
	 * @throws IOException
	 *             if a error occurred while reading the file
	 */
	public long readHash(Path path) throws InvalidAttributeValueException, IOException {
		if (!areAttributesValid(path)) {
			throw new InvalidAttributeValueException("The required attributes are not set or invalid");
		}
		
		String encodedHash = ExtendedAttribute.readExtendedAttributeAsString(path, hashNamespace);
		return Long.parseUnsignedLong(encodedHash, HEXADECIMAL_RADIX);
	}

	/**
	 * Write the hash value and the modified time of the file as extended attributes.
	 * 
	 * @param path
	 *            of the file to write to
	 * @param hash
	 *            associated with the file
	 */
	public void writeHash(Path path, long hash) {
		try {
			ExtendedAttribute.setExtendedAttribute(path, hashNamespace, Long.toHexString(hash));
			ExtendedAttribute.setExtendedAttribute(path, HASH_TIMESTAMP_NAME,
					Long.toString(Files.getLastModifiedTime(path).toMillis()));

		} catch (IOException e) {
			LOGGER.warn("Failed to write hash to file {} ({})", path, e.toString());
		}
	}
}
