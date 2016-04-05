/*  Copyright (C) 2014  Nicholas Wright
    
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
package com.github.dozedoff.similarImage;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.github.dozedoff.similarImage.thread.FilterSorterTest;
import com.github.dozedoff.similarImage.thread.GroupListPopulatorTest;
import com.github.dozedoff.similarImage.thread.ImageHashJobTest;
import com.github.dozedoff.similarImage.thread.ImageSorterTest;
import com.github.dozedoff.similarImage.thread.LoadJobVisitorTest;
import com.github.dozedoff.similarImage.thread.NamedThreadFactoryTest;

//@formatter:off
@RunWith(Suite.class)
@SuiteClasses({
	NamedThreadFactoryTest.class,
	GroupListPopulatorTest.class,
	FilterSorterTest.class,
	ImageHashJobTest.class,
	ImageSorterTest.class,
	LoadJobVisitorTest.class
})
public class ThreadTests {}
