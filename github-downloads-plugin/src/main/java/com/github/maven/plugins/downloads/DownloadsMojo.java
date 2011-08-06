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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
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

	/**
	 * Extra repository id from given SCM URL
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
				+ 1, url.length() - IGitHubConstants.SUFFIX_GIT.length());
		return RepositoryId.createFromId(url);
	}

	/**
	 * Get matching paths found in given base directory
	 * 
	 * @param includes
	 * @param excludes
	 * @param baseDir
	 * @return non-null but possibly empty array of string paths relative to the
	 *         base directory
	 */
	public static String[] getMatchingPaths(String[] includes,
			String[] excludes, String baseDir) {
		DirectoryScanner scanner = new DirectoryScanner();
		scanner.setBasedir(baseDir);
		if (includes != null && includes.length > 0)
			scanner.setIncludes(includes);
		if (excludes != null && excludes.length > 0)
			scanner.setExcludes(excludes);
		scanner.scan();
		return scanner.getIncludedFiles();
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
	 * @parameter expression="${github.downloads.override}"
	 */
	private boolean override;

	/**
	 * Include attached artifacts
	 * 
	 * @parameter expression="${github.downloads.includeAttached}"
	 */
	private boolean includeAttached;

	/**
	 * Show what downloads would be deleted and uploaded but don't actually
	 * alter the current set of repository downloads. Showing what downloads
	 * will be deleted does require still listing the current downloads
	 * available from the repository.
	 * 
	 * @parameter expression="${github.downloads.dryRun}"
	 */
	private boolean dryRun;

	/**
	 * Host for API calls
	 * 
	 * @parameter expression="${github.downloads.host}"
	 */
	private String host;

	/**
	 * Project being built
	 * 
	 * @parameter expression="${project}
	 * @required
	 */
	private MavenProject project;

	/**
	 * Files to exclude
	 * 
	 * @parameter
	 */
	private String[] excludes;

	/**
	 * Files to include
	 * 
	 * @parameter
	 */
	private String[] includes;

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
		GitHubClient client;
		if (!isEmpty(host))
			client = new GitHubClient(host, -1, IGitHubConstants.PROTOCOL_HTTPS);
		else
			client = new GitHubClient();
		if (!isEmpty(userName, password)) {
			if (isDebug())
				debug("Using basic authentication with username: " + userName);
			client.setCredentials(userName, password);
		} else if (!isEmpty(oauth2Token)) {
			if (isDebug())
				debug("Using OAuth2 access token authentication");
			client.setOAuth2Token(oauth2Token);
		} else
			throw new MojoExecutionException(
					"No authentication credentials configured");
		return client;
	}

	/**
	 * Get formatted exception message for {@link IOException}
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

	/**
	 * Get files to create downloads from
	 * 
	 * @return non-null but possibly empty list of files
	 */
	protected List<File> getFiles() {
		List<File> files = new ArrayList<File>();
		final String[] includePaths = removeEmpties(includes);
		final String[] excludePaths = removeEmpties(excludes);
		if (includePaths.length > 0 || excludePaths.length > 0) {
			String baseDir = project.getBuild().getDirectory();
			if (isDebug())
				debug(MessageFormat.format(
						"Scanning {0} and including {1} and exluding {2}",
						baseDir, Arrays.toString(includePaths),
						Arrays.toString(excludePaths)));
			String[] paths = getMatchingPaths(includePaths, excludePaths,
					baseDir);
			if (isDebug())
				debug(MessageFormat.format("Scanned files to include: {0}",
						Arrays.toString(paths)));
			for (String path : paths)
				files.add(new File(baseDir, path));
		} else {
			File file = getArtifactFile(project.getArtifact());
			if (file != null)
				files.add(file);
			if (includeAttached) {
				List<Artifact> attached = project.getAttachedArtifacts();
				if (attached != null)
					for (Artifact artifact : attached) {
						file = getArtifactFile(artifact);
						if (file != null)
							files.add(file);
					}
			}
			if (isDebug())
				debug(MessageFormat.format("Artifact files to include: {0}",
						files));
		}
		return files;
	}

	/**
	 * Get file from artifact
	 * 
	 * @param artifact
	 * @return existent artifact file or null
	 */
	protected File getArtifactFile(Artifact artifact) {
		if (artifact == null)
			return null;
		File file = artifact.getFile();
		return file != null && file.isFile() && file.exists() ? file : null;
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
	 * Get map of existing downloads with names mapped to download identifiers.
	 * 
	 * @param service
	 * @param repository
	 * @return map of existing downloads
	 * @throws MojoExecutionException
	 */
	protected Map<String, Integer> getExistingDownloads(
			DownloadService service, RepositoryId repository)
			throws MojoExecutionException {
		try {
			Map<String, Integer> existing = new HashMap<String, Integer>();
			for (Download download : service.getDownloads(repository))
				if (!isEmpty(download.getName()))
					existing.put(download.getName(), download.getId());
			if (isDebug()) {
				final int size = existing.size();
				if (size != 1)
					debug(MessageFormat.format("Listed {0} existing downloads",
							size));
				else
					debug("Listed 1 existing download");
			}
			return existing;
		} catch (IOException e) {
			throw new MojoExecutionException("Listing downloads failed: "
					+ getExceptionMessage(e), e);
		}
	}

	/**
	 * Deleting existing download with given id and name
	 * 
	 * @param repository
	 * @param name
	 * @param id
	 * @param service
	 * @throws MojoExecutionException
	 */
	protected void deleteDownload(RepositoryId repository, String name, int id,
			DownloadService service) throws MojoExecutionException {
		try {
			info(MessageFormat.format(
					"Deleting existing download: {0} (id={1})", name,
					Integer.toString(id)));
			if (!dryRun)
				service.deleteDownload(repository, id);
		} catch (IOException e) {
			String prefix = MessageFormat.format(
					"Deleting existing download {0} failed: ", name);
			throw new MojoExecutionException(prefix + getExceptionMessage(e), e);
		}
	}

	public void execute() throws MojoExecutionException {
		RepositoryId repository = getRepository();
		if (repository == null)
			throw new MojoExecutionException(
					"No GitHub repository (owner and name) configured");
		if (isDebug())
			debug(MessageFormat.format("Using GitHub repository {0}",
					repository.generateId()));

		DownloadService service = new DownloadService(createClient());

		Map<String, Integer> existing;
		if (override)
			existing = getExistingDownloads(service, repository);
		else
			existing = Collections.emptyMap();

		List<File> files = getFiles();

		if (dryRun)
			info("Dry run mode, downloads will not be deleted or uploaded");

		int fileCount = files.size();
		if (fileCount != 1)
			info(MessageFormat.format("Adding {0} downloads to repository {1}",
					fileCount, repository.generateId()));
		else
			info(MessageFormat.format("Adding 1 download to repository {0}",
					repository.generateId()));

		for (File file : files) {
			final String name = file.getName();
			final long size = file.length();
			Integer existingId = existing.get(name);
			if (existingId != null)
				deleteDownload(repository, name, existingId, service);

			Download download = new Download().setName(name).setSize(size);
			if (!isEmpty(description))
				download.setDescription(description);

			if (size != 1)
				info(MessageFormat.format("Adding download: {0} ({1} bytes)",
						name, size));
			else
				info(MessageFormat
						.format("Adding download: {0} (1 byte)", name));

			if (!dryRun)
				try {
					DownloadResource resource = service.createResource(
							repository, download);
					service.uploadResource(resource, new FileInputStream(file),
							size);
				} catch (IOException e) {
					String prefix = MessageFormat.format(
							"Resource {0} upload failed: ", name);
					throw new MojoExecutionException(prefix
							+ getExceptionMessage(e), e);
				}
		}
	}
}
