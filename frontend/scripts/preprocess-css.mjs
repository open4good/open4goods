import { readFile, writeFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import postcss from 'postcss'
import cssnano from 'cssnano'
import prefixer from 'postcss-prefix-selector'
import xwikiSandboxPrefixerOptions from '../config/postcss/xwiki-sandbox-prefixer-options.js'

const currentFilePath = fileURLToPath(import.meta.url)
const projectRoot = path.resolve(path.dirname(currentFilePath), '..')
const assetsDir = path.join(projectRoot, 'app', 'assets', 'css')

const bootstrapCssPath = path.join(assetsDir, 'bootstrap.css')
const outputPath = path.join(assetsDir, 'text-content.css')

const [bootstrapCss] = await Promise.all([readFile(bootstrapCssPath, 'utf8')])

const combinedCss = `${bootstrapCss}`

const prefixedCss = await postcss([
  prefixer(xwikiSandboxPrefixerOptions),
  cssnano({
    preset: [
      'default',
      {
        discardComments: {
          preserve: comment => /@preserve|@license|^!/i.test(comment),
        },
      },
    ],
  }),
]).process(combinedCss, {
  from: undefined,
  to: outputPath,
})

await writeFile(outputPath, prefixedCss.css, 'utf8')
