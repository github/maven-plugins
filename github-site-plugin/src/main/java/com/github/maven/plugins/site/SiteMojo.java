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
package com.github.maven.plugins.site;

import static java.lang.Integer.MAX_VALUE;
import static org.eclipse.egit.github.core.Blob.ENCODING_BASE64;
import static org.eclipse.egit.github.core.TreeEntry.MODE_BLOB;
import static org.eclipse.egit.github.core.TreeEntry.TYPE_BLOB;
import static org.eclipse.egit.github.core.TypedResource.TYPE_COMMIT;

import com.github.maven.plugins.core.GitHubProjectMojo;
import com.github.maven.plugins.core.PathUtils;
import com.github.maven.plugins.core.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.eclipse.egit.github.core.Blob;
import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.CommitUser;
import org.eclipse.egit.github.core.Reference;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.Tree;
import org.eclipse.egit.github.core.TreeEntry;
import org.eclipse.egit.github.core.TypedResource;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.DataService;
import org.eclipse.egit.github.core.service.UserService;
import org.eclipse.egit.github.core.util.EncodingUtils;

/**
 * Mojo which copies files to a GitHub repository branch. This directly uses the
 * GitHub data API to upload blobs, make commits, and update references and so a
 * local Git repository is not used.
 *
 * @author Kevin Sawicki (kevin@github.com)
 * @goal site
 */
public class SiteMojo extends GitHubProjectMojo {

	/**
	 * BRANCH_DEFAULT
	 */
	public static final String BRANCH_DEFAULT = "refs/heads/gh-pages";

	/**
	 * NO_JEKYLL_FILE
	 */
	public static final String NO_JEKYLL_FILE = ".nojekyll";

	/**
	 * Branch to update
	 *
	 * @parameter default-value="refs/heads/gh-pages"
	 */
	private String branch = BRANCH_DEFAULT;

	/**
	 * Path of tree
	 *
	 * @parameter
	 */
	private String path;

	/**
	 * The commit message used when committing the site.
	 *
	 * @parameter
	 * @required
	 */
	private String message;

	/**
	 * The name of the repository. This setting must be set if the project's url and scm metadata are not set.
	 *
	 * @parameter expression="${github.site.repositoryName}"
	 */
	private String repositoryName;

	/**
	 * The owner of repository. This setting must be set if the project's url and scm metadata are not set.
	 *
	 * @parameter expression="${github.site.repositoryOwner}"
	 */
	private String repositoryOwner;

	/**
	 * The user name for authentication
	 *
	 * @parameter expression="${github.site.userName}"
	 *            default-value="${github.global.userName}"
	 */
	private String userName;

	/**
	 * The password for authentication
	 *
	 * @parameter expression="${github.site.password}"
	 *            default-value="${github.global.password}"
	 */
	private String password;

	/**
	 * The oauth2 token for authentication
	 *
	 * @parameter expression="${github.site.oauth2Token}"
	 *            default-value="${github.global.oauth2Token}"
	 */
	private String oauth2Token;

	/**
	 * The Host for API calls.
	 *
	 * @parameter expression="${github.site.host}"
	 *            default-value="${github.global.host}"
	 */
	private String host;

	/**
	 * The <em>id</em> of the server to use to retrieve the Github credentials. This id must identify a
     * <em>server</em> from your <em>setting.xml</em> file.
	 *
	 * @parameter expression="${github.site.server}"
	 *            default-value="${github.global.server}"
	 */
	private String server;

	/**
	 * Paths and patterns to include
	 *
	 * @parameter
	 */
	private String[] includes;

	/**
	 * Paths and patterns to exclude
	 *
	 * @parameter
	 */
	private String[] excludes;

	/**
	 * The base directory to commit files from. <em>target/site</em> by default.
	 *
	 * @parameter expression="${siteOutputDirectory}"
	 *            default-value="${project.reporting.outputDirectory}"
	 * @required
	 */
	private File outputDirectory;

	/**
	 * The project being built
	 *
	 * @parameter expression="${project}
	 * @required
	 */
	private MavenProject project;

	/**
	 * The Maven session
	 *
	 * @parameter expression="${session}
	 */
	private MavenSession session;

	/**
	 * The Maven settings
	 *
	 * @parameter expression="${settings}
	 */
	private Settings settings;

	/**
	 * Force reference update
	 *
	 * @parameter expression="${github.site.force}"
	 */
	private boolean force;

