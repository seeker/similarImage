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
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
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

import com.github.dozedoff.similarImage.db.CustomUserTag;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.duplicate.DuplicateOperations;
import com.github.dozedoff.similarImage.event.GuiEventBus;
import com.github.dozedoff.similarImage.event.GuiUserTagChangedEvent;
import com.github.dozedoff.similarImage.io.Statistics.StatisticsEvent;
import com.github.dozedoff.similarImage.io.StatisticsChangedListener;
import com.github.dozedoff.similarImage.thread.GroupListPopulator;
import com.google.common.eventbus.Subscribe;

import net.miginfocom.swing.MigLayout;

public class SimilarImageView implements StatisticsChangedListener {
	private JFrame view;

	private static final int DEFAULT_TEXTFIELD_WIDTH = 20;

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

	private final UserTagSettingController utsController;
	final DuplicateOperations duplicateOperations;

	public SimilarImageView(SimilarImageController controller, DuplicateOperations duplicateOperations,
			int maxBufferSize, UserTagSettingController utsController) {
		this.controller = controller;
		this.utsController = utsController;

		view = new JFrame();

		view.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		view.setSize(500, 500);
		view.setTitle("Similar Image");
		view.setLayout(new MigLayout("wrap 4"));
		this.duplicateOperations = duplicateOperations;

		this.progress = new JProgressBar(0, 0);
		this.progress.setStringPainted(true);

		this.bufferLevel = new JProgressBar(0, maxBufferSize);
		this.bufferLevel.setStringPainted(true);

		setupComponents();
		setupMenu();
		updateHammingDisplay();
		view.setVisible(true);

		GuiEventBus.getInstance().register(this);
	}

	public void setStatus(String statusMsg) {
		if (status != null) {
			status.setText(statusMsg);
		}
	}

