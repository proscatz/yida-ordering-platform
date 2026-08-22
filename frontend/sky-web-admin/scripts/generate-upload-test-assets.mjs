import { mkdir, writeFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

const scriptDir = path.dirname(fileURLToPath(import.meta.url))
const outputDir = path.resolve(scriptDir, '../test-artifacts/upload')
const basePng = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=',
  'base64',
)

const assets = [
  ['small.png', 64 * 1024],
  ['over-1mb.png', 1536 * 1024],
  ['near-5mb.png', Math.floor(4.9 * 1024 * 1024)],
  ['over-5mb.png', 5 * 1024 * 1024 + 1],
]

await mkdir(outputDir, { recursive: true })
for (const [name, size] of assets) {
  const bytes = Buffer.alloc(size)
  basePng.copy(bytes)
  await writeFile(path.join(outputDir, name), bytes)
}

console.log(`[upload-test-assets] generated ${assets.length} non-personal PNG files in ${outputDir}`)
