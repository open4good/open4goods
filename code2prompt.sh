#!/bin/bash
# Usage: script.sh <directory> [output_file]
# This script lists files and directories in <directory> (ignoring hidden files).
# It includes files in the root folder and recursively processes only the first-level subdirectories
# that match allowed names (by default: src and test). It then prints a directory tree followed by
# file contents (for files with allowed extensions: .md, .java, .yml, .xml, .json).
#
# Updates in this version:
# - File contents are wrapped in Markdown code fences using tilde characters (~~~).
# - Any file named "pom.xml" (or "pom.xml (pom parent)") is printed after all other files.
# - The pom.xml from the current working directory is labeled as "(pom parent)".
# - Files with the basename "pom.xml" are skipped during the normal loops and printed at last.

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
ALLOWED_EXTENSIONS=("md" "java" "yml" "xml" "json")

# Configurable allowed first-level directories (only these folders will be recursed)
ALLOWED_FOLDERS=("src" "test")

# Array to hold pom.xml files (both pom and pom parent)
POM_FILES=()

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
echo "# Directory structure "

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
echo "# Files content"
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
# Helper function to add a pom.xml file to POM_FILES array if not already added.
#############################################
add_pom_file() {
    local file="$1"
    # Check for duplicates by comparing real paths.
    local real_file
    real_file=$(realpath "$file")
    for existing in "${POM_FILES[@]}"; do
        if [ "$(realpath "$existing")" = "$real_file" ]; then
            return
        fi
    done
    POM_FILES+=("$file")
}

#############################################
# Process allowed files in the root folder (except pom.xml).
#############################################
for item in "$TARGET_DIR"/*; do
    [ -e "$item" ] || continue
    if [ -f "$item" ] && [[ "$(basename "$item")" != .* ]]; then
        if has_allowed_extension "$item"; then
            if [ "$(basename "$item")" = "pom.xml" ]; then
                add_pom_file "$item"
                continue
            fi
            echo "## [$(basename "$item")]"
            echo '~~~'
            cat "$item"
            echo '~~~'
            echo ""
        fi
    fi
done

#############################################
# Process allowed files in allowed first-level subdirectories (except pom.xml).
#############################################
for item in "$TARGET_DIR"/*; do
    [ -e "$item" ] || continue
    if [ -d "$item" ] && [[ "$(basename "$item")" != .* ]]; then
        base=$(basename "$item")
        if [[ " ${ALLOWED_FOLDERS[@]} " =~ " $base " ]]; then
            # Use find to recursively list files, excluding hidden ones.
            while IFS= read -r -d '' file; do
                if has_allowed_extension "$file"; then
                    if [ "$(basename "$file")" = "pom.xml" ]; then
                        add_pom_file "$file"
                        continue
                    fi
                    echo "## [$(basename "$file")]"
                    echo '~~~'
                    cat "$file"
                    echo '~~~'
                    echo ""
                fi
            done < <(find "$item" -type f -not -path '*/.*' -print0)
        fi
    fi
done

#############################################
# If TARGET_DIR is not the current directory, include the current directory pom.xml (as pom parent)
#############################################
if [ "$(realpath "$TARGET_DIR")" != "$(pwd)" ] && [ -f "./pom.xml" ]; then
    add_pom_file "./pom.xml"
fi

#############################################
# Print all pom.xml files at the end.
#############################################
for pom in "${POM_FILES[@]}"; do
    # Check if this pom.xml is the one in the current directory.
    if [ "$(realpath "$pom")" = "$(realpath ./pom.xml)" ]; then
        echo "## [$(basename "$pom") (pom parent)]"
    else
        echo "## [$(basename "$pom")]"
    fi
    echo '~~~'
    cat "$pom"
    echo '~~~'
    echo ""
done
