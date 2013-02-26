#!/bin/bash
#echo "Please type in the project number you want to test:"
#read projnum
echo "Please type in the number of the question you want to test: "
read question
echo "Please type in the test you want to run: "
read testnum

ARGUMENT="p$question"
cd proj1 && make; nachos -@! $ARGUMENT testnum