# GitHub Maven Plugins
Collection of [Maven](http://maven.apache.org/) plugins that integrate with GitHub.
These plugins are built on top [API v3](http://developer.github.com/) through the
[GitHub Java library](https://github.com/eclipse/egit-github/tree/master/org.eclipse.egit.github.core).

You can obtain the plugins by adding the following plugin repository element to your `pom.xml` file:

```xml
<pluginRepositories>
  <pluginRepository>
    <id>oss-sonatype-snapshots</id>
    <url>https://oss.sonatype.org/content/repositories/snapshots/</url>
  </pluginRepository>
</pluginRepositories>
```

## Downloads Plugin
Maven plugin that creates and uploads built resources to be available as 
GitHub repository downloads.  The plugin has an `upload` goal and is configured with
a prefix of `ghDownloads`. The plugin will upload the single build artifact by default
but can be reconfigured to include/exclude files found in the build directory or to
include attached artifacts such as Javadoc jars or sources jars.

### Configuration
The downloads plugin supports several configuration options that can either
be expressed in your project's POM file or in your settings.xml file.  Where
you put the downloads-plugin settings depends on whether you want a specific
setting to be configured globally or on a per-project basis.

The notation belows shows the plugin configuration property name followed
by the settings configuration property in parentheses.

* host (github.downloads.host)
  * Domain of GitHub API calls (defaults to `api.github.com`)
* oauth2Token (github.downloads.oauth2token)
  * OAuth2 access token for API authentication
  * [More about GitHub OAuth support](http://developer.github.com/v3/oauth/)
* userName (github.downloads.userName)
  * GitHub user name used for API authentication
* password (github.downloads.password)
  * GitHub password used for API authentication
* description
  * Description visible on the repository download page
* includes
  * Sub-elements will be treated as patterns to include from the `project.build.directory` as downloads
  * This element is optional and will default to create a download of the build's main artifact
* excludes
  * Sub-elements will be treated as patterns to exclude from the `project.build.directory` as downloads
  * This element is optional and will default to create a download of the build's main artifact
* includeAttached (github.downloads.includeAttached)
  * true | false (default: false)
  * Whether to create downloads from attached artifacts, by default only the main artifact is uploaded
* dryRun (github.downloads.dryRun)
  * true | false (default: false)
  * Log what files *would* be uploaded and what existing downloads *would* be deleted without actually modifying the current downloads
* override (github.downloads.override)
  * true | false (default: false)
  * Whether existing downloads with the same name will be deleted before attempting to upload a new version
  * *Note:* Attempts to upload a download with the same name as one that already exists will fail unless this is set to true
* repositoryName
  * Name of repository that downloads will be uploaded to
* repositoryOwner
  * Owner of repository that downloads will be uploaded to

*Note:* `repositoryOwner` property and `repositoryName` are optional and will be inferred from the following properties if not specified

 * `project.url`
 * `project.scm.url`
 * `project.scm.connection`
 * `project.scm.developerConnection`

### Example
```xml
<build>
  <plugins>
    <plugin>
      <groupId>com.github.maven.plugins</groupId>
      <artifactId>github-downloads-plugin</artifactId>
      <version>0.0.2</version>
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

#License
* [MIT License](http://www.opensource.org/licenses/mit-license.php)
