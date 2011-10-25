#!/bin/bash

rm -rf out/*
./javacc/bin/javacc -NOSTATIC -OUTPUT_DIRECTORY=out src/grammar.jj
javac out/*.java