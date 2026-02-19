import json
import sys

def validate_ndjson(filepath):
    print(f"Validating {filepath}...")
    try:
        with open(filepath, 'r') as f:
            for i, line in enumerate(f):
                line = line.strip()
                if not line:
                    continue
                try:
                    json.loads(line)
                except json.JSONDecodeError as e:
                    print(f"Error on line {i+1}: {e}")
                    print(f"Content: {line[:100]}...")
                    return False
        print("Validation successful.")
        return True
    except Exception as e:
        print(f"Failed to open/read file: {e}")
        return False

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python3 validate_ndjson.py <filepath>")
        sys.exit(1)
    validate_ndjson(sys.argv[1])
