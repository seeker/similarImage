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
package com.github.dozedoff.similarImage.util;

public class StringUtil {
	public static final String MATCH_ALL_TAGS = "*";

	private StringUtil() {
	}

	/**
	 * Checks if the provided tag is valid, if not it will be replaced with the "query all" tag.
	 * 
	 * @param tagFromGui
	 *            the tag to search for
	 * @return a sanitized and correct tag
	 */
	public static String sanitizeTag(String tagFromGui) {
		if (tagFromGui == null || "".equals(tagFromGui)) {
			return MATCH_ALL_TAGS;
		}

		return tagFromGui;
	}
}
