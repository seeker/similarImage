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

import com.github.dozedoff.similarImage.duplicate.BucketComperatorTest;
import com.github.dozedoff.similarImage.duplicate.BucketTest;
import com.github.dozedoff.similarImage.duplicate.CompareHammingDistanceTest;
import com.github.dozedoff.similarImage.duplicate.CompareTest;
import com.github.dozedoff.similarImage.duplicate.DuplicateOperationsTest;
import com.github.dozedoff.similarImage.duplicate.DuplicateUtilTest;
import com.github.dozedoff.similarImage.duplicate.ImageInfoTest;
import com.github.dozedoff.similarImage.duplicate.ImageRecordComperatorTest;
import com.github.dozedoff.similarImage.duplicate.SortSimilarTest;

//@formatter:off
@RunWith(Suite.class)
@SuiteClasses({
	CompareTest.class,
	SortSimilarTest.class,
	BucketTest.class,
	CompareHammingDistanceTest.class,
	DuplicateOperationsTest.class,
	ImageInfoTest.class,
	BucketComperatorTest.class,
	DuplicateUtilTest.class,
	ImageRecordComperatorTest.class
})
public class DuplicateTests {}
