# GitHub Maven Plugins [![Build Status](https://travis-ci.org/github/maven-plugins.svg)](https://travis-ci.org/github/maven-plugins)

Collection of [Maven](http://maven.apache.org/) plugins that integrate with GitHub.
These plugins are built on top of [API v3](http://developer.github.com/) through the
[GitHub Java library](https://github.com/eclipse/egit-github/tree/master/org.eclipse.egit.github.core).

Released builds are available from [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Ccom.github.github).

## Core Configuration

The plugins support several configuration options that can either be expressed
in your project's POM file or in your `settings.xml` file. Where you put the
plugin settings depends on whether you want a specific setting to be configured
globally or on a per-project basis.

All GitHub Maven plugins support the following core configuration elements.

The notation below shows the plugin configuration property name followed
by the settings configuration property in parentheses.

* `host` (`github.global.host`)
  * Domain of GitHub API calls (defaults to `api.github.com`)
* `oauth2Token` (`github.global.oauth2Token`)
  * OAuth2 access token for API authentication
  * [More about GitHub OAuth support](http://developer.github.com/v3/oauth/)
* `userName` (`github.global.userName`)
  * GitHub user name used for API authentication
* `password` (`github.global.password`)
  * GitHub password used for API authentication
* `server` (`github.global.server`)
  * Id of the `server` element from the `settings.xml`. To use standard authentication
    set  the `username` and `password` elements in the `servers` section of your
    `settings.xml` file along with an `id`. Configure an OAuth2 token by leaving the
    `username` element blank/missing and just specify the token in the `password` element.
  * This option should be used **instead of** configuring any of `userName`, `password`
    or `oauth2Token` in the plugin `configuration` element or as a properties.
* `repositoryName`
  * Name of repository
* `repositoryOwner`
  * Owner of repository

*Note:* `repositoryOwner` property and `repositoryName` are optional and will be
inferred from the following properties if not specified

 * `project.scm.url`
 * `project.scm.connection`
 * `project.scm.developerConnection`
 * `project.url`

### Authentication Example

#### settings.xml

```xml
<servers>
  <server>
    <id>github</id>
    <username>GitHubLogin</username>
    <password>GitHubPassw0rd</password>
  </server>
</servers>
```
or
```xml
<servers>
  <server>
    <id>github</id>
    <password>OAUTH2TOKEN</password>
  </server>
</servers>
```

#### pom.xml

```xml
<properties>
  <github.global.server>github</github.global.server>
</properties>
```

## [Site Plugin](http://github.github.com/maven-plugins/site-plugin)
Maven plugin that commits files generated and updates a specific branch
reference in a GitHub repository.  This plugin can be used to deploy a created
Maven site to a `gh-pages` branch so that it can be served statically as a
GitHub Project Page.  The plugin has a `site` goal and is configured with a goal
prefix of `ghSite`.

### Configuration

* `branch`
  * Branch ref that will be updated to commit made
  * Default: `refs/heads/gh-pages`
* `message`
  * Message used for commit
* `outputDirectory`
  * Directory that includes and excludes will be relative to
  * Defaults to `siteOutputDirectory` or `project.reporting.outputDirectory`
* `includes`
  * Sub-elements will be treated as patterns to include from the
    `outputDirectory`
* `excludes`
  * Sub-elements will be treated as patterns to exclude from the
    `outputDirectory`
* `path`
  * Path relative to the root of the repository that all blobs should be
    relative to
* `force` (`github.site.force`)
  * `true` | `false` (default: `false`)
  * Whether to force a ref update, default is fast-forwards only
* `merge` (`github.site.merge`)
  * `true` | `false` (default: `false`)
  * Whether to merge with the current tree or completely replace the tree that
    the commit points to
* `dryRun` (`github.site.dryRun`)
  * `true` | `false` (default: `false`)
  * Log what blobs, tree, and commits *would* be created without actually
    creating them
* `noJekyll` (`github.site.noJekyll`)
  * `true` | `false` (default: `false`)
  * Whether to always create a `.nojekyll` file at the root of the site if one
    doesn't already exist.  This setting should be enabled if your site contains
    any folders that begin with an underscore.

### Example
```xml
<build>
  <plugins>
    <plugin>
      <groupId>com.github.github</groupId>
      <artifactId>site-maven-plugin</artifactId>
      <version>0.12</version>
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
