#!/bin/sh --

# Helper script for use on build server.
# Debugging: -x to enable, +x to disable
# -e: the shell shall immediately exit in case of failure of list of commands
set -xe
timestamp=$(date +%Y%m%d%H%M%S)
cd target
rm original*.jar
filename=$(ls *.jar | head -1)
targetFilename=$(basename ${filename} .jar)
#GIT_BRANCH =origin/TLUNIZIN-424 or origin/master jenkins environmental variable to get git branch
branch=${GIT_BRANCH}
if [ -n "$branch" ]; then
btemp=$(basename ${branch} /)
else
btemp="local"
fi
mv ${targetFilename}.jar ${targetFilename}.${btemp}.${timestamp}.jar
