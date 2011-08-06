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
package com.github.maven.plugins.downloads;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.eclipse.egit.github.core.RepositoryId;
import org.junit.Test;

/**
 * Unit tests of {@link DownloadsMojo}
 * 
 * @author Kevin Sawicki (kevin@github.com)
 */
public class DownloadsMojoTest {

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
	 * Test repository extraction from SCM anonymous Git URL
	 */
	@Test
	public void extractFromAnonymousUrl() {
		RepositoryId repo = DownloadsMojo
				.extractRepositoryFromScmUrl("scm:git:git://github.com/owner/project.git");
		assertNotNull(repo);
		assertEquals("owner", repo.getOwner());
		assertEquals("project", repo.getName());
		assertEquals("owner/project", repo.generateId());
	}

	/**
	 * Test repository extraction from malformed URLs
	 */
	@Test
	public void extractFromMalformedUrls() {
		assertNull(DownloadsMojo
				.extractRepositoryFromScmUrl("scm:git:git://github.com"));
		assertNull(DownloadsMojo
				.extractRepositoryFromScmUrl("scm:git:git://github.com/"));
		assertNull(DownloadsMojo
				.extractRepositoryFromScmUrl("scm:git:git@github.com"));
		assertNull(DownloadsMojo
				.extractRepositoryFromScmUrl("scm:git:git@github.com:"));
		assertNull(DownloadsMojo.extractRepositoryFromScmUrl(null));
		assertNull(DownloadsMojo.extractRepositoryFromScmUrl(""));
		assertNull(DownloadsMojo.extractRepositoryFromScmUrl(" "));
	}

	/**
	 * Test repository extraction from SCM SSH Git URL
	 */
	@Test
	public void extractFromSshUrl() {
		RepositoryId repo = DownloadsMojo
				.extractRepositoryFromScmUrl("scm:git:git@github.com:owner/project.git");
		assertNotNull(repo);
		assertEquals("owner", repo.getOwner());
		assertEquals("project", repo.getName());
		assertEquals("owner/project", repo.generateId());
	}

	/**
	 * Tests of {@link DownloadsMojo#isEmpty(String...)}
	 */
	@Test
	public void isEmpty() {
		assertTrue(DownloadsMojo.isEmpty((String[]) null));
		assertTrue(DownloadsMojo.isEmpty(new String[0]));
		assertTrue(DownloadsMojo.isEmpty((String) null));
		assertTrue(DownloadsMojo.isEmpty(""));
		assertTrue(DownloadsMojo.isEmpty("content", null));
		assertTrue(DownloadsMojo.isEmpty("content", ""));
		assertFalse(DownloadsMojo.isEmpty("content"));
	}

	/**
	 * Tests of {@link DownloadsMojo#removeEmpties(String...)}
	 */
	@Test
	public void removeEmpties() {
		assertArrayEquals(new String[0],
				DownloadsMojo.removeEmpties((String[]) null));
		assertArrayEquals(new String[0],
				DownloadsMojo.removeEmpties((String) null));
		assertArrayEquals(new String[0], DownloadsMojo.removeEmpties(""));
		assertArrayEquals(new String[] { "content" },
				DownloadsMojo.removeEmpties("", "content"));
		assertArrayEquals(new String[] { "content" },
				DownloadsMojo.removeEmpties(null, "content"));
	}

	/**
	 * Test of
	 * {@link DownloadsMojo#getMatchingPaths(String[], String[], String)}
	 * 
	 * @throws IOException
	 */
	@Test
	public void singleInclude() throws IOException {
		File include = File
				.createTempFile("include", ".txt", createDirectory());
		String[] paths = DownloadsMojo.getMatchingPaths(
				new String[] { include.getName() }, null, include.getParent());
		assertNotNull(paths);
		assertEquals(1, paths.length);
		assertEquals(include.getName(), paths[0]);
	}

	/**
	 * Test of
	 * {@link DownloadsMojo#getMatchingPaths(String[], String[], String)}
	 * 
	 * @throws IOException
	 */
	@Test
	public void singleIncludeSingleExclude() throws IOException {
		File dir = createDirectory();
		File include = File.createTempFile("include", ".filetomatch", dir);
		File.createTempFile("neutral", ".notmatch", dir);
		File exclude = File.createTempFile("exlude", ".filetomatch", dir);
		String[] paths = DownloadsMojo.getMatchingPaths(
				new String[] { "*.filetomatch" },
				new String[] { exclude.getName() }, include.getParent());
		assertNotNull(paths);
		assertEquals(1, paths.length);
		assertEquals(include.getName(), paths[0]);
	}

	/**
	 * Test of
	 * {@link DownloadsMojo#getMatchingPaths(String[], String[], String)}
	 * 
	 * @throws IOException
	 */
	@Test
	public void singleExlude() throws IOException {
		File dir = createDirectory();
		File include = File.createTempFile("include", ".filetomatch", dir);
		File exclude = File.createTempFile("exlude", ".filetomatch", dir);
		String[] paths = DownloadsMojo.getMatchingPaths(null,
				new String[] { exclude.getName() }, include.getParent());
		assertNotNull(paths);
		assertEquals(1, paths.length);
		assertEquals(include.getName(), paths[0]);
	}
}
