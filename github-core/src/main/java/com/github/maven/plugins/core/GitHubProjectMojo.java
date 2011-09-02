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

import java.io.IOException;
import java.text.MessageFormat;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.RequestException;

/**
 * Base GitHub Mojo class to be extended.
 * 
 * @author Kevin Sawicki (kevin@github.com)
 */
public abstract class GitHubProjectMojo extends AbstractMojo {

	/**
	 * Get formatted exception message for {@link IOException}
	 * 
	 * @param e
	 * @return message
	 */
	public static String getExceptionMessage(IOException e) {
		String message = null;
		if (e instanceof RequestException) {
			RequestException requestException = (RequestException) e;
			message = Integer.toString(requestException.getStatus()) + " "
					+ requestException.formatErrors();
		} else
			message = e.getMessage();
		return message;
	}

	/**
	 * Is debug logging enabled?
	 * 
	 * @return true if enabled, false otherwise
	 */
	protected boolean isDebug() {
		final Log log = getLog();
		return log != null ? log.isDebugEnabled() : false;
	}

	/**
	 * Is info logging enabled?
	 * 
	 * @return true if enabled, false otherwise
	 */
	protected boolean isInfo() {
		final Log log = getLog();
		return log != null ? log.isInfoEnabled() : false;
	}

	/**
	 * Log given message at debug level
	 * 
	 * @param message
	 */
	protected void debug(String message) {
		final Log log = getLog();
		if (log != null)
			log.debug(message);
	}

	/**
	 * Log given message and throwable at debug level
	 * 
	 * @param message
	 * @param throwable
	 */
	protected void debug(String message, Throwable throwable) {
		final Log log = getLog();
		if (log != null)
			log.debug(message, throwable);
	}

	/**
	 * Log given message at info level
	 * 
	 * @param message
	 */
	protected void info(String message) {
		final Log log = getLog();
		if (log != null)
			log.info(message);
	}

	/**
	 * Log given message and throwable at info level
	 * 
	 * @param message
	 * @param throwable
	 */
	protected void info(String message, Throwable throwable) {
		final Log log = getLog();
		if (log != null)
			log.info(message, throwable);
	}

	/**
	 * Create client
	 * 
	 * @param host
	 * @param userName
	 * @param password
	 * @param oauth2Token
	 * @return client
	 * @throws MojoExecutionException
	 */
	protected GitHubClient createClient(String host, String userName,
			String password, String oauth2Token) throws MojoExecutionException {
		GitHubClient client;
		if (!StringUtils.isEmpty(host))
			client = new GitHubClient(host);
		else
			client = new GitHubClient();
		if (!StringUtils.isEmpty(userName, password)) {
			if (isDebug())
				debug("Using basic authentication with username: " + userName);
			client.setCredentials(userName, password);
		} else if (!StringUtils.isEmpty(oauth2Token)) {
			if (isDebug())
				debug("Using OAuth2 access token authentication");
			client.setOAuth2Token(oauth2Token);
		} else
			throw new MojoExecutionException(
					"No authentication credentials configured");
		return client;
	}

	/**
	 * Get repository and throw a {@link MojoExecutionException} on failures
	 * 
	 * @param project
	 * @param owner
	 * @param name
	 * @return non-null repository id
	 * @throws MojoExecutionException
	 */
	protected RepositoryId getRepository(MavenProject project, String owner,
			String name) throws MojoExecutionException {
		RepositoryId repository = RepositoryUtils.getRepository(project, owner,
				name);
		if (repository == null)
			throw new MojoExecutionException(
					"No GitHub repository (owner and name) configured");
		if (isDebug())
			debug(MessageFormat.format("Using GitHub repository {0}",
					repository.generateId()));
		return repository;
	}
}
