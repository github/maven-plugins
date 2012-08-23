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

import org.codehaus.plexus.util.DirectoryScanner;

/**
 * Path utilities
 *
 * @author Kevin Sawicki (kevin@github.com)
 */
public class PathUtils {

	/**
	 * Get matching paths found in given base directory
	 *
	 * @param includes
	 * @param excludes
	 * @param baseDir
	 * @return non-null but possibly empty array of string paths relative to the
	 *         base directory
	 */
	public static String[] getMatchingPaths(final String[] includes,
			final String[] excludes, final String baseDir) {
		final DirectoryScanner scanner = new DirectoryScanner();
		scanner.setBasedir(baseDir);
		if (includes != null && includes.length > 0)
			scanner.setIncludes(includes);
		if (excludes != null && excludes.length > 0)
			scanner.setExcludes(excludes);
		scanner.scan();
		return scanner.getIncludedFiles();
	}
}
