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
package com.github.dozedoff.similarImage.event;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.google.common.collect.Multimap;

/**
 * Event that indicates a change in the group information.
 * 
 * @author Nicholas Wright
 *
 */
public class GuiGroupEvent {
	private final Multimap<Long, ImageRecord> groups;

	/**
	 * Create a new event with updated group information.
	 * 
	 * @param groups
	 *            the new group information
	 */
	public GuiGroupEvent(Multimap<Long, ImageRecord> groups) {
		this.groups = groups;
	}

	/**
	 * Get the group information from this event.
	 * 
	 * @return the group information
	 */
	public final Multimap<Long, ImageRecord> getGroups() {
		return groups;
	}
}
