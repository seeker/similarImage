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
package com.github.dozedoff.similarImage.gui;

import com.github.dozedoff.similarImage.duplicate.DuplicateOperations;
import com.github.dozedoff.similarImage.result.Result;

/**
 * Factory for building {@link OperationsMenu}s.
 * 
 * @author Nicholas Wright
 *
 */
public class OperationsMenuFactory {
	private final DuplicateOperations duplicateOperations;
	private final UserTagSettingController userTagSettingController;

	/**
	 * Create a new factory that with the instances to use for creating new {@link OperationsMenu}.
	 * 
	 * @param duplicateOperations
	 *            actions that can be performed on the result
	 * @param userTagSettingController
	 *            user tags
	 */
	public OperationsMenuFactory(DuplicateOperations duplicateOperations,
			UserTagSettingController userTagSettingController) {
		this.duplicateOperations = duplicateOperations;
		this.userTagSettingController = userTagSettingController;
	}

	/**
	 * Create a new {@link OperationsMenu} for the given {@link Result}.
	 * 
	 * @param result
	 *            to create a menu for
	 * @return the constructed menu
	 */
	public OperationsMenu createOperationsMenu(Result result) {
		return new OperationsMenu(result, duplicateOperations, userTagSettingController);
	}
}
