#!/bin/sh --

# Helper script for use on build server.
# Debugging: -x to enable, +x to disable
# -e: the shell shall immediately exit in case of failure of list of commands
set -xe
timestamp=$(date +%Y%m%d%H%M%S)
cd target

# Removing original-canvasCourseDeleteUtility.jar as this is redundant and created as part of <maven-shade-plugin> when creating a uber/fat jar
#rm original*.jar
#filename=$(ls *.jar | head -1)
#targetFilename=$(basename ${filename} .jar)
#GIT_BRANCH =origin/TLUNIZIN-424 or origin/master jenkins environmental variable to get git branch
#branch=${GIT_BRANCH}
#if [ -n "$branch" ]; then
#btemp=$(basename ${branch} /)
#else
#btemp="local"
#fi
#mv ${targetFilename}.jar ${targetFilename}.${btemp}.${timestamp}.jar

echo "shell env variable BUILD_NUMBER is ${BUILD_NUMBER}"
build_number=${BUILD_NUMBER:=1}

if [ ! -d "artifact" ]; then
    ## no artifact folder
    ## create such folder first
    echo "create artifact folder"
    mkdir artifact
    chmod -R 755 artifact
fi
echo "Generating zip file for build number: ${build_number}"

gitNum=`git log -n 1 --pretty="format:%h"`
printf 'github version number is %s.' $gitNum > git_version.txt

## construct new tar file
#find . -maxdepth 2 -type f \( -name "*_version.txt" -o -name "canvasCourseDeleteUtility.jar" \) -exec tar -rf ./artifact/CCDU_$build_number_$gitNum.tar {} \;
