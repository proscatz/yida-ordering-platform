import { readdir, readFile, stat } from 'node:fs/promises'
import path from 'node:path'
import process from 'node:process'

const artifactRoot = path.resolve(process.argv[2] || 'dist')
const violations = []

async function collectFiles(directory) {
  const entries = await readdir(directory, { withFileTypes: true })
  const nestedFiles = await Promise.all(entries.map(async (entry) => {
    const fullPath = path.join(directory, entry.name)
    return entry.isDirectory() ? collectFiles(fullPath) : [fullPath]
  }))
  return nestedFiles.flat()
}

function relative(file) {
  return path.relative(artifactRoot, file).replaceAll(path.sep, '/')
}

try {
  const rootStat = await stat(artifactRoot)
  if (!rootStat.isDirectory()) throw new Error('artifact path is not a directory')

  const files = await collectFiles(artifactRoot)
  for (const file of files) {
    const artifactPath = relative(file)
    if (file.toLowerCase().endsWith('.map')) {
      violations.push(`${artifactPath}: Source Map file`)
      continue
    }

    if (!/\.(?:js|css)$/i.test(file)) continue
    const content = await readFile(file, 'utf8')
    if (/sourceMappingURL/i.test(content)) {
      violations.push(`${artifactPath}: sourceMappingURL reference`)
    }
    if (/sourcesContent/i.test(content)) {
      violations.push(`${artifactPath}: embedded sourcesContent`)
    }
  }

  if (violations.length) {
    console.error('[source-map-check] FAILED')
    for (const violation of violations) console.error(`- ${violation}`)
    process.exitCode = 1
  } else {
    const totalBytes = (await Promise.all(files.map(async (file) => (await stat(file)).size)))
      .reduce((sum, size) => sum + size, 0)
    console.log(`[source-map-check] PASS: ${files.length} files, ${totalBytes} bytes`)
  }
} catch (error) {
  console.error(`[source-map-check] FAILED: ${error instanceof Error ? error.message : String(error)}`)
  process.exitCode = 1
}
