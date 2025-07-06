const fs = require('fs');
const path = require('path');

const tokensPath = path.join(__dirname, '..', 'design-tokens', 'tokens.json');
const tokens = JSON.parse(fs.readFileSync(tokensPath, 'utf8'));

const mergedSets = Object.assign({}, tokens.global, tokens.scss, tokens.css);

const grouped = {};
for (const [name, token] of Object.entries(mergedSets)) {
  const type = token.type || 'other';
  if (!grouped[type]) grouped[type] = {};
  grouped[type][name] = { "$value": token.value, "$type": type };
  if (token.description) grouped[type][name].description = token.description;
}

const outputPath = path.join(__dirname, '..', 'design-tokens', 'merged-tokens.json');
fs.writeFileSync(outputPath, JSON.stringify(grouped, null, 2));
console.log(`Merged tokens written to ${outputPath}`);

