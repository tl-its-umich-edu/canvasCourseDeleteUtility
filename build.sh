#!/bin/sh --

# Helper script for use on build server.
# Debugging: -x to enable, +x to disable
# -e: the shell shall immediately exit in case of failure of list of commands
set -xe
timestamp=$(date +%Y%m%d%H%M%S)

cd target

#GIT_BRANCH =origin/TLUNIZIN-424 or origin/master jenkins environmental variable to get git branch
branch=${GIT_BRANCH}
if [ -n "$branch" ]; then
btemp=$(basename ${branch} /)
else
btemp="local"
fi

# creating artifact folder
if [ ! -d "artifact" ]; then
    ## no artifact folder
    ## create such folder first
    echo "create artifact folder"
    mkdir artifact
    chmod -R 755 artifact
fi

#Git commit version
gitNum=`git log -n 1 --pretty="format:%h"`

echo "Build# $BUILD_NUMBER | $GIT_URL | $gitNum | $btemp | $BUILD_ID" > version.txt


## construct new tar file
find . -maxdepth 2 -type f \( -name "version.txt" -o -name "canvasCourseDeleteUtility.jar" \) -exec tar -rf ./artifact/CCDU_${btemp}_${gitNum}_${timestamp}.tar {} \;
