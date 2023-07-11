#!/bin/bash

# not Fail on any error.
set +e

bazel build --noincremental_dexing testdpc
