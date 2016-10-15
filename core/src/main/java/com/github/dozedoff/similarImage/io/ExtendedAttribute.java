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
public class ExtendedAttribute implements ExtendedAttributeQuery {
	private static final Logger LOGGER = LoggerFactory.getLogger(ExtendedAttribute.class);

	public static final String SIMILARIMAGE_NAMESPACE = "user.similarimage";
	private static final String XATTR_TEST_NAME = SIMILARIMAGE_NAMESPACE + "test";

	/**
	 * Create a name for an extended attribute with {@link ExtendedAttribute#SIMILARIMAGE_NAMESPACE} as the prefix. The
	 * values provided in names are separated with a '.'.
	 * 
	 * @param names
	 *            elements to use for namespaces and name
	 * @return the full name for the attribute
	 */
	public static String createName(String... names) {
		StringBuilder sb = new StringBuilder(SIMILARIMAGE_NAMESPACE);

		for (String name : names) {
			sb.append(".");
			sb.append(name);
		}

		return sb.toString();
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
			return Files.getFileStore(path).supportsFileAttributeView(UserDefinedFileAttributeView.class);
		} catch (IOException e) {
			LOGGER.warn("Failed to check extended attributes via FileStore ({}) for {}, falling back to write test...",
					e.toString(), path);
			return checkSupportWithWrite(path);
		}
	}

	private static boolean checkSupportWithWrite(Path path) {
		try {
			setExtendedAttribute(path, XATTR_TEST_NAME, Charset.defaultCharset().encode("xattr support test"));
			return true;
		} catch (IOException e) {
			LOGGER.debug("Failed to write test attribute ({})", e.toString());
		} finally {
			try {
				deleteExtendedAttribute(path, XATTR_TEST_NAME);
			} catch (IOException e) {
				LOGGER.debug("Failed to delete test attribute ({})", e.toString());
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isEaSupported(Path path) {
		return supportsExtendedAttributes(path);
	}
}
