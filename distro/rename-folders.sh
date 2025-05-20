#!/bin/bash

base_dir="$1"  # Take the first argument as the base_dir

cd "$base_dir" || {
  echo "Directory not found: $base_dir"
  exit 1
}

# Iterate over each top-level folder (like addresshierarchy, ampathforms, etc.)
for module_dir in */ ; do
  module_dir=${module_dir%/}

  # Enter each module directory
  cd "$base_dir/$module_dir" || continue

  for sub_dir in */ ; do
    sub_dir=${sub_dir%/}
    case "$sub_dir" in "$2")
        mv "$sub_dir" "00_$sub_dir"
        ;;
      "$3")
        mv "$sub_dir" "01_$sub_dir"
        ;;
      "$4")
        mv "$sub_dir" "02_$sub_dir"
        ;;
      "$5")
        mv "$sub_dir" "03_$sub_dir"
        ;;
      "$6")
        mv "$sub_dir" "04_$sub_dir"
        ;;
      # Add more renaming logic here if needed
      *)
        echo "No rename rule for $module_dir/$sub_dir"
        ;;
    esac
  done
done
