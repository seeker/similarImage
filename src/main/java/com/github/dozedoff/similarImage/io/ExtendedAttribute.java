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
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.UserDefinedFileAttributeView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used for writing and reading extended attributes to files.
 * 
 * @author Nicholas Wright
 *
 */
public class ExtendedAttribute {
	private static final Logger LOGGER = LoggerFactory.getLogger(ExtendedAttribute.class);

	private static final String XATTR_TEST_NAME = "user.similarimage.test";

	private ExtendedAttribute(){
	}

	/**
	 * Checks if the given path supports extended attributes. This can be used to check extended attributes on file
	 * systems.
	 * 
	 * @param path
	 *            to check for extended attribute support
	 * @return true if extended attributes are supported and there were no errors
	 */
	public static boolean supportsExtendedAttributes(Path path) {
		try {
			setExtendedAttribute(path, XATTR_TEST_NAME, Charset.defaultCharset().encode("xattr support test"));
			return true;
		} catch (IOException e) {
			LOGGER.error("Failed to write test attribute", e);
		} finally {
			try {
				deleteExtendedAttribute(path, XATTR_TEST_NAME);
			} catch (IOException e) {
				LOGGER.error("Failed to delete test attribute", e);
			}
		}

		return false;
	}

	private static UserDefinedFileAttributeView createUserDefinedFileAttributeView(Path path) {
		return Files.getFileAttributeView(path, UserDefinedFileAttributeView.class);
	}

	/**
	 * Delete the given name and value from the file.
	 * 
	 * @param path
	 *            to file to delete the attribute from
	 * @param name
	 *            to delete
	 * @throws IOException
	 *             if there is an error deleting the attribute
	 */
	public static void deleteExtendedAttribute(Path path, String name) throws IOException {
		UserDefinedFileAttributeView view = Files.getFileAttributeView(path, UserDefinedFileAttributeView.class);
	
		view.delete(name);
	}

	/**
	 * Write the String as an attribute in <b>ASCII</b> encoding.
	 * 
	 * @param path
	 *            file to write attribute to
	 * @param name
	 *            to write the value to
	 * @param value
	 *            to write as ASCII encoded
	 * @throws IOException
	 *             if there is an error writing the attribute
	 */
	public static void setExtendedAttribute(Path path, String name, String value) throws IOException {
		setExtendedAttribute(path, name, StandardCharsets.US_ASCII.encode(value));
	}

	/**
	 * Write the ByteBuffer as an attribute.
	 * 
	 * @param path
	 *            file to write attribute to
	 * @param name
	 *            to write the value to
	 * @param value
	 *            to write as ASCII encoded
	 * @throws IOException
	 *             if there is an error writing the attribute
	 */
	public static void setExtendedAttribute(Path path, String name, ByteBuffer value) throws IOException {
		createUserDefinedFileAttributeView(path).write(name, value);
	}

	/**
	 * Read and return the extended attribute from the file.
	 * 
	 * @param path
	 *            file to read attribute from
	 * @param name
	 *            to read
	 * @return the read value
	 * @throws IOException
	 *             if there is an error reading the attribute
	 */
	public static ByteBuffer readExtendedAttribute(Path path, String name) throws IOException {
		UserDefinedFileAttributeView view = createUserDefinedFileAttributeView(path);
		ByteBuffer buffer = ByteBuffer.allocate(view.size(name));

		createUserDefinedFileAttributeView(path).read(name, buffer);
		buffer.flip();

		return buffer;
	}

	/**
	 * Read the extended attribute as an ASCII encoded string.
	 * 
	 * @param path
	 *            file to read attribute from
	 * @param name
	 *            of extended attribute to read
	 * @return the read value
	 * @throws IOException
	 *             if there is an error reading the attribute
	 */
	public static String readExtendedAttributeAsString(Path path, String name) throws IOException {
		ByteBuffer buffer = readExtendedAttribute(path, name);

		return StandardCharsets.US_ASCII.decode(buffer).toString();
	}

	/**
	 * Checks if the file has an extended attribute with the given name
	 * 
	 * @param path
	 *            to file with attribute
	 * @param name
	 *            of the attribute
	 * @return true if the attribute is set
	 * @throws IOException
	 *             if there is an error accessing the file
	 */
	public static boolean isExtendedAttributeSet(Path path, String name) throws IOException {
		return createUserDefinedFileAttributeView(path).list().contains(name);
	}
}
