#!/bin/bash

rm -rf out/*
./javacc/bin/jjtree -NOSTATIC -OUTPUT_DIRECTORY=out src/dev/grammar-claus.jjt
./javacc/bin/javacc -NOSTATIC -OUTPUT_DIRECTORY=out out/grammar-claus.jj
javac out/*.java