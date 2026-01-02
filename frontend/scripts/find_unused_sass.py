
import os
import re
import glob

# Configuration defines
SASS_DIR = 'app/assets/sass'
SEARCH_DIRS = ['app']
IGNORE_DIRS = ['app/assets/sass', 'node_modules', '.nuxt', '.output', 'dist']
EXTENSIONS_TO_SEARCH = ['.vue', '.ts', '.js', '.mjs']

def get_all_files(root_dir, search_dirs, ignore_subdirs, extensions):
    files = []
    for search_dir in search_dirs:
        path = os.path.join(root_dir, search_dir)
        for root, dirs, filenames in os.walk(path):
            # Filtering ignored directories
            dirs[:] = [d for d in dirs if os.path.join(root, d) not in ignore_subdirs and d not in ['.git', 'node_modules']]
            
            # Check relative path to ensure we aren't in an ignored path
            rel_root = os.path.relpath(root, root_dir)
            if any(ignored in rel_root for ignored in ignore_subdirs) and rel_root != '.':
                 continue

            for filename in filenames:
                if any(filename.endswith(ext) for ext in extensions):
                    files.append(os.path.join(root, filename))
    return files

def find_defined_classes(root_dir, sass_dir):
    defined_classes = {}
    sass_full_path = os.path.join(root_dir, sass_dir)
    
    # Python glob doesn't support ** recursively well in older versions without recursive=True, 
    # but os.walk is safer.
    sass_files = []
    for root, dirs, files in os.walk(sass_full_path):
        for file in files:
            if file.endswith(('.sass', '.scss', '.css')):
                sass_files.append(os.path.join(root, file))

    # Regex for class definition: starts with dot, followed by name, then space/newline/bracket/comma
    # Simplified regex, might need tuning for SASS nesting
    # .class-name
    class_def_pattern = re.compile(r'\.([a-zA-Z0-9_-]+)') 

    for file_path in sass_files:
        with open(file_path, 'r', encoding='utf-8') as f:
            lines = f.readlines()
            for line_no, line in enumerate(lines):
                # Simple heuristic: if line starts with . or contains .class { or nested
                # SASS is indentation based, but classes start with .
                # We want to avoid capturing property values like .5em or matching inside comments improperly
                # This is a 'loose' parser.
                
                # Strip comments
                line_content = line.split('//')[0] # Single line comment
                # (Block comments /* */ are harder but SASS usually uses //)
                
                matches = class_def_pattern.finditer(line_content)
                for match in matches:
                    class_name = match.group(1)
                    # Filter out obvious non-classes or numbers (like .5)
                    if class_name[0].isdigit():
                        continue
                    
                    # Store
                    if class_name not in defined_classes:
                        defined_classes[class_name] = []
                    defined_classes[class_name].append({'file': file_path, 'line': line_no + 1})

    return defined_classes

def find_usages(root_dir, files_to_search, defined_classes):
    used_classes = set()
    # Convert list of classes to a set for fast lookup? 
    # Or just search strings in file content?
    # Searching for every class in every file is O(N*M).
    # Better: tokenize files and check intersection.
    
    # Optimization: Read all files into a big text glob or process one by one
    # Regex to find "potential class strings"
    
    token_pattern = re.compile(r'([a-zA-Z0-9_-]+)')
    
    for file_path in files_to_search:
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
                tokens = set(token_pattern.findall(content))
                
                # Check overlapping
                for cls in defined_classes:
                    if cls in tokens:
                        used_classes.add(cls)
        except Exception as e:
            print(f"Error reading {file_path}: {e}")

    return used_classes

def main():
    root_dir = os.getcwd()
    # If running from scripts/, go up? Assuming run from project root based on file path args
    if not os.path.exists(os.path.join(root_dir, SASS_DIR)):
         # Try identifying root
         if os.path.exists(os.path.join(root_dir, 'frontend')):
             root_dir = os.path.join(root_dir, 'frontend')
    
    print(f"Analyzing in {root_dir}")
    print(f"Looking for CSS definitions in {SASS_DIR}")

    classes_def_map = find_defined_classes(root_dir, SASS_DIR)
    all_defined_classes = list(classes_def_map.keys())
    print(f"Found {len(all_defined_classes)} unique classes defined.")

    ignore_subdirs = [os.path.join(root_dir, d) for d in IGNORE_DIRS]
    files_to_search = get_all_files(root_dir, SEARCH_DIRS, ignore_subdirs, EXTENSIONS_TO_SEARCH)
    print(f"Scanning {len(files_to_search)} files for usages...")

    used_classes = find_usages(root_dir, files_to_search, all_defined_classes)
    print(f"Found {len(used_classes)} used classes.")

    unused_classes = [c for c in all_defined_classes if c not in used_classes]
    
    print("\n--- Unused Classes Report ---")
    if not unused_classes:
        print("No unused classes found!")
    else:
        for cls in sorted(unused_classes):
            locs = classes_def_map[cls]
            loc_str = ", ".join([f"{os.path.basename(l['file'])}:{l['line']}" for l in locs])
            print(f"[UNUSED] {cls} (defined in {loc_str})")

    # Generate deletion script?
    # Maybe just list them for now.

if __name__ == "__main__":
    main()