	private JComponent buildActiveTagsList(JTextField tagField) {
		JList<String> activeTags = new JList<String>(duplicateOperations.getFilterTags().toArray(new String[0]));
		activeTags.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				String selectedValue = activeTags.getSelectedValue();
				if (selectedValue != null) {
					tagField.setText(selectedValue);
				}
			}
		});

		return new JScrollPane(activeTags);
	}

	private void setupComponents() {
		path = new JTextField(20);
		find = new JButton("Find");
		stop = new JButton("Stop");
		status = new JLabel("Idle");
		sortSimilar = new JButton("Sort similar");
		sortFilter = new JButton("Sort filter");

		groupListModel = new DefaultListModel<Long>();
		groups = new JList<Long>(groupListModel);
		groups.setComponentPopupMenu(new OperationsMenu(utsController));
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
				JTextField tag = new JTextField(DEFAULT_TEXTFIELD_WIDTH);
				tag.setToolTipText("Limit search to Tag. Empty tag or * will select ALL tags");

				Object[] message = { "Tag: ", tag, "Active Tags:", buildActiveTagsList(tag) };
				JOptionPane pane = new JOptionPane(message, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
				JDialog getTopicDialog = pane.createDialog(null, "Select Tag to use in search");
				getTopicDialog.setVisible(true);

				if (pane.getValue() != null && (Integer) pane.getValue() == JOptionPane.OK_OPTION) {
					String r = tag.getText();
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

		JMenuItem directoryTag;
		JMenuItem pruneRecords;
		JMenuItem userTags;


		directoryTag = new JMenuItem("Tag directory");
		directoryTag.setToolTipText("Tag all images in a directory");

		pruneRecords = new JMenuItem("Prune records");

		JMenuItem about = new JMenuItem("About");

		directoryTag.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				JTextField directoryField = new JTextField(DEFAULT_TEXTFIELD_WIDTH);
				JTextField tagField = new JTextField(DEFAULT_TEXTFIELD_WIDTH);

				Object[] message = { "Directory: ", directoryField, "Tag:", tagField, "Active Tags:",
						buildActiveTagsList(tagField) };

				JOptionPane pane = new JOptionPane(message, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
				JDialog getTopicDialog = pane.createDialog(null, "Tag all images in directory");
				getTopicDialog.setVisible(true);

				if (pane.getValue() != null && (Integer) pane.getValue() == JOptionPane.OK_OPTION) {
					Path selectedPath = Paths.get(directoryField.getText());
					String tag = tagField.getText();
					duplicateOperations.markDirectoryAs(selectedPath, tag);
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

		userTags = new JMenuItem("User Tags");
		userTags.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new UserTagSettingView(utsController);
			}
		});

		JMenu file = new JMenu("File");
		file.add(directoryTag);
		file.add(pruneRecords);

		JMenu help = new JMenu("Help");
		help.add(about);

		JMenu settings = new JMenu("Settings");
		settings.add(userTags);

		JMenuBar menuBar = new JMenuBar();
		menuBar.add(file);
		menuBar.add(settings);
		menuBar.add(help);
		view.setJMenuBar(menuBar);
	}

	private void updateHammingDisplay() {
		hammingValue.setText("" + hammingDistance.getValue());
	}

	public void setTotalFiles(int numOfFiles) {
		progress.setMaximum(numOfFiles);
	}

	public void populateGroupList(Collection<Long> groups) {
		SwingUtilities.invokeLater(new GroupListPopulator(groups, groupListModel));
	}

	private void deleteAll(long group) {
		Set<ImageRecord> set = controller.getGroup(group);
		duplicateOperations.deleteAll(set);
		groupListModel.removeElement(group);
	}

	private void tagAll(long group) {
		JTextField tagField = new JTextField(DEFAULT_TEXTFIELD_WIDTH);

		Object[] message = { "Tag:", tagField, "Active Tags:", buildActiveTagsList(tagField) };

		JOptionPane pane = new JOptionPane(message, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
		JDialog getTopicDialog = pane.createDialog(null, "Tag all images");
		getTopicDialog.setVisible(true);

		if (pane.getValue() != null && (Integer) pane.getValue() == JOptionPane.OK_OPTION) {
			duplicateOperations.markAll(controller.getGroup(group), tagField.getText());
		}
	}

	public boolean okToDisplayLargeGroup(int groupSize) {
		Object[] message = { "Group size is " + groupSize + "\nContinue loading?" };
		JOptionPane pane = new JOptionPane(message, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
		JDialog getTopicDialog = pane.createDialog(null, "Continue?");
		getTopicDialog.setVisible(true);

		return (pane.getValue() != null && (Integer) pane.getValue() == JOptionPane.OK_OPTION);
	}

	/**
	 * Rebuild menu with new user tags.
	 */
	@Subscribe
	private void updateMenuOnUserTagChange(GuiUserTagChangedEvent event) {
		groups.setComponentPopupMenu(new OperationsMenu(utsController));
	}

	/**
	 * Operations for the group window context menu
	 * 
	 * @author Nicholas Wright
	 *
	 */
	class OperationsMenu extends JPopupMenu {
		private static final long serialVersionUID = 1L;

		public OperationsMenu(UserTagSettingController utsController) {
			JMenuItem deleteAll = new JMenuItem("Delete all");
			deleteAll.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					deleteAll(getSelectedGroup());
				}
			});

			JMenuItem dnwAll = new JMenuItem("Tag all");
			dnwAll.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					tagAll(getSelectedGroup());
				}
			});

			this.add(deleteAll);
			this.add(dnwAll);

			addUserTags(utsController);
		}

		/**
		 * Create mark menu items for all user tags
		 * 
		 * @param tagController
		 *            {@link UserTagSettingController} to use
		 */
		private void addUserTags(UserTagSettingController tagController) {
			for (CustomUserTag tag : tagController.getAllUserTags()) {
				JMenuItem menu = new JMenuItem("Tag " + tag.toString());
				menu.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						duplicateOperations.markAll(controller.getGroup(getSelectedGroup()), tag.toString());
					}
				});

				this.add(menu);
			}
		}
	}

	@Override
	public void statisticsChangedEvent(StatisticsEvent event, int newValue) {
		switch (event) {
		case FOUND_FILES:
			progress.setMaximum(newValue);
			break;

		case PROCESSED_FILES:
			progress.setValue(newValue);
			break;

		default:
			break;
		}

	}
}
