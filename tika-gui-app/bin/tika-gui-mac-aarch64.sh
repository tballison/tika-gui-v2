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


if [ ! -d "jre/zulu17.44.53-ca-fx-jre17.0.5-macosx_aarch64" ]
then
  echo "Unpacking zipped jre."
  ditto -xk jre/zulu17.44.53-ca-fx-jre17.0.7-macosx_aarch64.zip jre
  mv jre/zulu17.44.53-ca-fx-jre17.0.7-macosx_aarch64/* jre
fi

#this is barely a start. Initially targeting linux

#TODO: update the script to find the executable, whether it
# is under the jdk name or the aarch Contents/Home/ stuff.
# Can we do anything better than chmod?

JAVA_HOME="jre/zulu-17.jre/Contents/Home"
JAVA_BIN="$JAVA_HOME/bin"

JAVA="$JAVA_BIN/java"
$JAVA -DTIKA_GUI_JAVA_HOME=$JAVA_BIN -jar lib/tika-gui-app.jar