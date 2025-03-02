# tika-gui-v2

[![license](https://img.shields.io/github/license/apache/tika.svg?maxAge=2592000)](http://www.apache.org/licenses/LICENSE-2.0)

Unofficial next gen user interface for Apache Tika

This is an initial draft of the next generation user interface application for Apache Tika.
This is not part of the Apache Tika project.

# Getting Started
Download the .zip file in the [releases section](https://github.com/tballison/tika-gui-v2/releases) for your operating system.

## Windows
 * Unzip then run `tika-gui.bat`

## Mac
 * Unzip then run `./tika-gui-mac-x86.sh` or `./tika-gui-mac-aarch64.sh`

*NOTE* I've been able to run the mac distro on a mac aarch64.  There's a warning about javafx stuff, but it seems to work

## Linux
 * Unzip then `./tika-gui.sh`

# Requirements
This project requires a Java 21 jdk/jre with java-fx built in! We use Azul's Zulu jdk-fx for development, and we bundle Zulu jre-fx with the release artifacts.  To download: 
[www.azul.com](https://www.azul.com/downloads/?version=java-21-lts&package=jdk-fx#download-openjdk).

# Program Maturity
This is just the beginning. Everything is still in a state of flux and is subject to change. We may abandon
the whole thing and do something in Electron...

***HELP WANTED!!!***

# Initial Design Thoughts
This breaks substantially from Tika's current user interface.

This will make use of the tika-pipes modules to enable fetching files from 
local file shares, S3 and other resources and then emitting the parsed output
to local file shares, S3, OpenSearch, Apache Solr, jdbc, etc.

This requires Java 21 (with built-in java-fx) and will not run Tika "in process", but rather 
it will rely on forking Tika. 

There's quite a bit of work to streamline the releases so that we don't have 16 copies of
jackson-databind, for example...

Ideally, there will be some user interface to visualize information from
a run of Tika via tika-eval.

# Version Notes
* 1.0.0-BETA7 -- updated dependencies. Now requires Java 21.
* 1.0.0-BETA6 -- same warning as BETA5 -- only use this as a demo until commons-compress regressions have been fixed.
* 1.0.0-BETA5 -- this has known regressions in commons-compress and should be used for demo purposes only.

# Release Notes
At some point, we should use jreleaser.  We aren't yet doing that.

To generate the download sections of the pom, run DownloadPluginUpdater and PackageBinaries.

The release is currently triggered by pushing a tag starting with 'v'.

Before running this, change the version to the version you intend to release.

```git tag -a "v1.0.0-BETA5" -m "v1.0.0-BETA5 release" && git push origin v1.0.0-BETA5```

Change the version back to the development/SNAPSHOT version.

If you need to re-release, delete the tag:
```git push --delete origin v1.0.0-BETA5 && git tag --delete v1.0.0-BETA5```

We should figure out how to sign artifacts and offer sha256s.  Again, see jreleaser above.

Further, short of going the full jreleaser route, we should start using: appassembler-maven-plugin.

