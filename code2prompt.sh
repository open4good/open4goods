#!/bin/bash
# Usage: script.sh <directory> [output_file]
# This script lists files and directories in <directory> (ignoring hidden files).
# It includes files in the root folder and recursively processes only the first-level subdirectories
# that match allowed names (by default: src and test). It then prints a directory tree followed by
# file contents (for files with allowed extensions: .md, .java, .yml, .xml).

# Show help if no arguments are provided.
if [ $# -lt 1 ]; then
    echo "Usage: $0 <directory> [output_file]"
    echo "Recursively lists files and outputs the directory structure and file contents."
    exit 1
fi

TARGET_DIR="$1"
OUTPUT_FILE="$2"

# Verify TARGET_DIR exists and is a directory.
if [ ! -d "$TARGET_DIR" ]; then
    echo "Error: '$TARGET_DIR' is not a valid directory."
    exit 1
fi

# Configurable allowed file extensions (without the dot)
ALLOWED_EXTENSIONS=("md" "java" "yml" "xml")

# Configurable allowed first-level directories (only these folders will be recursed)
ALLOWED_FOLDERS=("src" "test")

# If output file provided, redirect stdout (overwriting)
if [ -n "$OUTPUT_FILE" ]; then
    exec > "$OUTPUT_FILE"
fi

#############################################
# Function: Recursively print directory tree.
# Parameters:
#   $1 - directory to process
#   $2 - current indentation (e.g. "-" or "--")
#############################################
print_tree_rec() {
    local current_dir="$1"
    local indent="$2"
    for item in "$current_dir"/*; do
        [ -e "$item" ] || continue
        # Skip hidden files and folders.
        if [[ "$(basename "$item")" == .* ]]; then
            continue
        fi
        echo "${indent}- $(basename "$item")"
        if [ -d "$item" ]; then
            print_tree_rec "$item" "${indent}-"
        fi
    done
}

#############################################
# Output the directory structure
#############################################
echo "* Directory structure *"

# Process files in the root folder.
for item in "$TARGET_DIR"/*; do
    [ -e "$item" ] || continue
    if [ -f "$item" ] && [[ "$(basename "$item")" != .* ]]; then
         echo "- $(basename "$item")"
    fi
done

# Process allowed first-level subdirectories.
for item in "$TARGET_DIR"/*; do
    [ -e "$item" ] || continue
    if [ -d "$item" ] && [[ "$(basename "$item")" != .* ]]; then
        base=$(basename "$item")
        # Check if this first-level folder is allowed.
        if [[ " ${ALLOWED_FOLDERS[@]} " =~ " $base " ]]; then
            echo "- $base"
            print_tree_rec "$item" "--"
        fi
    fi
done

echo ""
echo "* File content *"
echo ""

#############################################
# Function: Check if a file's extension is allowed.
# Parameter:
#   $1 - file path
# Returns:
#   0 (true) if allowed; 1 (false) otherwise.
#############################################
has_allowed_extension() {
    local filename="$1"
    local ext="${filename##*.}"
    for allowed in "${ALLOWED_EXTENSIONS[@]}"; do
        if [ "$ext" == "$allowed" ]; then
            return 0
        fi
    done
    return 1
}

#############################################
# Output file contents for allowed files.
#############################################

# Process allowed files in the root folder.
for item in "$TARGET_DIR"/*; do
    [ -e "$item" ] || continue
    if [ -f "$item" ] && [[ "$(basename "$item")" != .* ]]; then
        if has_allowed_extension "$item"; then
            echo "** [$item] **"
            cat "$item"
            echo ""
        fi
    fi
done

# Process allowed files in allowed first-level subdirectories.
for item in "$TARGET_DIR"/*; do
    [ -e "$item" ] || continue
    if [ -d "$item" ] && [[ "$(basename "$item")" != .* ]]; then
        base=$(basename "$item")
        if [[ " ${ALLOWED_FOLDERS[@]} " =~ " $base " ]]; then
            # Use find to recursively list files, excluding hidden ones.
            while IFS= read -r -d '' file; do
                if has_allowed_extension "$file"; then
                    echo "** [$file] **"
                    cat "$file"
                    echo ""
                fi
            done < <(find "$item" -type f -not -path '*/.*' -print0)
        fi
    fi
done
