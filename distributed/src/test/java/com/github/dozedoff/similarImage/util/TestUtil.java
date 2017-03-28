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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Predicate;

//TODO duplicate class, move shared testing code to own module
public class TestUtil {
	private TestUtil() {
	}

	/**
	 * Delete all files, directories are not removed.
	 * 
	 * @param directory
	 *            containing files to delete
	 * @throws IOException
	 *             if there is an error during filesystem access
	 */
	public static void deleteAllFiles(Path directory) throws IOException {
		Files.walk(directory).filter(new Predicate<Path>() {
			@Override
			public boolean test(Path t) {
				return Files.isRegularFile(t, LinkOption.NOFOLLOW_LINKS);
			}
		}).forEach(new Consumer<Path>() {
			@Override
			public void accept(Path t) {
				try {
					Files.deleteIfExists(t);
				} catch (IOException e) {
					// we don't care
				}
			}
		});
	}
}
