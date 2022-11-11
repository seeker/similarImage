/*  Copyright (C) 2017  Nicholas Wright
    
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
package com.github.dozedoff.similarImage.app;

import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MainSettingValidatorTest {
	@Mock
	private MainSetting mainSetting;

	@Before
	public void setup() {
		when(mainSetting.threads()).thenReturn(1);
	}

	@Test
	public void testValidatePositiveThread() throws Exception {
		MainSettingValidator.validate(mainSetting);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateZeroThread() throws Exception {
		when(mainSetting.threads()).thenReturn(0);

		MainSettingValidator.validate(mainSetting);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateNegativeThread() throws Exception {
		when(mainSetting.threads()).thenReturn(-1);

		MainSettingValidator.validate(mainSetting);
	}
}
