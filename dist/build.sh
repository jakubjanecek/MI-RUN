#!/bin/bash

rm -rf out/*
cp -R src/vm out
cp -R test/vm out
cp test/input.dat out/input.dat
./javacc/bin/jjtree -NOSTATIC -OUTPUT_DIRECTORY=out src/claus_parser.jj
./javacc/bin/javacc -NOSTATIC -OUTPUT_DIRECTORY=out out/claus_parser.jj

javac -cp .:junit-4.10.jar:junit-dep-4.10.jar out/vm/mm/*.java out/vm/*.java out/*.java
