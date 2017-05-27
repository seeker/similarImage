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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

/**
 * A list of {@link ResultGroup}s
 */
public class GroupList {
	private static final Logger LOGGER = LoggerFactory.getLogger(GroupList.class);

	private final Multimap<Result, ResultGroup> resultsToGroups;
	private final Map<Long, ResultGroup> hashToGroup;
	private final Collection<ResultGroup> groups;
	private DefaultListModel<ResultGroup> mappedListeModel;

	/**
	 * Create a new {@link GroupList} with a empty {@link DefaultListModel}.
	 */
	public GroupList() {
		this(new DefaultListModel<ResultGroup>());
	}

	/**
	 * Create a new {@link GroupList}.
	 * 
	 * @param mappedListModel
	 *            the {@link DefaultListModel} this {@link GroupList} was mapped to.
	 */
	public GroupList(DefaultListModel<ResultGroup> mappedListModel) {
		this.mappedListeModel = mappedListModel;
		groups = new LinkedList<ResultGroup>();
		hashToGroup = new HashMap<Long, ResultGroup>();
		resultsToGroups = MultimapBuilder.hashKeys().linkedListValues().build();
	}

	/**
	 * Populate this {@link GroupList} with the given {@link ResultGroup}s, also creates a reverse map for
	 * {@link Result} -> {@link ResultGroup}.
	 * 
	 * @param groupsToAdd
	 *            the {@link ResultGroup}s to add
	 */
	public void populateList(Collection<ResultGroup> groupsToAdd) {
		clearGroups();

		LOGGER.info("Creating group list with {} groups", groupsToAdd.size());

		for (ResultGroup g : groupsToAdd) {
			groups.add(g);
			hashToGroup.put(g.getHash(), g);
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

		LOGGER.debug("Removing {} from {} group(s).", result, groupsToRemoveFrom.size());

		for (ResultGroup g : groupsToRemoveFrom) {
			g.remove(result, false);
			checkAndremoveEmptyGroup(g);
		}
	}

	private void checkAndremoveEmptyGroup(ResultGroup groupToCheck) {
		if (!groupToCheck.hasResults()) {
			LOGGER.debug("Removing  {} because it has no results.", groupToCheck);
			groups.remove(groupToCheck);
			mappedListeModel.removeElement(groupToCheck);
		}
	}

	/**
	 * Get the {@link ResultGroup} for the given hash.
	 * 
	 * @param hash
	 *            for the result group
	 * @return the {@link ResultGroup} for the hash
	 * @throws IllegalArgumentException
	 *             if the hash was not found
	 */
	public ResultGroup getGroup(long hash) throws IllegalArgumentException {
		ResultGroup group = hashToGroup.get(hash);

		if (group == null) {
			throw new IllegalArgumentException("Query for unknown hash");
		}

		return group;
	}

	/**
	 * Get a list of all groups.
	 * 
	 * @return all groups
	 */
	public List<ResultGroup> getAllGroups() {
		return new LinkedList<ResultGroup>(groups);
	}

	/**
	 * Set the {@link DefaultListModel} that maps this {@link GroupList}.
	 * 
	 * @param mappedListeModel
	 *            {@link DefaultListModel} to set
	 */
	public void setMappedListModel(DefaultListModel<ResultGroup> mappedListeModel) {
		this.mappedListeModel = mappedListeModel;
	}
}
