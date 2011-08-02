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
package com.github.mojo.downloads;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.MessageFormat;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
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
	 * Location of the file.
	 * 
	 * @parameter expression="${project.build.directory}"
	 * @required
	 */
	private File outputDirectory;

	/**
	 * Location of the artifact name
	 * 
	 * @parameter expression="${project.build.finalName}"
	 * @required
	 */
	private String buildName;

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
	 * User name for authentication
	 * 
	 * @parameter expression="${github.downloads.oauth2Token}"
	 */
	private String oauth2Token;

	/**
	 * SCM URL
	 * 
	 * @parameter expression="${project.scm.url}"
	 */
	private String scmUrl;

	/**
	 * Project URL
	 * 
	 * @parameter expression="${project.url}"
	 */
	private String projectUrl;

	/**
	 * SCM connection
	 * 
	 * @parameter expression="${project.scm.connection}"
	 */
	private String scmConnection;

	/**
	 * SCM developer connection
	 * 
	 * @parameter expression="${project.scm.developerConnection}"
	 */
	private String scmDeveloperConnection;

	/**
	 * Get repository
	 * 
	 * @return repository id or null if none configured
	 */
	protected RepositoryId getRepository() {
		RepositoryId repo = null;
		if (!isEmpty(repositoryOwner, repositoryName))
			repo = RepositoryId.create(repositoryOwner, repositoryName);
		if (repo == null && !isEmpty(projectUrl))
			repo = RepositoryId.createFromUrl(projectUrl);
		if (repo == null && !isEmpty(scmUrl))
			repo = RepositoryId.createFromUrl(scmUrl);
		if (repo == null)
			repo = extractRepositoryFromScmUrl(scmConnection);
		if (repo == null)
			repo = extractRepositoryFromScmUrl(scmDeveloperConnection);
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

	public void execute() throws MojoExecutionException {
		final Log log = getLog();
		File[] files = outputDirectory.listFiles(new FilenameFilter() {

			public boolean accept(File dir, String name) {
				return name.startsWith(buildName);
			}
		});
		if (files.length == 0) {
			if (log.isDebugEnabled())
				log.debug(MessageFormat.format(
						"No files found in {0} that started with {1}",
						outputDirectory, buildName));
			return;
		}

		RepositoryId repository = getRepository();
		if (repository == null) {
			StringBuilder message = new StringBuilder(
					"No GitHub repository configured:\n");
			message.append("repositoryName=").append(repositoryName)
					.append('\n');
			message.append("repositoryOwner=").append(repositoryOwner)
					.append('\n');
			message.append("scmUrl=").append(scmUrl).append('\n');
			message.append("scmConnection=").append(scmConnection).append('\n');
			message.append("scmDeveloperConnection=")
					.append(scmDeveloperConnection).append('\n');
			throw new MojoExecutionException(message.toString());
		}

		DownloadService service = new DownloadService(createClient());
		for (File file : files) {
			Download download = new Download();
			download.setName(file.getName());
			download.setSize(file.length());
			if (log.isDebugEnabled())
				log.debug(MessageFormat.format(
						"Creating download with name: {0} and size: {1}",
						download.getName(), download.getSize()));
			try {
				DownloadResource resource = service.createResource(repository,
						download);
				service.uploadResource(resource, new FileInputStream(file),
						download.getSize());
			} catch (IOException e) {
				String message = null;
				if (e instanceof RequestException) {
					RequestException requestException = (RequestException) e;
					message = Integer.toString(requestException.getStatus())
							+ " " + requestException.formatErrors();
				} else
					message = e.getMessage();
				throw new MojoExecutionException("Resource upload failed: "
						+ message, e);
			}
		}
	}
}
