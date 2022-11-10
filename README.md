# tika-gui-v2

[![license](https://img.shields.io/github/license/apache/tika.svg?maxAge=2592000)](http://www.apache.org/licenses/LICENSE-2.0)

Unofficial next gen user interface for Apache Tika

This is an initial draft of the next generation user interface application for Apache Tika.
This is not part of the Apache Tika project.

# Getting Started
Download the .zip file in the releases section for your operating system.

## Windows
 * Unzip then run `tika-gui.bat`

## Mac
 * Unzip then `chmod u+x tika-gui-mac.sh`
 * `./tika-gui-mac.sh`
*NOTE* I've been able to run the mac distro on a mac aarch64.  There's a warning about javafx stuff, but it seems to work


## Linux
 * Unzip then `chmod u+x tika-gui.sh`
 * `./tika-gui.sh`

# Program Maturity
This is just the beginning.  Everything is still in a state of flux and is subject to change. 

***HELP WANTED!!!***

# Initial Design Thoughts
This breaks substantially from Tika's current user interface.

This will make use of the tika-pipes modules to enable fetching files from 
local file shares, S3 and other resources and then emitting the parsed output
to local file shares, S3, OpenSearch, Apache Solr, jdbc, etc.

This requires Java 17 and will not run Tika "in process", but rather 
it will rely on forking Tika. 

My intention is to start with Java 17 and keep Java 17 for a good long while.  I'm currently
bundling non-JavaFX jres with each platform bundle.  We might move to java distros that include
java fx so that we don't have to include the jars and native bits via the javafx dependencies
ourselves.

There's quite a bit of work to streamline the releases so that we don't have 16 copies of
jackson-databind, for example...

Ideally, there will be some user interface to visualize information from
a run of Tika via tika-eval.
