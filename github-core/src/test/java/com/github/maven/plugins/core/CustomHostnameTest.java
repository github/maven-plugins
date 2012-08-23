/*
 * Copyright (c) 2012 GitHub Inc.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.settings.Settings;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.junit.Test;

/**
 * Tests using client with custom hostname
 *
 * @author Kevin Sawicki (kevin@github.com)
 */
public class CustomHostnameTest {

	private class TestMojo extends GitHubProjectMojo {

		private final AtomicReference<String> host = new AtomicReference<String>();

		protected GitHubClient createClient() {
			host.set(null);
			return super.createClient();
		}

		protected GitHubClient createClient(String hostname)
				throws MojoExecutionException {
			host.set(hostname);
			return super.createClient(hostname);
		}

		public GitHubClient createClient(String host, String userName,
				String password, String oauth2Token, String serverId,
				Settings settings, MavenSession session)
				throws MojoExecutionException {
			return super.createClient(host, userName, password, oauth2Token,
					serverId, settings, session);
		}

		public void execute() throws MojoExecutionException,
				MojoFailureException {
			// Intentionally left blank
		}
	}

	/**
	 * Test custom hostname
	 *
	 * @throws Exception
	 */
	@Test
	public void validHostname() throws Exception {
		TestMojo mojo = new TestMojo();
		GitHubClient client = mojo.createClient("h", "a", "b", null, null,
				null, null);
		assertNotNull(client);
		assertEquals("h", mojo.host.get());
	}

	/**
	 * Test null custom hostname
	 *
	 * @throws Exception
	 */
	@Test
	public void nullHostname() throws Exception {
		TestMojo mojo = new TestMojo();
		GitHubClient client = mojo.createClient(null, "a", "b", null, null,
				null, null);
		assertNotNull(client);
		assertNull(mojo.host.get());
	}

	/**
	 * Test empty custom hostname
	 *
	 * @throws Exception
	 */
	@Test
	public void emptyHost() throws Exception {
		TestMojo mojo = new TestMojo();
		GitHubClient client = mojo.createClient("", "a", "b", null, null, null,
				null);
		assertNotNull(client);
		assertNull(mojo.host.get());
	}
}
