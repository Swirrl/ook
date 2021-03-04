#! /bin/bash

#circle doesn't allow interpolating in its environemnt command
export CIRCLE_BRANCH_ESCAPED="${CIRCLE_BRANCH//\//_}"
export BRANCH_ESCAPED=$CIRCLE_BRANCH_ESCAPED
export SHORT_SHA=`echo $CIRCLE_SHA1|cut -c1-6`
# note that circle build num is the job id (not workflow), but we currently do build and publish in one job.
export BUILD_NUMBER="circle_${CIRCLE_BUILD_NUM}_${SHORT_SHA}"
