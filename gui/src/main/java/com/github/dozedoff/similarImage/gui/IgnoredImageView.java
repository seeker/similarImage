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
package com.github.dozedoff.similarImage.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.github.dozedoff.similarImage.db.IgnoreRecord;

public class IgnoredImageView extends JFrame {
	private static final long serialVersionUID = 3895643911633834599L;
	private static final int SIZE = 500;
	private static final String WINDOW_TITLE = "Ignored images";

	private final IgnoredImagePresenter presenter;
	private JFrame window;
	private JList<IgnoreRecord> ignoredList;

	public IgnoredImageView(IgnoredImagePresenter presenter) {
		super(WINDOW_TITLE);
		this.presenter = presenter;
		presenter.setView(this);

		setupWindow();
		setupList();
		setupControls();

		pack();
	}

	private void setupWindow() {
		window = this;
		window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		window.setLayout(new BorderLayout());
	}

	private void setupList() {
		ignoredList = new JList<IgnoreRecord>(presenter.getModel());
		JScrollPane scroll = new JScrollPane(ignoredList);
		window.add(scroll, BorderLayout.CENTER);
	}

	private void setupControls() {
		JPanel controlPanel = new JPanel();
		JButton refresh = new JButton("Refresh list");
		refresh.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				presenter.refreshList();
			}
		});

		JButton remove = new JButton("Remove");
		remove.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				presenter.removeIgnoredImages(ignoredList.getSelectedValuesList());
				presenter.refreshList();
			}
		});

		controlPanel.add(remove);
		controlPanel.add(refresh);
		this.add(controlPanel, BorderLayout.SOUTH);
	}
}
