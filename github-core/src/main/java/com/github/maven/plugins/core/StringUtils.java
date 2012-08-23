/*
 * Copyright (c) 2011 GitHub Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.github.maven.plugins.core;

import java.util.ArrayList;
import java.util.List;

/**
 * String utilities
 *
 * @author Kevin Sawicki (kevin@github.com)
 */
public class StringUtils {

	/**
	 * Are any given values null or empty?
	 *
	 * @param values
	 * @return true if any null or empty, false otherwise
	 */
	public static boolean isEmpty(final String... values) {
		if (values == null || values.length == 0)
			return true;
		for (String value : values)
			if (value == null || value.length() == 0)
				return true;
		return false;
	}

	/**
	 * Create an array with only the non-null and non-empty values
	 *
	 * @param values
	 * @return non-null but possibly empty array of non-null/non-empty strings
	 */
	public static String[] removeEmpties(final String... values) {
		if (values == null || values.length == 0)
			return new String[0];
		List<String> validValues = new ArrayList<String>();
		for (String value : values)
			if (value != null && value.length() > 0)
				validValues.add(value);
		return validValues.toArray(new String[validValues.size()]);
	}

}
