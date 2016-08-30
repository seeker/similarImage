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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import com.github.dozedoff.similarImage.db.CustomUserTag;

import net.miginfocom.swing.MigLayout;

public class UserTagSettingView {
	private final UserTagSettingController controller;
	private final SimilarImageView parent;

	private JFrame mainWindow;

	/**
	 * Create a view using the given controller.
	 * 
	 * @param controller
	 *            to use
	 */
	public UserTagSettingView(UserTagSettingController controller, SimilarImageView parent) {
		this.controller = controller;
		this.parent = parent;

		setup();
	}

	private void updateParentMenu() {
		parent.updateMenuOnUserTagChange();
	}

	private void setup() {
		mainWindow = new JFrame("User Tags");
		mainWindow.setSize(500, 500);
		mainWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		mainWindow.setLayout(new MigLayout("wrap 2"));
		
		JTextField tagField = new JTextField();
		JButton addTag = new JButton("Add");
		JButton removeTag = new JButton("Remove");


		DefaultListModel<CustomUserTag> tagModel = new DefaultListModel<CustomUserTag>();

		for (CustomUserTag tag : controller.getAllUserTags()) {
			tagModel.addElement(tag);
		}

		JList<CustomUserTag> tags = new JList<CustomUserTag>(tagModel);

		addTag.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				tagModel.addElement(controller.addTag(tagField.getText()));
				updateParentMenu();
			}
		});
		
		removeTag.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				CustomUserTag selectedTag = tags.getSelectedValue();

				if (selectedTag != null) {
					controller.removeTag(selectedTag);
					tagModel.removeElement(selectedTag);
					updateParentMenu();
				}
			}
		});

		mainWindow.add(new JScrollPane(tags), "span 2, growx");
		mainWindow.add(tagField, "span 2, growx");
		mainWindow.add(addTag);
		mainWindow.add(removeTag);

		mainWindow.pack();
		mainWindow.setVisible(true);
	}
}
