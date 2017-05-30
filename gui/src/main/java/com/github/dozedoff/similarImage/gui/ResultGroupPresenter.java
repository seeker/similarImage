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

import com.github.dozedoff.similarImage.result.Result;
import com.github.dozedoff.similarImage.result.ResultGroup;

public class ResultGroupPresenter {
	private ResultGroup resultGroup;
	private OperationsMenuFactory menuFactory;
	private final SimilarImageController siController;

	public ResultGroupPresenter(ResultGroup resultGroup, OperationsMenuFactory menuFactory,
			SimilarImageController siController) {
		this.resultGroup = resultGroup;
		this.menuFactory = menuFactory;
		this.siController = siController;
	}

	public void setView(ResultGroupView view) {
		for (Result result : resultGroup.getResults()) {
			ResultPresenter resultPresenter = new ResultPresenter(result);
			ResultView resultView = new ResultView(resultPresenter, menuFactory.createOperationsMenu(result));
			view.addResultView(resultView);
		}
	}

	public void previousGroup() {
		siController.displayPreviousGroup(resultGroup);
	}

	public void nextGroup() {
		siController.displayNextGroup(resultGroup);
	}
}
