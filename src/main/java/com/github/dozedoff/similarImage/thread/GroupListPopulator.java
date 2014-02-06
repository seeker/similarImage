/*  Copyright (C) 2014  Nicholas Wright
    
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
import java.util.List;

import javax.swing.DefaultListModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupListPopulator implements Runnable {
	Logger logger = LoggerFactory.getLogger(GroupListPopulator.class);
	private List<Long> groups;
	private DefaultListModel<Long> groupListModel;

	public GroupListPopulator(List<Long> groups, DefaultListModel<Long> groupListModel) {
		this.groups = groups;
		this.groupListModel = groupListModel;
	}

	@Override
	public void run() {
		this.logger.info("Populating group list with {} groups", groups.size());
		groupListModel.clear();

		Collections.sort(groups);

		for (Long g : groups) {
			groupListModel.addElement(g);
		}

		this.logger.info("Finished populating group list");
	}
}
