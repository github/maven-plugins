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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.egit.github.core.Download;
import org.eclipse.egit.github.core.DownloadResource;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.IGitHubConstants;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.DownloadService;

/**
 * Mojo that uploads a built resource as a GitHub repository download
 * 
 * @author Kevin Sawicki (kevin@github.com)
 * @goal upload
 */
public class DownloadsMojo extends AbstractMojo {

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
	 * Extra repository id from scm url
	 * 
	 * @param url
	 * @return repository id or null if extraction fails
	 */
	public static RepositoryId extractRepositoryFromScmUrl(String url) {
		if (isEmpty(url))
			return null;
		int ghIndex = url.indexOf(IGitHubConstants.HOST_DEFAULT);
		if (ghIndex == -1 || ghIndex + 1 >= url.length())
			return null;
		if (!url.endsWith(IGitHubConstants.SUFFIX_GIT))
			return null;
		url = url.substring(ghIndex + IGitHubConstants.HOST_DEFAULT.length()
				+ 1);
		url = url.substring(0,
				url.length() - IGitHubConstants.SUFFIX_GIT.length());
		return RepositoryId.createFromId(url);
	}

	/**
	 * Owner of repository to upload to
	 * 
	 * @parameter expression="${github.downloads.repositoryOwner}"
	 */
	private String repositoryOwner;

	/**
	 * Name of repository to upload to
	 * 
	 * @parameter expression="${github.downloads.repositoryName}"
	 */
	private String repositoryName;

	/**
	 * User name for authentication
	 * 
	 * @parameter expression="${github.downloads.userName}"
	 */
	private String userName;

	/**
	 * User name for authentication
	 * 
	 * @parameter expression="${github.downloads.password}"
	 */
	private String password;

	/**
	 * Description of download
	 * 
	 * @parameter
	 */
	private String description;

	/**
	 * User name for authentication
	 * 
	 * @parameter expression="${github.downloads.oauth2Token}"
	 */
	private String oauth2Token;

	/**
	 * Override existing downloads
	 * 
	 * @parameter
	 */
	private boolean override;

	/**
	 * 
	 * @parameter expression="${project}
	 * @required
	 */
	private MavenProject project;

	/**
	 * Get repository
	 * 
	 * @return repository id or null if none configured
	 */
	protected RepositoryId getRepository() {
		RepositoryId repo = null;
		if (!isEmpty(repositoryOwner, repositoryName))
			repo = RepositoryId.create(repositoryOwner, repositoryName);
		if (repo == null && !isEmpty(project.getUrl()))
			repo = RepositoryId.createFromUrl(project.getUrl());
		if (repo == null && !isEmpty(project.getScm().getUrl()))
			repo = RepositoryId.createFromUrl(project.getScm().getUrl());
		if (repo == null)
			repo = extractRepositoryFromScmUrl(project.getScm().getConnection());
		if (repo == null)
			repo = extractRepositoryFromScmUrl(project.getScm()
					.getDeveloperConnection());
		return repo;
	}

	/**
	 * Create client
	 * 
	 * @return client
	 * @throws MojoExecutionException
	 */
	protected GitHubClient createClient() throws MojoExecutionException {
		final Log log = getLog();
		GitHubClient client = new GitHubClient();
		if (userName != null && password != null) {
			if (log.isDebugEnabled())
				log.debug("Using basic authentication with username: "
						+ userName);
			client.setCredentials(userName, password);
		} else if (oauth2Token != null) {
			if (log.isDebugEnabled())
				log.debug("Using OAuth2 authentication");
			client.setOAuth2Token(oauth2Token);
		} else
			throw new MojoExecutionException(
					"No authentication credentials configured");
		return client;
	}

	/**
	 * Get exception message for {@link IOException}
	 * 
	 * @param e
	 * @return message
	 */
	protected String getExceptionMessage(IOException e) {
		String message = null;
		if (e instanceof RequestException) {
			RequestException requestException = (RequestException) e;
			message = Integer.toString(requestException.getStatus()) + " "
					+ requestException.formatErrors();
		} else
			message = e.getMessage();
		return message;
	}

	public void execute() throws MojoExecutionException {
		final Log log = getLog();
		final boolean debug = log.isDebugEnabled();

		RepositoryId repository = getRepository();
		if (repository == null)
			throw new MojoExecutionException("No GitHub repository configured");

		DownloadService service = new DownloadService(createClient());

		Map<String, Integer> existing;
		if (override)
			try {
				existing = new HashMap<String, Integer>();
				for (Download download : service.getDownloads(repository))
					if (!isEmpty(download.getName()))
						existing.put(download.getName(), download.getId());
				if (debug)
					log.debug(MessageFormat.format(
							"Listed {0} existing downloads", existing.size()));
			} catch (IOException e) {
				throw new MojoExecutionException("Listing downloads failed: "
						+ getExceptionMessage(e), e);
			}
		else
			existing = Collections.emptyMap();

		File file = project.getArtifact().getFile();
		final String name = file.getName();

		Integer existingId = existing.get(name);
		if (existingId != null)
			try {
				if (debug)
					log.debug(MessageFormat.format(
							"Deleting existing download: {0} ({1})", name,
							existingId));
				service.deleteDownload(repository, existingId);
			} catch (IOException e) {
				throw new MojoExecutionException(
						"Delete existing download failed: "
								+ getExceptionMessage(e), e);
			}

		Download download = new Download();
		download.setName(name);
		if (!isEmpty(description))
			download.setDescription(description);
		download.setSize(file.length());
		if (debug)
			log.debug(MessageFormat.format(
					"Creating download with name {0} and size {1}", name,
					download.getSize()));
		try {
			DownloadResource resource = service.createResource(repository,
					download);
			service.uploadResource(resource, new FileInputStream(file),
					download.getSize());
		} catch (IOException e) {
			throw new MojoExecutionException("Resource upload failed: "
					+ getExceptionMessage(e), e);
		}
	}
}
