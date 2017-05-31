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
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.inject.Inject;
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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.component.ApplicationScope;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Tag;
import com.github.dozedoff.similarImage.db.repository.FilterRepository;
import com.github.dozedoff.similarImage.duplicate.DuplicateOperations;
import com.github.dozedoff.similarImage.event.GuiEventBus;
import com.github.dozedoff.similarImage.event.GuiUserTagChangedEvent;
import com.github.dozedoff.similarImage.io.Statistics.StatisticsEvent;
import com.github.dozedoff.similarImage.io.StatisticsChangedListener;
import com.github.dozedoff.similarImage.result.ResultGroup;
import com.google.common.eventbus.Subscribe;

import net.miginfocom.swing.MigLayout;

@ApplicationScope
public class SimilarImageView implements StatisticsChangedListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(SimilarImageView.class);

	private JFrame view;

	private static final int DEFAULT_TEXTFIELD_WIDTH = 20;
	private static final String QUEUE_LABEL = "Queue size: ";
	private final SimilarImageController controller;

	private JTextField path;
	private JButton find, stop, sortSimilar, sortFilter;
	private JLabel status, hammingValue;
	private JProgressBar progress;
	private JLabel queueSize;
	private JList<ResultGroup> groups;
	private DefaultListModel<ResultGroup> groupListModel;
	private JScrollPane groupScrollPane;
	private JScrollBar hammingDistance;

	private final UserTagSettingController utsController;
	final DuplicateOperations duplicateOperations;

	private final FilterRepository filterRepository;
	private final JFrame resultGroupWindow;


	/**
	 * @deprecated Use constructor with repositories
	 */
	@Deprecated
	public SimilarImageView(SimilarImageController controller, DuplicateOperations duplicateOperations,
			int maxBufferSize, UserTagSettingController utsController) {
		this(controller, duplicateOperations, maxBufferSize, utsController, null);
	}

	@Inject
	public SimilarImageView(SimilarImageController controller, DuplicateOperations duplicateOperations,
			UserTagSettingController utsController, FilterRepository filterRepository) {
		this(controller, duplicateOperations, 0, utsController, filterRepository);

	}

	private void setupResultGroupWindow() {
		resultGroupWindow.setSize(500, 500);
		resultGroupWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		resultGroupWindow.setFocusableWindowState(true);
	}

	public SimilarImageView(SimilarImageController controller, DuplicateOperations duplicateOperations, int maxBufferSize,
			UserTagSettingController utsController, FilterRepository filterRepository) {
		this.controller = controller;
		this.utsController = utsController;
		this.filterRepository = filterRepository;
		view = new JFrame();
		this.resultGroupWindow = new JFrame();
		setupResultGroupWindow();

		view.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		view.setSize(500, 500);
		view.setTitle("Similar Image");
		view.setLayout(new MigLayout("wrap 4"));
		this.duplicateOperations = duplicateOperations;

		this.progress = new JProgressBar(0, 0);
		this.progress.setStringPainted(true);

		this.queueSize = new JLabel(QUEUE_LABEL + 0);

		setupComponents();
		setupMenu();
		updateHammingDisplay();
		view.setVisible(true);

		GuiEventBus.getInstance().register(this);

		updateProgress();
		this.controller.setGui(this);
	}

	/**
	 * Set the model for the result used to display a list.
	 * 
	 * @param model
	 *            to use
	 */
	public void setListModel(DefaultListModel<ResultGroup> model) {
		groupListModel = model;
		groups.setModel(model);
	}

	public void setStatus(String statusMsg) {
		if (status != null) {
			status.setText(statusMsg);
		}
	}

	private JList<Tag> buildActiveTagsList() {
		JList<Tag> activeTags = new JList<Tag>(duplicateOperations.getFilterTags().toArray(new Tag[0]));
		return activeTags;
	}

	private void setupComponents() {
		path = new JTextField(20);
		find = new JButton("Find");
		stop = new JButton("Stop");
		status = new JLabel("Idle");
		sortSimilar = new JButton("Sort similar");
		sortFilter = new JButton("Sort filter");

		groups = new JList<ResultGroup>();
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
				JList<Tag> activeTags = buildActiveTagsList();

				Object[] message = { "Active Tags:", new JScrollPane(activeTags) };
				JOptionPane pane = new JOptionPane(message, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
				JDialog getTopicDialog = pane.createDialog(null, "Select Tag to use in search");
				getTopicDialog.setVisible(true);

				if (pane.getValue() != null && (Integer) pane.getValue() == JOptionPane.OK_OPTION) {
					Tag r = activeTags.getSelectedValue();
					controller.sortFilter(hammingDistance.getValue(), r, path.getText());
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
					ResultGroup group = groupListModel.get(index);
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
		view.add(queueSize);
		view.add(sortFilter, "wrap");
		view.add(groupScrollPane, "growy");
		view.add(hammingDistance, "growx");
		view.add(hammingValue);
	}

	private ResultGroup getSelectedGroup() {
		return groups.getSelectedValue();
	}

	private void setupMenu() {

		JMenuItem directoryTag;
		JMenuItem pruneRecords;
		JMenuItem userTags;
		JMenuItem filters;

		directoryTag = new JMenuItem("Tag directory");
		directoryTag.setToolTipText("Tag all images in a directory and sub-directories");

		pruneRecords = new JMenuItem("Prune records");

		JMenuItem about = new JMenuItem("About");

		directoryTag.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				JTextField directoryField = new JTextField(DEFAULT_TEXTFIELD_WIDTH);

				JList<Tag> activeTags = buildActiveTagsList();
				Object[] message = { "Directory: ", directoryField, "Active Tags:", activeTags };

				JOptionPane pane = new JOptionPane(message, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
				JDialog getTopicDialog = pane.createDialog(null, "Tag all images in directory");
				getTopicDialog.setVisible(true);

				if (pane.getValue() != null && (Integer) pane.getValue() == JOptionPane.OK_OPTION) {
					Path selectedPath = Paths.get(directoryField.getText());
					duplicateOperations.markDirectoryAndChildrenAs(selectedPath, activeTags.getSelectedValue());
				}
			}
		});

		pruneRecords.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				Path directory = Paths.get(path.getText());

				List<ImageRecord> toPrune = duplicateOperations.findMissingFiles(directory);

				LOGGER.info("Found {} non-existant records", toPrune.size());

				Object options[] = { "Prune " + toPrune.size() + " records?" };
				JOptionPane pane = new JOptionPane(options, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
				JDialog dialog = pane.createDialog("Prune records");
				dialog.setVisible(true);

				if (pane.getValue() != null && (Integer) pane.getValue() == JOptionPane.OK_OPTION) {
					duplicateOperations.remove(toPrune);
				} else {
					LOGGER.info("User aborted prune operation for {}", directory);
				}
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

		filters = new JMenuItem("Filters");
		filters.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new FilterView(filterRepository);
			}
		});

		JMenu file = new JMenu("File");
		file.add(directoryTag);
		file.add(pruneRecords);

		JMenu help = new JMenu("Help");
		help.add(about);

		JMenu settings = new JMenu("Settings");
		settings.add(userTags);
		settings.add(filters);

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

	private void deleteAll(ResultGroup group) {
		duplicateOperations.deleteAll(group.getResults());
		groupListModel.removeElement(group);
	}

	private void tagAll(ResultGroup group) {
		JList<Tag> activeTags = buildActiveTagsList();

		Object[] message = { "Active Tags:", new JScrollPane(activeTags) };

		JOptionPane pane = new JOptionPane(message, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
		JDialog getTopicDialog = pane.createDialog(null, "Tag all images");
		getTopicDialog.setVisible(true);

		if (pane.getValue() != null && (Integer) pane.getValue() == JOptionPane.OK_OPTION) {
			duplicateOperations.markAll(group.getResults(), activeTags.getSelectedValue());
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

	private void updateProgress() {
		Timer timer = new Timer("Progress updater", true);

		int intervall = 1000;

		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				queueSize.setText(QUEUE_LABEL + controller.getNumberOfQueuedTasks());
			}
		}, intervall, intervall);
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

			JMenuItem tagAll = new JMenuItem("Tag all");
			tagAll.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					tagAll(getSelectedGroup());
				}
			});

			this.add(deleteAll);
			this.add(tagAll);
			this.addSeparator();

			addUserTags(utsController);
		}

		/**
		 * Create mark menu items for all user tags
		 * 
		 * @param tagController
		 *            {@link UserTagSettingController} to use
		 */
		private void addUserTags(UserTagSettingController tagController) {
			for (Tag tag : tagController.getAllUserTags()) {
				JMenuItem menu = new JMenuItem("Tag " + tag.toString());
				menu.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						duplicateOperations.markAll(getSelectedGroup().getResults(), tag);
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

	/**
	 * Set the title for the result group display and update it's view with the backing {@link ResultGroupPresenter}.
	 * 
	 * @param title
	 *            for the result group window
	 * @param rgp
	 *            the presenter that will be used to create the {@link ResultGroupView}
	 */
	public void displayResultGroup(String title, ResultGroupPresenter rgp) {
		resultGroupWindow.getContentPane().removeAll();

		this.resultGroupWindow.setTitle(title);
		JComponent rgv = new ResultGroupView(rgp).getView();
		rgv.setPreferredSize(resultGroupWindow.getSize());
		this.resultGroupWindow.add(rgv);
		this.resultGroupWindow.setVisible(true);
	}
}
