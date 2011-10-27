# GitHub Maven Plugins

Collection of [Maven](http://maven.apache.org/) plugins that integrate with GitHub.
These plugins are built on top of [API v3](http://developer.github.com/) through the
[GitHub Java library](https://github.com/eclipse/egit-github/tree/master/org.eclipse.egit.github.core).

Released builds are available from [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Ccom.github.github)
and snapshots can be obtained by adding the following plugin repository element to
your `pom.xml` file:

```xml
<pluginRepositories>
  <pluginRepository>
    <id>oss-sonatype-snapshots</id>
    <url>https://oss.sonatype.org/content/repositories/snapshots/</url>
  </pluginRepository>
</pluginRepositories>
```

## Core Configuration

The plugins support several configuration options that can either be expressed
in your project's POM file or in your settings.xml file. Where you put the
plugin settings depends on whether you want a specific setting to be configured
globally or on a per-project basis.

All GitHub Maven plugins support the following core configuration elements.

The notation below shows the plugin configuration property name followed
by the settings configuration property in parentheses.

* host (github.global.host)
  * Domain of GitHub API calls (defaults to `api.github.com`)
* oauth2Token (github.global.oauth2Token)
  * OAuth2 access token for API authentication
  * [More about GitHub OAuth support](http://developer.github.com/v3/oauth/)
* userName (github.global.userName)
  * GitHub user name used for API authentication
* password (github.global.password)
  * GitHub password used for API authentication
* repositoryName
  * Name of repository that downloads will be uploaded to
* repositoryOwner
  * Owner of repository that downloads will be uploaded to

*Note:* `repositoryOwner` property and `repositoryName` are optional and will be
inferred from the following properties if not specified

 * `project.url`
 * `project.scm.url`
 * `project.scm.connection`
 * `project.scm.developerConnection`

## [Downloads Plugin](http://github.github.com/maven-plugins/downloads-plugin)
Maven plugin that creates and uploads built resources to be available as 
GitHub repository downloads.  The plugin has an `upload` goal and is configured
with a goal prefix of `ghDownloads`. The plugin will upload the single build
artifact by default but can be reconfigured to include/exclude files found in
the build directory or to include attached artifacts such as Javadoc jars or
sources jars.

### Configuration

* description
  * Description visible on the repository download page
* includes
  * Sub-elements will be treated as patterns to include from the
    `project.build.directory` as downloads
  * This element is optional and will default to create a download of the
    build's main artifact
* excludes
  * Sub-elements will be treated as patterns to exclude from the
    `project.build.directory` as downloads
  * This element is optional and will default to create a download of the
    build's main artifact
* includeAttached (github.downloads.includeAttached)
  * true | false (default: false)
  * Whether to create downloads from attached artifacts, by default only the
    main artifact is uploaded
* dryRun (github.downloads.dryRun)
  * true | false (default: false)
  * Log what files *would* be uploaded and what existing downloads *would* be
    deleted without actually modifying the current downloads
* override (github.downloads.override)
  * true | false (default: false)
  * Whether existing downloads with the same name will be deleted before
    attempting to upload a new version
  * *Note:* Attempts to upload a download with the same name as one that already
    exists will fail unless this is set to true
* suffix
  * String to be appended after file name but before file extension for uploaded files
  * A suffix of `-master` would cause artifacts to be uploaded as `myartifact-1.0-master.jar`

### Example
```xml
<build>
  <plugins>
    <plugin>
      <groupId>com.github.github</groupId>
      <artifactId>downloads-maven-plugin</artifactId>
      <version>0.3</version>
      <configuration>
        <description>${project.version} release of ${project.name}</description>
        <override>true</override>
        <includeAttached>true</includeAttached>
      </configuration>
    </plugin>
  </plugins>
</build>
```

To upload a built artifact run the following command:

`$ mvn clean install ghDownloads:upload`

You can also bind the upload goal to execute as part of a specific phase:

```xml
<executions>
  <execution>
    <goals>
      <goal>upload</goal>
    </goals>
    <phase>install</phase>
  </execution>
</executions>
```

## [Site Plugin](http://github.github.com/maven-plugins/site-plugin)
Maven plugin that commits files generated and updates a specific branch
reference in a GitHub repository.  This plugin can be used to deploy a created
Maven site to a `gh-pages` branch so that it can be served statically as a
GitHub Project Page.  The plugin has a `site` goal and is configured with a goal
prefix of `ghSite`.

### Configuration

* branch
  * Branch ref that will be updated to commit made
  * Default: `refs/heads/gh-pages`
* message
  * Message used for commit
* outputDirectory
  * Directory that includes and excludes will be relative to
  * Defaults to `siteOutputDirectory` or `project.reporting.outputDirectory`
* includes
  * Sub-elements will be treated as patterns to include from the
    `outputDirectory`
* excludes
  * Sub-elements will be treated as patterns to exclude from the
    `outputDirectory`
* path
  * Path relative to the root of the repository that all blobs should be
    relative to
* force (github.site.force)
  * true | false (default: false)
  * Whether to force a ref update, default is fast-forwards only
* merge (github.site.merge)
  * true | false (default: false)
  * Whether to merge with the current tree or completely replace the tree that
    the commit points to
* dryRun (github.site.dryRun)
  * true | false (default: false)
  * Log what blobs, tree, and commits *would* be created without actually
    creating them

### Example
```xml
<build>
  <plugins>
    <plugin>
      <groupId>com.github.github</groupId>
      <artifactId>site-maven-plugin</artifactId>
      <version>0.3</version>
      <configuration>
        <message>Creating site for ${project.version}</message>
      </configuration>
      <executions>
        <execution>
          <goals>
            <goal>site</goal>
          </goals>
          <phase>site</phase>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

To commit a created site run the following command:

`$ mvn site`

# License
* [MIT License](http://www.opensource.org/licenses/mit-license.php)
