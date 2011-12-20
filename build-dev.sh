#!/bin/bash

rm -rf out/*
cp -R src/vm out
./javacc/bin/jjtree -NOSTATIC -OUTPUT_DIRECTORY=out src/claus_parser.jjt
./javacc/bin/javacc -NOSTATIC -OUTPUT_DIRECTORY=out out/claus_parser.jj
javac out/vm/mm/*.java out/vm/*.java out/*.java
