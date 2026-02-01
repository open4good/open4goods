import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const ROOT_DIR = path.resolve(__dirname, '..')
const CONFIG_PATH = path.resolve(ROOT_DIR, 'config/lint-forbidden.json')

// Read Config
let config = { ignore: [], rules: [] }
try {
  const raw = fs.readFileSync(CONFIG_PATH, 'utf-8')
  config = JSON.parse(raw)
} catch (e) {
  console.warn(
    `[lint-forbidden] Could not read config at ${CONFIG_PATH}. Using defaults.` +
      e
  )
}

// Helper to check ignores
function isIgnored(filePath) {
  const relative = path.relative(ROOT_DIR, filePath)

  for (const pattern of config.ignore) {
    // Simple cases
    if (pattern === relative) return true
    if (relative.startsWith(pattern + path.sep)) return true
    if (relative.split(path.sep).includes(pattern)) return true // e.g. node_modules in path

    // Simple glob-like handling
    if (pattern.startsWith('**/')) {
      const suffix = pattern.slice(3) // remove **/
      if (relative.endsWith(suffix) || filePath.endsWith(suffix)) return true
    }

    // Exact directory match in various places
    if (relative === pattern || relative.startsWith(pattern + '/')) return true
  }
  return false
}

// Recursive walker
function getFiles(dir) {
  let results = []
  try {
    const list = fs.readdirSync(dir)
    list.forEach(file => {
      const fullPath = path.join(dir, file)

      // Check ignore immediately
      if (isIgnored(fullPath)) return

      const stat = fs.statSync(fullPath)
      if (stat && stat.isDirectory()) {
        results = results.concat(getFiles(fullPath))
      } else {
        results.push(fullPath)
      }
    })
  } catch (e) {
    if (e.code !== 'ENOENT') console.error(e)
  }
  return results
}

// Validation logic
let hasError = false
const allFiles = getFiles(ROOT_DIR)

console.log(`[lint-forbidden] Scanning ${allFiles.length} files...`)

for (const filePath of allFiles) {
  const ext = path.extname(filePath)

  // Find applicable rules
  const rule = config.rules.find(r => r.extensions.includes(ext))
  if (rule) {
    const content = fs.readFileSync(filePath, 'utf-8')
    for (const item of rule.forbidden) {
      const char = typeof item === 'string' ? item : item.char
      const replace = typeof item === 'string' ? null : item.replace

      if (content.includes(char)) {
        let msg = `ERROR: Found forbidden character "${char}"`
        if (replace) msg += ` (use "${replace}" instead)`
        msg += ` in ${path.relative(ROOT_DIR, filePath)}`

        console.error(msg)
        hasError = true
      }
    }
  }
}

if (hasError) {
  console.error('[lint-forbidden] Validation failed.')
  process.exit(1)
} else {
  console.log('[lint-forbidden] Validation passed.')
}
