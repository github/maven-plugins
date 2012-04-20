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

import com.github.maven.plugins.core.GitHubProjectMojo;
import com.github.maven.plugins.core.PathUtils;
import com.github.maven.plugins.core.StringUtils;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.eclipse.egit.github.core.Download;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.DownloadService;

/**
 * Mojo that uploads a built resource as a GitHub repository download
 * 
 * @author Kevin Sawicki (kevin@github.com)
 * @goal upload
 */
public class DownloadsMojo extends GitHubProjectMojo {

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
	 *            default-value="${github.global.userName}"
	 */
	private String userName;

	/**
	 * User name for authentication
	 * 
	 * @parameter expression="${github.downloads.password}"
	 *            default-value="${github.global.password}"
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
	 *            default-value="${github.global.oauth2Token}"
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
	 *            default-value="${github.global.host}"
	 */
	private String host;

	/**
	 * Suffix to append to all uploaded files. The configured suffix will go
	 * before the file extension.
	 * 
	 * @parameter expression="${github.downloads.suffix}"
	 */
	private String suffix;

	/**
	 * Id of server to use
	 * 
	 * @parameter expression="${github.downloads.server}"
	 *            default-value="${github.global.server}"
	 */
	private String server;

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
	 * Project being built
	 * 
	 * @parameter expression="${project}
	 * @required
	 */
	private MavenProject project;

	/**
	 * Settings
	 * 
	 * @parameter expression="${settings}
	 */
	private Settings settings;

	/**
	 * Get files to create downloads from
	 * 
	 * @return non-null but possibly empty list of files
	 */
	protected List<File> getFiles() {
		List<File> files = new ArrayList<File>();
		final String[] includePaths = StringUtils.removeEmpties(includes);
		final String[] excludePaths = StringUtils.removeEmpties(excludes);
		if (includePaths.length > 0 || excludePaths.length > 0) {
			String baseDir = project.getBuild().getDirectory();
			if (isDebug())
				debug(MessageFormat.format(
						"Scanning {0} and including {1} and exluding {2}",
						baseDir, Arrays.toString(includePaths),
						Arrays.toString(excludePaths)));
			String[] paths = PathUtils.getMatchingPaths(includePaths,
					excludePaths, baseDir);
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
				if (!StringUtils.isEmpty(download.getName()))
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
		RepositoryId repository = getRepository(project, repositoryOwner,
				repositoryName);

		DownloadService service = new DownloadService(createClient(host,
				userName, password, oauth2Token, server, settings));

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
			String name = file.getName();
			if (!StringUtils.isEmpty(suffix)) {
				final int lastDot = name.lastIndexOf('.');
				if (lastDot != -1)
					name = name.substring(0, lastDot) + suffix
							+ name.substring(lastDot);
				else
					name += suffix;
			}

			final long size = file.length();
			Integer existingId = existing.get(name);
			if (existingId != null)
				deleteDownload(repository, name, existingId, service);

			Download download = new Download().setName(name).setSize(size);
			if (!StringUtils.isEmpty(description))
				download.setDescription(description);

			if (size != 1)
				info(MessageFormat.format("Adding download: {0} ({1} bytes)",
						name, size));
			else
				info(MessageFormat
						.format("Adding download: {0} (1 byte)", name));

			if (!dryRun)
				try {
					service.createDownload(repository, download, file);
				} catch (IOException e) {
					String prefix = MessageFormat.format(
							"Resource {0} upload failed: ", name);
					throw new MojoExecutionException(prefix
							+ getExceptionMessage(e), e);
				}
		}
	}
}
