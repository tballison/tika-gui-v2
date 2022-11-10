# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#this is barely a start. Initially targeting linux

#TODO: update the script to find the executable, whether it
# is under the jdk name or the aarch Home/ stuff.
# Can we do anything better than chmod?

JAVA_HOME="jre/jdk-17.0.5+8-jre"
JAVA_BIN="$JAVA_HOME/bin"

JAVA="$JAVA_BIN/java"
chmod u+x $JAVA && \
chmod u+x $JAVA_HOME/lib/jspawnhelper && \
$JAVA -DTIKA_GUI_JAVA_HOME=$JAVA_BIN -jar lib/tika-gui-app.jar