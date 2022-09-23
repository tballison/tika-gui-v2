# tika-gui-v2

[![license](https://img.shields.io/github/license/apache/tika.svg?maxAge=2592000)](http://www.apache.org/licenses/LICENSE-2.0)

Unofficial next gen user interface for Apache Tika

This is an initial draft of the next generation user interface application for Apache Tika.
This is not part of the Apache Tika project.

# Initial Design Thoughts
This breaks substantially from Tika's current user interface and some of the initial
of Apache Tika.

This will make use of the tika-pipes modules to enable fetching files from 
local file shares, S3 and other resources and then emitting the parsed output
to local file shares, S3, OpenSearch, Apache Solr, etc.

This requires Java 17 and will not run Tika "in process", but rather rely
on forking it.

Ideally, there will be some user interface to visualize information from
a run of Tika.
