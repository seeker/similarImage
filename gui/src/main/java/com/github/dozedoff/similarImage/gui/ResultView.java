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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

/**
 * Displays a image and the corresponding information. Displays the full image or a context menu depending on the mouse click.
 * 
 * @author Nicholas Wright
 *
 */
public class ResultView implements View {
	private JPanel view;
	private JLabel image;
	private ResultPresenter presenter;

	/**
	 * Creates a default view with no image and no information. A placeholder "NO IMAGE" is displayed.
	 * 
	 * @param presenter
	 *            responsible for handling image requests
	 * @param opMenu
	 *            menu containing the possible operations for this image
	 */
	public ResultView(ResultPresenter presenter, OperationsMenu opMenu) {
		this.presenter = presenter;
		view = new JPanel();
		view.setLayout(new MigLayout("wrap"));
		image = new JLabel("NO IMAGE");
		view.addMouseListener(new ClickListener());
		view.setComponentPopupMenu(opMenu.getMenu());
		this.presenter.setView(this);
	}

	public void createLable(String info) {
		JLabel lable = new JLabel(info);
		view.add(lable);
	}

	public void setImage(JLabel image) {
		view.remove(this.image);
		this.image = image;
		view.add(this.image);
		view.revalidate();
	}

	public void displayFullImage(JLabel image, Path path) {
		new FullImageView(image, path);
	}

	@Override
	public JComponent getView() {
		return view;
	}

	class ClickListener extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getButton() == MouseEvent.BUTTON1) {
				presenter.displayFullImage();
			}
		}
	}
}
