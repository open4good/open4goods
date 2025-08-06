import sys, json
from datetime import datetime

start_date, end_date, output = sys.argv[1], sys.argv[2], sys.argv[3]

data = {
    "plausible": {"visits": 12345},
    "xwiki": {"new_content": 25},
    "github": {
        "issues_closed": 10,
        "issues_opened": 5,
        "pr_merged": 8,
        "loc_added": 500,
    },
    "custom_api": {"conversion_rate": 3.2}
}

with open(output, 'w') as f:
    json.dump({"start_date": start_date, "end_date": end_date, "data": data}, f, indent=2)
