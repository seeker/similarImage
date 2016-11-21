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
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Path;

import com.github.dozedoff.commonj.filefilter.FileExtensionFilter;

/**
 * Filter class for filtering images based on the file extension;
 */
public class ImageFileFilter implements Filter<Path> {
	private static final String[] SUPPORTED_IMAGE_EXTENSIONS = { "jpg", "jpeg", "png", "gif" };
	private FileExtensionFilter extFilter;

	/**
	 * Create a filter with the following extensions: {@link #SUPPORTED_IMAGE_EXTENSIONS}.
	 * 
	 */
	public ImageFileFilter() {
		this.extFilter = new FileExtensionFilter(SUPPORTED_IMAGE_EXTENSIONS);
	}

	/**
	 * Accept the path if it ends with one of the supported file extensions.
	 * 
	 * @param entry
	 *            path to check
	 * @return true if the file is a supported image
	 * @throws IOException
	 *             if there is an error accessing the file
	 */
	@Override
	public boolean accept(Path entry) throws IOException {
		return extFilter.accept(entry);
	}
}
