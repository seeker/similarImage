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

import java.nio.file.Path;

/**
 * Interface for handlers that can provide hashes for files.
 * 
 * @author Nicholas Wright
 *
 */
public interface HashHandler {
	/**
	 * Acquire a hash for the given file and make it available to the porgramm.
	 * 
	 * @param file
	 *            for which a hash is required
	 * @return true if the handler could successfully provide a hash
	 */
	boolean handle(Path file);
}
