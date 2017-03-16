/*  Copyright (C) 2017  Nicholas Wright
    
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
package com.github.dozedoff.similarImage.result;

import java.util.Collection;
import java.util.LinkedList;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

/**
 * A list of {@link ResultGroup}s
 */
public class GroupList {
	private final Multimap<Result, ResultGroup> resultsToGroups;
	private final Collection<ResultGroup> groups;

	/**
	 * Create a new {@link GroupList}.
	 */
	public GroupList() {
		groups = new LinkedList<ResultGroup>();
		resultsToGroups = MultimapBuilder.hashKeys().linkedListValues().build();
	}

	/**
	 * Populate this {@link GroupList} with the given {@link ResultGroup}s, also creates a reverse map for
	 * {@link Result} -> {@link ResultGroup}.
	 * 
	 * @param groupsToAdd
	 *            the {@link ResultGroup}s to add
	 */
	public void populateList(ResultGroup... groupsToAdd) {
		clearGroups();

		for (ResultGroup g : groupsToAdd) {
			groups.add(g);
			mapResultsToGroups(g);
		}
	}

	/**
	 * Get the number of groups.
	 * 
	 * @return the number of groups.
	 */
	public int groupCount() {
		return groups.size();
	}

	private void mapResultsToGroups(ResultGroup group) {
		for (Result result : group.getResults()) {
			resultsToGroups.put(result, group);
		}
	}

	private void clearGroups() {
		this.groups.clear();
		this.resultsToGroups.clear();
	}

	/**
	 * Remove a result from all groups. Will not trigger additional notifications to the {@link GroupList}.
	 * If the last {@link Result} was removed from a {@link ResultGroup}, the group itself will be removed.
	 * 
	 * @param result
	 *            to be removed
	 */
	public void remove(Result result) {
		Collection<ResultGroup> groupsToRemoveFrom = resultsToGroups.removeAll(result);

		for (ResultGroup g : groupsToRemoveFrom) {
			g.remove(result, false);
			checkAndremoveEmptyGroup(g);
		}
	}

	private void checkAndremoveEmptyGroup(ResultGroup groupToCheck) {
		if (!groupToCheck.hasResults()) {
			groups.remove(groupToCheck);
		}
	}
}