	/**
	 * Set it to {@code true} to always create a '.nojekyll' file at the root of the site if one
	 * doesn't already exist.
	 *
	 * @parameter expression="${github.site.noJekyll}"
	 */
	private boolean noJekyll;

	/**
	 * Set it to {@code true} to merge with existing the existing tree that is referenced by the commit
	 * that the ref currently points to
	 *
	 * @parameter expression="${github.site.merge}"
	 */
	private boolean merge;

	/**
	 * Show what blob, trees, commits, and references would be created/updated
	 * but don't actually perform any operations on the target GitHub
	 * repository.
	 *
	 * @parameter expression="${github.site.dryRun}"
	 */
	private boolean dryRun;

    /**
     * Skip the site upload.
     *
     * @parameter expression="${github.site.skip}"
     *            default-value="false"
     * @since 0.9
     */
    private boolean skip;

	/**
	 * Create blob
	 *
	 * @param service
	 * @param repository
	 * @param path
	 * @return blob SHA-1
	 * @throws MojoExecutionException
	 */
	protected String createBlob(DataService service, RepositoryId repository,
			String path) throws MojoExecutionException {
		File file = new File(outputDirectory, path);
		final long length = file.length();
		final int size = length > MAX_VALUE ? MAX_VALUE : (int) length;
		ByteArrayOutputStream output = new ByteArrayOutputStream(size);
		FileInputStream stream = null;
		try {
			stream = new FileInputStream(file);
			final byte[] buffer = new byte[8192];
			int read;
			while ((read = stream.read(buffer)) != -1)
				output.write(buffer, 0, read);
		} catch (IOException e) {
			throw new MojoExecutionException("Error reading file: "
					+ getExceptionMessage(e), e);
		} finally {
			if (stream != null)
				try {
					stream.close();
				} catch (IOException e) {
					debug("Exception closing stream", e);
				}
		}

		Blob blob = new Blob().setEncoding(ENCODING_BASE64);
		String encoded = EncodingUtils.toBase64(output.toByteArray());
		blob.setContent(encoded);

		try {
			if (isDebug())
				debug(MessageFormat.format("Creating blob from {0}",
						file.getAbsolutePath()));
			if (!dryRun)
				return service.createBlob(repository, blob);
			else
				return null;
		} catch (IOException e) {
			throw new MojoExecutionException("Error creating blob: "
					+ getExceptionMessage(e), e);
		}
	}

