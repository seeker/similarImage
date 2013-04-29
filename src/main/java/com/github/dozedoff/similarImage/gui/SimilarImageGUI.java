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
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import com.github.dozedoff.similarImage.app.SimilarImage;

public class SimilarImageGUI extends JFrame {
	private static final long serialVersionUID = 1L;
	private SimilarImage parent;
	
	private JTextField path;
	private JButton find, stop, sort;
	private JLabel status;
	private JProgressBar progress;
	
	private AtomicInteger currentProgress = new AtomicInteger();
	
	public SimilarImageGUI(SimilarImage parent) {
		this.parent = parent;
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setSize(500, 500);
		this.setTitle("Similar Image");
		this.setLayout(new MigLayout("wrap 4"));
		setupComponents();
		this.setVisible(true);
	}
	
	public void setStatus(String statusMsg){
		if(status != null) {
			status.setText(statusMsg);
		}
	}
	
	private void setupComponents() {
		path = new JTextField(20);
		find = new JButton("Find");
		stop = new JButton("Stop");
		status = new JLabel("Idle");
		progress = new JProgressBar();
		progress.setStringPainted(true);
		sort = new JButton("Sort");
		
		find.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String userpath = path.getText();
				parent.indexImages(userpath);
			}
		});
		
		stop.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				parent.stopWorkers();
			}
		});
		
		sort.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				parent.sortDuplicates();
			}
		});
		
		this.add(path);
		this.add(find);
		this.add(stop);
		this.add(status);
		this.add(progress);
		this.add(sort);
	}
	
	public void clearProgress() {
		currentProgress.set(0);
		progress.setMaximum(0);
		progress.setValue(0);
	}
	
	public void setTotalFiles(int numOfFiles) {
		progress.setMaximum(numOfFiles);
	}
	
	public void addDelta(int numOfFiles) {
		currentProgress.addAndGet(numOfFiles);
		progress.setValue(currentProgress.get());
	}
}
