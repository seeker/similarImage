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
package com.github.dozedoff.similarImage.thread;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.DefaultListModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.result.GroupList;
import com.github.dozedoff.similarImage.result.ResultGroup;

public class GroupListPopulator implements Runnable {
	Logger logger = LoggerFactory.getLogger(GroupListPopulator.class);
	private GroupList groups;
	private DefaultListModel<ResultGroup> groupListModel;

	public GroupListPopulator(GroupList groups, DefaultListModel<ResultGroup> groupListModel) {
		this.groups = groups;
		this.groupListModel = groupListModel;
	}

	@Override
	public void run() {
		this.logger.info("Populating group list with {} groups", groups.groupCount());
		groupListModel.clear();

		List<ResultGroup> resultGroups = groups.getAllGroups();
		Collections.sort(resultGroups, new Comparator<ResultGroup>() {
			@Override
			public int compare(ResultGroup o1, ResultGroup o2) {
				return Long.compare(o1.getHash(), o2.getHash());
			}
		});

		for (ResultGroup g : resultGroups) {
			groupListModel.addElement(g);
		}

		this.logger.info("Finished populating group list");
	}
}
