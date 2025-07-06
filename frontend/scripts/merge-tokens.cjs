const fs = require('fs');
const path = require('path');

const tokensPath = path.join(__dirname, '..', 'design-tokens', 'tokens.json');
const tokens = JSON.parse(fs.readFileSync(tokensPath, 'utf8'));

// merge all sets except special keys starting with '$'
const merged = {};
for (const [setName, setTokens] of Object.entries(tokens)) {
  if (!setName.startsWith('$') && typeof setTokens === 'object') {
    Object.assign(merged, setTokens);
  }
}

// group tokens by type for Style Dictionary
const grouped = {};
for (const [name, token] of Object.entries(merged)) {
  const type = token.type || 'other';
  if (!grouped[type]) grouped[type] = {};
  grouped[type][name] = { $value: token.value, $type: type };
  if (token.description) grouped[type][name].description = token.description;
}

const outPath = path.join(__dirname, '..', 'design-tokens', 'merged-tokens.json');
fs.writeFileSync(outPath, JSON.stringify(grouped, null, 2));
console.log(`Merged tokens written to ${outPath}`);

