#!/usr/bin/env bash

print_help() {
  echo "$0 <new package name> <project name> <modid>"
}

if [ "$#" -ne "3" ]; then
  print_help
  exit 0
fi

base=$(dirname "$(readlink -f "$0")")
echo "Updating $base"
package_name="$1"
project_name="$2"
modid="$3"
package_dir=$(echo "$package_name" | tr . /)
echo "Setting package name to $package_name"
echo "Setting project name to $project_name"
echo "Setting package dir to $package_dir"
echo "Setting mod id to $modid"
(
  set -x
  find "$base"/src/main -type f -exec sed -i s/examplemod/"$modid"/g\;s/com.example/"$package_name"/g {} +
  sed -i s/com.example/"$package_name"/g\;s/examplemod/"$modid"/g "$base"/gradle.properties
  sed -i s/examplemod/"$project_name"/g "$base"/settings.gradle.kts
  mkdir -p "$base"/src/main/java/"$package_dir"
  mkdir -p "$base"/src/main/kotlin/"$package_dir"
  mv "$base"/src/main/java/com/example/* "$base"/src/main/java/"$package_dir"
  mv "$base"/src/main/kotlin/com/example/* "$base"/src/main/kotlin/"$package_dir"
  mv "$base"/src/main/resources/mixins.examplemod.json "$base"/src/main/resources/mixins."$modid".json
  rm "$base"/.github/workflows/init.yml
  rm "$(readlink -f $0)"
)
echo "All done"
echo "Now go commit those changes"
