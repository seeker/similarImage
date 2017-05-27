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

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.Tag;
import com.github.dozedoff.similarImage.duplicate.DuplicateOperations;
import com.github.dozedoff.similarImage.event.GuiEventBus;
import com.github.dozedoff.similarImage.event.GuiUserTagChangedEvent;
import com.github.dozedoff.similarImage.result.Result;
import com.google.common.eventbus.Subscribe;

/**
 * Context menu operations for the {@link DuplicateEntryView}.
 */
public class OperationsMenu {
	private static final Tag TAG_DNW = new Tag(DuplicateOperations.Tags.DNW.toString());
	private static final Tag TAG_BLOCK = new Tag("BLOCK");

	private static final Logger LOGGER = LoggerFactory.getLogger(OperationsMenu.class);

	private DuplicateOperations duplicateOperations;
	private final UserTagSettingController utsController;
	private JPopupMenu menu;
	private final HashMap<Operations, ActionListener> actions = new HashMap<OperationsMenu.Operations, ActionListener>();

	private enum Operations {
		Delete, MarkAndDeleteDNW, MarkBlocked, Ignore, CopyDirectoryPath
	};

	private final Result result;

	public OperationsMenu(Result result, DuplicateOperations dupOps, UserTagSettingController utsController) {
		this.duplicateOperations = dupOps;
		this.result = result;
		this.menu = new JPopupMenu();
		this.utsController = utsController;

		setupPopupMenu();
		GuiEventBus.getInstance().register(this);
	}

	private Path getPath() {
		return Paths.get(result.getImageRecord().getPath());
	}

	private long getHash() {
		return result.getImageRecord().getpHash();
	}

	private void deleteFile() {
		duplicateOperations.deleteFile(getPath());
		result.remove();
	}

	private void setupPopupMenu() {
		actions.put(Operations.Delete, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				deleteFile();
			}
		});

		actions.put(Operations.MarkAndDeleteDNW, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				duplicateOperations.markAs(getPath(), TAG_DNW);
				deleteFile();
			}
		});

		actions.put(Operations.MarkBlocked, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				duplicateOperations.markAs(getPath(), TAG_BLOCK);
			}
		});

		actions.put(Operations.Ignore, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				duplicateOperations.ignore(getHash());
				result.remove();
			}
		});

		actions.put(Operations.CopyDirectoryPath, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Path directory = getPath().getParent();

				Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
				clpbrd.setContents(new StringSelection(directory.toString()), null);
			}
		});

		createMenuItems(actions);
	}

	private void createUserTags() {
		for (Tag cut : utsController.getAllUserTags()) {
			JMenuItem menuItem = new JMenuItem("Tag " + cut.getTag());
			menuItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					duplicateOperations.markAs(getPath(), cut);
				}
			});

			menu.add(menuItem);
		}
	}

	private void createMenuItems(HashMap<Operations, ActionListener> actions) {
		for (Operations op : Operations.values()) {
			ActionListener listener = actions.get(op);

			if (listener != null) {
				JMenuItem jmi = new JMenuItem(op.toString());
				jmi.addActionListener(listener);
				menu.add(jmi);
			}
		}

		menu.addSeparator();

		createUserTags();
	}

	@Subscribe
	private void reCreateMenu(GuiUserTagChangedEvent event) {
		menu = new JPopupMenu();
		createMenuItems(actions);
		createUserTags();
	}

	public JPopupMenu getMenu() {
		return menu;
	}
}
