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
package com.github.dozedoff.similarImage.io;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.swing.JProgressBar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.commonj.io.DataProducer;
import com.github.dozedoff.commonj.util.Pair;
import com.github.dozedoff.similarImage.db.BadFileRecord;
import com.github.dozedoff.similarImage.db.Persistence;

public class ImageProducer extends DataProducer<Path, Pair<Path, BufferedImage>> {
	private static final Logger logger = LoggerFactory.getLogger(ImageProducer.class);
	private final JProgressBar bufferLevel, totalProgress;
	private final Persistence persistence;
	private final AtomicInteger total = new AtomicInteger();
	private final AtomicInteger processed = new AtomicInteger();

	private final int WORK_BATCH_SIZE = 20;

	private AbstractBufferStrategy bufferStrategy = new SimpleBufferStrategy(input, output, WORK_BATCH_SIZE);

	public ImageProducer(int maxOutputQueueSize, Persistence persistence) {
		super(maxOutputQueueSize);
		this.bufferStrategy = new RefillBufferStartegy(input, output, maxOutputQueueSize);
		this.persistence = persistence;

		totalProgress = new JProgressBar(processed.get(), total.get());
		totalProgress.setStringPainted(true);
		bufferLevel = new JProgressBar(0, maxOutputQueueSize);
		bufferLevel.setStringPainted(true);

	}

	public JProgressBar getBufferLevel() {
		return bufferLevel;
	}

	@Override
	public void addToLoad(List<Path> paths) {
		total.addAndGet(paths.size());
		super.addToLoad(paths);
	}

	@Override
	public void addToLoad(Path... paths) {
		total.addAndGet(paths.length);
		super.addToLoad(paths);
	}

	@Override
	public void clear() {
		super.clear();
		processed.set(0);
		total.set(0);
	}

	public int getTotal() {
		return total.get();
	}

	public int getProcessed() {
		return processed.get();
	}

	public JProgressBar getTotalProgress() {
		return totalProgress;
	}

	@Override
	protected void loaderDoWork() throws InterruptedException {
		Path n = null;
		ArrayList<Path> work = new ArrayList<Path>(WORK_BATCH_SIZE + 1);

		if (bufferStrategy.workAvailable()) {
			synchronized (output) {
				output.notifyAll();
			}
		}

		n = input.take();
		work.add(n);
		input.drainTo(work, WORK_BATCH_SIZE);

		for (Path p : work) {
			try {
				processFile(p);
			} catch (IIOException e) {
				logger.warn("Failed to process image(IIO) - {}", e.getMessage());
				try {
					persistence.addBadFile(new BadFileRecord(p));
				} catch (SQLException e1) {
					logger.warn("Failed to add bad file record for {} - {}", p, e.getMessage());
				}
			} catch (IOException e) {
				logger.warn("Failed to load file - {}", e.getMessage());
			} catch (SQLException e) {
				logger.warn("Failed to query database - {}", e.getMessage());
			} catch (Exception e) {
				logger.warn("Failed to process image(other) - {}", e.getMessage());
				try {
					persistence.addBadFile(new BadFileRecord(p));
				} catch (SQLException e1) {
					logger.warn("Failed to add bad file record for {} - {}", p, e.getMessage());
				}
			}
		}
	}

	private void processFile(Path next) throws SQLException, IOException, InterruptedException {
		if (persistence.isBadFile(next) || persistence.isPathRecorded(next)) {
			processed.addAndGet(1);
			totalProgress.setValue(processed.get());
			return;
		}

		byte[] data = Files.readAllBytes(next);
		InputStream is = new ByteArrayInputStream(data);
		BufferedImage img = ImageIO.read(is);

		if (img == null) {
			throw new IIOException("No ImageReader was able to read " + next.toString());
		}

		Pair<Path, BufferedImage> pair = new Pair<Path, BufferedImage>(next, img);
		output.put(pair);

		processed.addAndGet(1);
		totalProgress.setValue(processed.get());
	}

	@Override
	protected void outputQueueChanged() {
		bufferLevel.setValue(output.size());
	}

	@Override
	public void drainTo(Collection<Pair<Path, BufferedImage>> drainTo, int maxElements) throws InterruptedException {
		bufferStrategy.bufferCheck();
		super.drainTo(drainTo, maxElements);
	}
}
