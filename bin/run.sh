#!/bin/env bash

echo "Updating youtube-dl..."
./youtube-dl --update
echo "Starting music-get..."
java -Xmx2500m -jar music-get.jar 
