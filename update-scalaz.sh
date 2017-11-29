#!/bin/bash

git submodule update --init || exit 3
(cd lib/scalaz && sbt publishLocal) || exit 3
