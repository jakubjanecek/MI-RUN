#!/bin/bash

cd out

echo "FirstTest"
java Claus < ../test/FirstTest.claus

echo "Knapsack03"
java vm.Claus < ../test/Knapsack03.claus

cd ..
