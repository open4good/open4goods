import sys, json, pandas as pd
from jinja2 import Template

json_input, template_file, markdown_output, csv_file = sys.argv[1:]

with open(json_input) as f:
    kpi_data = json.load(f)

with open(template_file) as f:
    template = Template(f.read())

markdown_content = template.render(**kpi_data)
with open(markdown_output, 'w') as f:
    f.write(markdown_content)

csv_row = {
    "date": kpi_data['end_date'],
    **{k: v for api in kpi_data['data'].values() for k, v in api.items()}
}

try:
    df = pd.read_csv(csv_file)
except FileNotFoundError:
    df = pd.DataFrame()

df = pd.concat([df, pd.DataFrame([csv_row])])
df.to_csv(csv_file, index=False)
