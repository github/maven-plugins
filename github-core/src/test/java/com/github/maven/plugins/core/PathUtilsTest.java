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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

/**
 * Unit tests of {@link PathUtils}
 *
 * @author Kevin Sawicki (kevin@github.com)
 */
public class PathUtilsTest {

	/**
	 * Create temporary directory to use in a test
	 *
	 * @return directory that exists
	 */
	public static final File createDirectory() {
		String tmpDir = System.getProperty("java.io.tmpdir");
		assertNotNull(tmpDir);
		File dir = new File(tmpDir, "test" + System.nanoTime());
		assertFalse(dir.exists());
		assertTrue(dir.mkdirs());
		dir.deleteOnExit();
		return dir;
	}

	/**
	 * Test of {@link PathUtils#getMatchingPaths(String[], String[], String)}
	 *
	 * @throws IOException
	 */
	@Test
	public void singleInclude() throws IOException {
		File include = File
				.createTempFile("include", ".txt", createDirectory());
		String[] paths = PathUtils.getMatchingPaths(
				new String[] { include.getName() }, null, include.getParent());
		assertNotNull(paths);
		assertEquals(1, paths.length);
		assertEquals(include.getName(), paths[0]);
	}

	/**
	 * Test of {@link PathUtils#getMatchingPaths(String[], String[], String)}
	 *
	 * @throws IOException
	 */
	@Test
	public void singleIncludeSingleExclude() throws IOException {
		File dir = createDirectory();
		File include = File.createTempFile("include", ".filetomatch", dir);
		File.createTempFile("neutral", ".notmatch", dir);
		File exclude = File.createTempFile("exlude", ".filetomatch", dir);
		String[] paths = PathUtils.getMatchingPaths(
				new String[] { "*.filetomatch" },
				new String[] { exclude.getName() }, include.getParent());
		assertNotNull(paths);
		assertEquals(1, paths.length);
		assertEquals(include.getName(), paths[0]);
	}

	/**
	 * Test of {@link PathUtils#getMatchingPaths(String[], String[], String)}
	 *
	 * @throws IOException
	 */
	@Test
	public void singleExlude() throws IOException {
		File dir = createDirectory();
		File include = File.createTempFile("include", ".filetomatch", dir);
		File exclude = File.createTempFile("exlude", ".filetomatch", dir);
		String[] paths = PathUtils.getMatchingPaths(null,
				new String[] { exclude.getName() }, include.getParent());
		assertNotNull(paths);
		assertEquals(1, paths.length);
		assertEquals(include.getName(), paths[0]);
	}
}
