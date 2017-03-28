#!/bin/sh

README="README.md" 

mvn versions:set
mvn versions:commit

echo "Replacing branch tags in $README..."
sed -i -f tools/release/badgeBranch.sed "$README"

echo "Done"
