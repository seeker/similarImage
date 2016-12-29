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
package com.github.dozedoff.similarImage.messaging;

import org.apache.activemq.artemis.api.core.client.ClientRequestor;

public class ArtemisQueue {
	public enum QueueAddress {
		/**
		 * Resized images are sent here for hashing
		 */
		HASH_REQUEST,
		/**
		 * Hashing results or corrupt files are reported here
		 */
		RESULT,
		/**
		 * Full sized images are sent here for resizing
		 */
		RESIZE_REQUEST,
		/**
		 * Used for repository queries via {@link ClientRequestor}
		 */
		REPOSITORY_QUERY,
		/**
		 * Update messages for extended attributes
		 */
		EA_UPDATE
	}
}
