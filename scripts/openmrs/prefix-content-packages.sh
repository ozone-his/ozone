#!/bin/bash

# Given the below folder structure
#base_dir/
#├── module1/
#│   ├── alpha/
#│   ├── beta/
#│   └── gamma/
#├── module2/
#│   ├── beta/
#│   ├── delta/
#│   └── alpha/

# The script will change the above folder structure when the following `content-packages.txt` is passed to it:
# -> content-packages.txt:
#  alpha
#  beta
#  gamma
#
# into:
#
#base_dir/
#├── module1/
#│   ├── 00_alpha/
#│   ├── 01_beta/
#│   └── 02_gamma/
#├── module2/
#│   ├── 00_alpha/
#│   ├── 01_beta/
#│   ├── delta/            ← Not renamed, not in content-packages.txt

base_dir="$1"       # First argument is the base directory
order_file="$2"     # Second argument is the text file with the ordering

if [[ ! -d "$base_dir" ]]; then
  echo "Directory not found: $base_dir"
  exit 1
fi

if [[ ! -f "$order_file" ]]; then
  echo "Order file not found: $order_file"
  exit 1
fi

cd "$base_dir" || exit 1

# Read the list of target directories from the order file into an array
while read -r line; do
  order+=("$line")
done < $order_file

# Iterate over each top-level folder (like addresshierarchy, ampathforms, etc.)
for module_dir in */ ; do
  module_dir=${module_dir%/}
  cd "$base_dir/$module_dir" || continue

  # Loop through each subdirectory (Eg. referenceapplication, referenceapplication-demo) in the module
  for sub_dir in */ ; do
    sub_dir=${sub_dir%/}

    matched=false
    index=0

    # Loop through the each line in order file and prefix the sub directory with appropriate number
    for target_dir in "${order[@]}"; do
      padded_index=$(printf "%02d" "$index")
      if [[ "$sub_dir" == "$target_dir" ]]; then
        mv "$sub_dir" "${padded_index}_$sub_dir"
        matched=true
        break
      fi
      ((index++))
    done

  done
done