	public void execute() throws MojoExecutionException {
        if (skip) {
            info("Github Site Plugin execution skipped");
            return;
        }

		RepositoryId repository = getRepository(project, repositoryOwner,
				repositoryName);

		if (dryRun)
			info("Dry run mode, repository will not be modified");

		// Find files to include
		String baseDir = outputDirectory.getAbsolutePath();
		String[] includePaths = StringUtils.removeEmpties(includes);
		String[] excludePaths = StringUtils.removeEmpties(excludes);
		if (isDebug())
			debug(MessageFormat.format(
					"Scanning {0} and including {1} and exluding {2}", baseDir,
					Arrays.toString(includePaths),
					Arrays.toString(excludePaths)));
		String[] paths = PathUtils.getMatchingPaths(includePaths, excludePaths,
				baseDir);

		if (paths.length != 1)
			info(MessageFormat.format("Creating {0} blobs", paths.length));
		else
			info("Creating 1 blob");
		if (isDebug())
			debug(MessageFormat.format("Scanned files to include: {0}",
					Arrays.toString(paths)));

		DataService service = new DataService(createClient(host, userName,
				password, oauth2Token, server, settings, session));

		// Write blobs and build tree entries
		List<TreeEntry> entries = new ArrayList<TreeEntry>(paths.length);
		String prefix = path;
		if (prefix == null)
			prefix = "";
		if (prefix.length() > 0 && !prefix.endsWith("/"))
			prefix += "/";

		// Convert separator to forward slash '/'
		if ('\\' == File.separatorChar)
			for (int i = 0; i < paths.length; i++)
				paths[i] = paths[i].replace('\\', '/');

		boolean createNoJekyll = noJekyll;

		for (String path : paths) {
			TreeEntry entry = new TreeEntry();
			entry.setPath(prefix + path);
			// Only create a .nojekyll file if it doesn't already exist
			if (createNoJekyll && NO_JEKYLL_FILE.equals(entry.getPath()))
				createNoJekyll = false;
			entry.setType(TYPE_BLOB);
			entry.setMode(MODE_BLOB);
			entry.setSha(createBlob(service, repository, path));
			entries.add(entry);
		}

		if (createNoJekyll) {
			TreeEntry entry = new TreeEntry();
			entry.setPath(NO_JEKYLL_FILE);
			entry.setType(TYPE_BLOB);
			entry.setMode(MODE_BLOB);

			if (isDebug())
				debug("Creating empty .nojekyll blob at root of tree");
			if (!dryRun)
				try {
					entry.setSha(service.createBlob(repository, new Blob()
							.setEncoding(ENCODING_BASE64).setContent("")));
				} catch (IOException e) {
					throw new MojoExecutionException(
							"Error creating .nojekyll empty blob: "
									+ getExceptionMessage(e), e);
				}
			entries.add(entry);
		}

		Reference ref = null;
		try {
			ref = service.getReference(repository, branch);
		} catch (RequestException e) {
			if (404 != e.getStatus())
				throw new MojoExecutionException("Error getting reference: "
						+ getExceptionMessage(e), e);
		} catch (IOException e) {
			throw new MojoExecutionException("Error getting reference: "
					+ getExceptionMessage(e), e);
		}

		if (ref != null && !TYPE_COMMIT.equals(ref.getObject().getType()))
			throw new MojoExecutionException(
					MessageFormat
							.format("Existing ref {0} points to a {1} ({2}) instead of a commmit",
									ref.getRef(), ref.getObject().getType(),
									ref.getObject().getSha()));

		// Write tree
		Tree tree;
		try {
			int size = entries.size();
			if (size != 1)
				info(MessageFormat.format(
						"Creating tree with {0} blob entries", size));
			else
				info("Creating tree with 1 blob entry");
			String baseTree = null;
			if (merge && ref != null) {
				Tree currentTree = service.getCommit(repository,
						ref.getObject().getSha()).getTree();
				if (currentTree != null)
					baseTree = currentTree.getSha();
				info(MessageFormat.format("Merging with tree {0}", baseTree));
			}
			if (!dryRun)
				tree = service.createTree(repository, entries, baseTree);
			else
				tree = new Tree();
		} catch (IOException e) {
			throw new MojoExecutionException("Error creating tree: "
					+ getExceptionMessage(e), e);
		}

		// Build commit
		Commit commit = new Commit();
		commit.setMessage(message);
		commit.setTree(tree);

        try {
            UserService userService = new UserService(service.getClient());
            User user = userService.getUser();

            CommitUser author = new CommitUser();
            author.setName(user.getName());
            author.setEmail(userService.getEmails().get(0));
            author.setDate(new GregorianCalendar().getTime());

            commit.setAuthor(author);
            commit.setCommitter(author);
        } catch (IOException e) {
            throw new MojoExecutionException("Error retrieving user info: "
                    + getExceptionMessage(e), e);
        }

		// Set parent commit SHA-1 if reference exists
		if (ref != null)
			commit.setParents(Collections.singletonList(new Commit().setSha(ref
					.getObject().getSha())));

		Commit created;
		try {
			if (!dryRun)
				created = service.createCommit(repository, commit);
			else
				created = new Commit();
			info(MessageFormat.format("Creating commit with SHA-1: {0}",
					created.getSha()));
		} catch (IOException e) {
			throw new MojoExecutionException("Error creating commit: "
					+ getExceptionMessage(e), e);
		}

		TypedResource object = new TypedResource();
		object.setType(TYPE_COMMIT).setSha(created.getSha());
		if (ref != null) {
			// Update existing reference
			ref.setObject(object);
			try {
				info(MessageFormat.format(
						"Updating reference {0} from {1} to {2}", branch,
						commit.getParents().get(0).getSha(), created.getSha()));
				if (!dryRun)
					service.editReference(repository, ref, force);
			} catch (IOException e) {
				throw new MojoExecutionException("Error editing reference: "
						+ getExceptionMessage(e), e);
			}
		} else {
			// Create new reference
			ref = new Reference().setObject(object).setRef(branch);
			try {
				info(MessageFormat.format(
						"Creating reference {0} starting at commit {1}",
						branch, created.getSha()));
				if (!dryRun)
					service.createReference(repository, ref);
			} catch (IOException e) {
				throw new MojoExecutionException("Error creating reference: "
						+ getExceptionMessage(e), e);
			}
		}
	}
}
