#!/bin/bash

cd out
echo "-------------------"
echo "[FirstTest]:"
java Claus < ../test/FirstTest.claus
echo "-------------------"
echo "[Knapsack03]:"
java Claus < ../test/Knapsack03.claus
echo "-------------------"
cd ..
