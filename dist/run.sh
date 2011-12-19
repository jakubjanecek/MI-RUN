#!/bin/bash

cd out
echo ""
echo "---------------------------------------------------------"
echo ""
echo "[Parser]:"
echo ""
java Claus < ../test/FirstTest.claus
echo ""
echo "---------------------------------------------------------"

echo ""
echo "---------------------------------------------------------"
echo ""
echo "KnapsackManual:"
java vm.KnapsackManual
echo ""
echo ""
echo "---------------------------------------------------------"

echo ""
echo "---------------------------------------------------------"
echo ""
echo "Unit tests:"
java -cp .:../junit-4.10.jar:../junit-dep-4.10.jar org.junit.runner.JUnitCore vm.ClausVMTest
java -cp .:../junit-4.10.jar:../junit-dep-4.10.jar org.junit.runner.JUnitCore vm.mm.MMTest
java -cp .:../junit-4.10.jar:../junit-dep-4.10.jar org.junit.runner.JUnitCore vm.mm.GCTest
echo ""
echo ""
echo "---------------------------------------------------------"


cd ..