#!/bin/bash

# Function to recursively process files
process_file() {
    local input_file="$1"
    local output_file="$2"
    local processed_files="$3"
    local base_dir="$4"

    # Get the absolute path of the input file
    local full_path=$(realpath "$input_file")

    # Check if the file has already been processed
    if grep -q "$full_path" <<< "$processed_files"; then
        echo "Skipping already processed file: $full_path"
        return
    fi

    # Append the file to the list of processed files
    processed_files+="$full_path\n"

    # Write the content of the current file to the output file
    echo -e "** Content of $full_path **\n" >> "$output_file"
    cat "$input_file" >> "$output_file"
    echo -e "\n" >> "$output_file"

    # Search for included HTML files and recursively process them
    local included_files=$(grep -oP 'inc/[a-zA-Z0-9_.-]+\.html(?=})' "$input_file")

    for included_file in $included_files; do
        # Resolve the relative path to an absolute path
        local included_file_path="$base_dir/$included_file"

        # Check if the file exists
        if [[ -f "$included_file_path" ]]; then
            process_file "$included_file_path" "$output_file" "$processed_files" "$base_dir"
        else
            echo "Warning: Included file not found: $included_file_path"
        fi
    done
}

# Main script
if [[ $# -ne 2 ]]; then
    echo "Usage: $0 <input_file> <output_file>"
    exit 1
fi

input_file="$1"
output_file="$2"

# Ensure the input file exists
if [[ ! -f "$input_file" ]]; then
    echo "Error: Input file not found: $input_file"
    exit 1
fi

# Get the base directory of the input file
base_dir=$(dirname "$input_file")

# Clear the output file if it exists
> "$output_file"

# Start processing
process_file "$input_file" "$output_file" "" "$base_dir"

