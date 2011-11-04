#!/bin/bash

rm -rf out/*
./javacc/bin/jjtree -NOSTATIC -OUTPUT_DIRECTORY=out src/grammar.jjt
./javacc/bin/javacc -NOSTATIC -OUTPUT_DIRECTORY=out out/grammar.jj
javac out/*.java