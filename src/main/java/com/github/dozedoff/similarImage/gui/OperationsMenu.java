/*  Copyright (C) 2013  Nicholas Wright
    
    This file is part of similarImage - A similar image finder using pHash
    
    mmut is free software: you can redistribute it and/or modify
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
import java.nio.file.Path;
import java.util.HashMap;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.github.dozedoff.similarImage.duplicate.DuplicateEntry;
import com.github.dozedoff.similarImage.duplicate.DuplicateOperations;
import com.github.dozedoff.similarImage.duplicate.ImageInfo;

public class OperationsMenu extends JPopupMenu {
	private static final long serialVersionUID = 1L;

	private enum Operations {
		Delete, MarkAndDeleteDNW, MarkBlocked, Ignore
	};

	private final DuplicateEntry parent;

	public OperationsMenu(DuplicateEntry parent) {
		super();
		this.parent = parent;
		setupPopupMenu();
	}

	private void setupPopupMenu() {
		HashMap<Operations, ActionListener> actions = new HashMap<OperationsMenu.Operations, ActionListener>();

		actions.put(Operations.Delete, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Path path = getPath();
				DuplicateOperations.deleteFile(path);
			}
		});

		actions.put(Operations.MarkAndDeleteDNW, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Path path = getPath();
				DuplicateOperations.markAsDnw(path);
				DuplicateOperations.deleteFile(path);
			}
		});

		actions.put(Operations.MarkBlocked, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Path path = getPath();
				DuplicateOperations.markAs(path, "BLOCK");
			}
		});

		actions.put(Operations.Ignore, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				parent.ignore();
			}
		});

		createMenuItems(actions);
		parent.setComponentPopupMenu(this);
	}

	private Path getPath() {
		ImageInfo ii = parent.getImageInfo();
		Path path = ii.getPath();
		return path;
	}

	private void createMenuItems(HashMap<Operations, ActionListener> actions) {
		for (Operations op : Operations.values()) {
			ActionListener listener = actions.get(op);

			if (listener != null) {
				JMenuItem jmi = new JMenuItem(op.toString());
				jmi.addActionListener(listener);
				this.add(jmi);
			}
		}
	}
}
