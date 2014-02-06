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
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Persistence;
import com.github.dozedoff.similarImage.duplicate.DuplicateOperations;
import com.github.dozedoff.similarImage.thread.GroupListPopulator;

public class SimilarImageView {
	private JFrame view;

	private final SimilarImageController controller;

	private JTextField path;
	private JButton find, stop, sortSimilar, sortFilter;
	private JLabel status, hammingValue;
	private JProgressBar progress;
	private JProgressBar bufferLevel;
	private JList<Long> groups;
	private DefaultListModel<Long> groupListModel;
	private JScrollPane groupScrollPane;
	private JScrollBar hammingDistance;
	final DuplicateOperations duplicateOperations;

	public SimilarImageView(SimilarImageController controller, Persistence persistence) {
		this.controller = controller;

		view = new JFrame();

		view.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		view.setSize(500, 500);
		view.setTitle("Similar Image");
		view.setLayout(new MigLayout("wrap 4"));
		duplicateOperations = new DuplicateOperations(persistence);
		setupComponents();
		setupMenu();
		updateHammingDisplay();
		view.setVisible(true);
	}

	public void setStatus(String statusMsg) {
		if (status != null) {
			status.setText(statusMsg);
		}
	}

	private void setupComponents() {
		path = new JTextField(20);
		find = new JButton("Find");
		stop = new JButton("Stop");
		status = new JLabel("Idle");
		progress = controller.getTotalProgress();
		sortSimilar = new JButton("Sort similar");
		sortFilter = new JButton("Sort filter");
		bufferLevel = controller.getBufferLevel();

		groupListModel = new DefaultListModel<Long>();
		groups = new JList<Long>(groupListModel);
		groups.setComponentPopupMenu(new OperationsMenu());
		groupScrollPane = new JScrollPane(groups);
		hammingDistance = new JScrollBar(JScrollBar.HORIZONTAL, 0, 2, 0, 64);
		hammingValue = new JLabel();

		find.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String userpath = path.getText();
				controller.indexImages(userpath);
			}
		});

		stop.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				controller.stopWorkers();
			}
		});

		sortSimilar.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				controller.sortDuplicates(hammingDistance.getValue(), path.getText());
			}
		});

		sortFilter.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				// TODO display option dialog to select reason - null or empty
				// mean all
				JTextField reason = new JTextField(20);
				Object[] message = { "Reason: ", reason };
				JOptionPane pane = new JOptionPane(message, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
				JDialog getTopicDialog = pane.createDialog(null, "Select directory");
				getTopicDialog.setVisible(true);

				if (pane.getValue() != null && (Integer) pane.getValue() == JOptionPane.OK_OPTION) {
					String r = reason.getText();
					controller.sortFilter(hammingDistance.getValue(), r);
				}
			}
		});

		groups.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent event) {
				if (event.getValueIsAdjusting()) {
					return; // Still adjusting, do nothing...
				}

				int index = groups.getSelectedIndex();
				if (index > -1 && index < groupListModel.size()) {
					long group = groupListModel.get(index);
					controller.displayGroup(group);
				}

			}
		});

		hammingDistance.addAdjustmentListener(new AdjustmentListener() {
			@Override
			public void adjustmentValueChanged(AdjustmentEvent event) {
				if (!event.getValueIsAdjusting()) {
					updateHammingDisplay();
				}
			}
		});

		view.add(path);
		view.add(find);
		view.add(stop);
		view.add(status);
		view.add(progress);
		view.add(sortSimilar, "wrap");
		view.add(bufferLevel);
		view.add(sortFilter, "wrap");
		view.add(groupScrollPane, "growy");
		view.add(hammingDistance, "growx");
		view.add(hammingValue);
	}

	private long getSelectedGroup() {
		return groups.getSelectedValue();
	}

	private void setupMenu() {
		JMenuBar menuBar = new JMenuBar();
		JMenu file, help;
		JMenuItem folderDnw, folderBlock, pruneRecords, about;

		file = new JMenu("File");
		help = new JMenu("Help");

		folderDnw = new JMenuItem("Add folder as dnw");
		folderBlock = new JMenuItem("Add folder as block");
		pruneRecords = new JMenuItem("Prune records");

		about = new JMenuItem("About");

		folderDnw.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				JTextField directory = new JTextField(20);
				Object[] message = { "Directory: ", directory };
				JOptionPane pane = new JOptionPane(message, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
				JDialog getTopicDialog = pane.createDialog(null, "Select directory");
				getTopicDialog.setVisible(true);

				if (pane.getValue() != null && (Integer) pane.getValue() == JOptionPane.OK_OPTION) {
					Path path = Paths.get(directory.getText());
					duplicateOperations.markDirectoryAs(path, DuplicateOperations.Tags.DNW.toString());
				}
			}
		});

		folderBlock.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				JTextField directory = new JTextField(20);
				Object[] message = { "Directory: ", directory };
				JOptionPane pane = new JOptionPane(message, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
				JDialog getTopicDialog = pane.createDialog(null, "Select directory");
				getTopicDialog.setVisible(true);

				if (pane.getValue() != null && (Integer) pane.getValue() == JOptionPane.OK_OPTION) {
					Path path = Paths.get(directory.getText());
					duplicateOperations.markDirectoryAs(path, "BLOCK");
				}
			}
		});

		pruneRecords.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				duplicateOperations.pruneRecords(Paths.get(path.getText()));
			}
		});

		about.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String version = this.getClass().getPackage().getImplementationVersion();

				if (version == null) {
					version = "unknown";
				}

				JOptionPane.showMessageDialog(null, "SimilarImage Version: " + version, "About", JOptionPane.PLAIN_MESSAGE);
			}
		});

		file.add(folderDnw);
		file.add(folderBlock);
		file.add(pruneRecords);

		help.add(about);

		menuBar.add(file);
		menuBar.add(help);
		view.setJMenuBar(menuBar);
	}

	private void updateHammingDisplay() {
		hammingValue.setText("" + hammingDistance.getValue());
	}

	public void setTotalFiles(int numOfFiles) {
		progress.setMaximum(numOfFiles);
	}

	public void populateGroupList(List<Long> groups) {
		SwingUtilities.invokeLater(new GroupListPopulator(groups, groupListModel));
	}

	private void deleteAll(long group) {
		Set<ImageRecord> set = controller.getGroup(group);
		duplicateOperations.deleteAll(set);
		groupListModel.removeElement(group);
	}

	private void dnwAll(long group) {
		Set<ImageRecord> set = controller.getGroup(group);
		duplicateOperations.markDnwAndDelete(set);
		groupListModel.removeElement(group);
	}

	class OperationsMenu extends JPopupMenu {
		private static final long serialVersionUID = 1L;

		public OperationsMenu() {
			JMenuItem deleteAll = new JMenuItem("Delete all");
			deleteAll.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					deleteAll(getSelectedGroup());
				}

			});

			JMenuItem dnwAll = new JMenuItem("Mark dnw & delete all");
			dnwAll.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					dnwAll(getSelectedGroup());
				}
			});

			this.add(deleteAll);
			this.add(dnwAll);
		}
	}
}
