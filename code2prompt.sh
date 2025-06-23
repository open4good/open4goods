#!/bin/bash
# Usage: script.sh <directory> [output_file]
# This script recursively lists files and directories in <directory> (ignoring hidden files).
# It outputs a directory tree followed by file contents (for allowed extensions: .md, .java, .yml, .xml, .json)
# in Markdown format, using tilde fences (~~~) to wrap the content.
#
# Special ordering:
# - Files with "service" in their name are printed first.
# - Files named "pom.xml" are omitted during the main file output and printed at the end.
# - If a pom.xml exists in the current working directory (and TARGET_DIR is not the current directory),
#   it is labeled as "pom.xml (pom parent)" and printed at the end.
#
# Launch the script with no arguments or with -? to display extensive documentation.

# Extensive documentation (if no arguments or -? is provided)
if [ $# -lt 1 ] || [ "$1" = "-?" ]; then
    cat <<'EOF'
Usage: script.sh <directory> [output_file]

Description:
  This script recursively lists files and directories in the given <directory> (ignoring hidden files).
  It outputs the directory structure and then prints the contents of files with the allowed extensions:
    md, java, yml, xml, json.

  The script processes files in the root folder and in the allowed first-level subdirectories
  (by default: src and test). The output is formatted as Markdown:
    - File contents are wrapped in code fences using tilde characters (~~~).
    - Files whose names contain "service" (case-insensitive) are printed first.
    - Files named "pom.xml" are omitted during the main output and printed at the end.
    - If a pom.xml exists in the current working directory (and it’s not part of the target directory),
      it is labeled as "pom.xml (pom parent)" and printed at the end.
    - Each file title shows the filename, followed by a hyphen and the file's full path.

Examples:
  script.sh /path/to/target
  script.sh /path/to/target output.md

EOF
    exit 0
fi

TARGET_DIR="$1"
OUTPUT_FILE="$2"

# Verify TARGET_DIR exists and is a directory.
if [ ! -d "$TARGET_DIR" ]; then
    echo "Error: '$TARGET_DIR' is not a valid directory."
    exit 1
fi

# Configurable allowed file extensions (without the dot)
ALLOWED_EXTENSIONS=("md" "java" "yml" "xml" "json" "html")

# Configurable allowed first-level directories (only these folders will be recursed)
ALLOWED_FOLDERS=("src" "test")

# Arrays to hold files
SERVICE_FILES=()   # non-pom files containing "service"
OTHER_FILES=()     # other non-pom files
POM_FILES=()       # all pom.xml files

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
# Helper function to add a non-pom file to the appropriate array.
# Files whose basename contains "service" (case-insensitive) are added to SERVICE_FILES.
#############################################
add_non_pom_file() {
    local file="$1"
    local base
    base=$(basename "$file")
    if [[ "${base,,}" == *"service"* ]]; then
         SERVICE_FILES+=("$file")
    else
         OTHER_FILES+=("$file")
    fi
}

#############################################
# Process allowed files in the root folder (excluding pom.xml).
#############################################
for item in "$TARGET_DIR"/*; do
    [ -e "$item" ] || continue
    if [ -f "$item" ] && [[ "$(basename "$item")" != .* ]]; then
        if has_allowed_extension "$item"; then
            if [ "$(basename "$item")" = "pom.xml" ]; then
                add_pom_file "$item"
                continue
            fi
            add_non_pom_file "$item"
        fi
    fi
done

#############################################
# Process allowed files in allowed first-level subdirectories (excluding pom.xml).
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
                    add_non_pom_file "$file"
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
# Print non-pom files: "service" files first, then others.
#############################################
for file in "${SERVICE_FILES[@]}"; do
    echo "## [$(basename "$file") - $(realpath "$file")]"
    echo '~~~'
    cat "$file"
    echo '~~~'
    echo ""
done

for file in "${OTHER_FILES[@]}"; do
    echo "## [$(basename "$file") - $(realpath "$file")]"
    echo '~~~'
    cat "$file"
    echo '~~~'
    echo ""
done

#############################################
# Finally, print all pom.xml files at the end.
#############################################
for pom in "${POM_FILES[@]}"; do
    if [ "$(realpath "$pom")" = "$(realpath ./pom.xml)" ]; then
        echo "## [$(basename "$pom") (pom parent) - $(realpath "$pom")]"
    else
        echo "## [$(basename "$pom") - $(realpath "$pom")]"
    fi
    echo '~~~'
    cat "$pom"
    echo '~~~'
    echo ""
done
