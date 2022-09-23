# tika-gui-v2
Unofficial user interface for Apache Tika

This is an initial draft of the next generation user interface application for Apache Tika.

# Initial Design Thoughts
This breaks substantially from Tika's current user interface and some of the initial
of Apache Tika.

This will make use of the tika-pipes modules to enable fetching files from 
local file shares, S3 and other resources and then emitting the parsed output
to local file shares, S3, OpenSearch, Apache Solr, etc.

Ideally, there will be some user interface to visualize information from
the a run of Tika.
