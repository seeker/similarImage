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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import uk.co.timwise.wraplayout.WrapLayout;

/**
 * Display {@link View}s in a wrapped layout.
 * 
 * @author Nicholas Wright
 *
 */
public class ResultGroupView implements View {
	private JPanel content;
	private JComponent view;
	private final ResultGroupPresenter presenter;

	/**
	 * Setup for displaying duplicate images.
	 */
	public ResultGroupView(ResultGroupPresenter presenter) {
		this.content = new JPanel(new WrapLayout(WrapLayout.LEFT));
		this.view = new JPanel(new BorderLayout());
		this.presenter = presenter;

		view.add(new JScrollPane(content), BorderLayout.CENTER);
		addNavigationButtons();
		presenter.setView(this);

	}

	private void addNavigationButtons() {
		JPanel buttonPanel = new JPanel();

		JButton next = new JButton("Next");
		JButton previous = new JButton("Previous");

		next.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				presenter.nextGroup();
			}
		});

		previous.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				presenter.previousGroup();
			}
		});

		buttonPanel.add(previous);
		buttonPanel.add(next);

		view.add(buttonPanel, BorderLayout.NORTH);
	}

	public void addResultView(ResultView resultView) {
		content.add(resultView.getView());
	}

	@Override
	public JComponent getView() {
		return view;
	}
}
