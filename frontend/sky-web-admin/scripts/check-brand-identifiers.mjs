import { readdir, readFile, stat } from 'node:fs/promises'
import path from 'node:path'
import process from 'node:process'
import { fileURLToPath } from 'node:url'

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url))
const projectRoot = path.resolve(scriptDirectory, '..')
const workspaceRoot = path.resolve(projectRoot, '..', '..')
const backendRoot = path.join(workspaceRoot, 'backend')

const separator = '[\\s_\\-‐‑‒–—]*'
const rules = [
  { id: 'legacy-cn-brand-a', expression: new RegExp(`\u82cd${separator}\u7a79${separator}\u5916${separator}\u5356`, 'giu') },
  { id: 'legacy-cn-brand-b', expression: new RegExp(`\u745e${separator}\u5409${separator}\u5916${separator}\u5356`, 'giu') },
  { id: 'legacy-tutorial-a', expression: new RegExp(`it${separator}cast`, 'giu') },
  { id: 'legacy-tutorial-b', expression: new RegExp(`it${separator}heima`, 'giu') },
  {
    id: 'legacy-admin-template',
    expression: new RegExp(`vue${separator}typescript${separator}admin(?:${separator}template)?`, 'giu'),
  },
]

const excludedDirectoryNames = new Set([
  '.git', '.idea', '.vscode', 'node_modules', 'target', 'coverage',
])
const archiveDirectoryPattern = /(?:^|[-_.])(?:backup|archive|previous|deploy|failed)(?:$|[-_.])/i
const ignoredFilePattern = /(?:~|\.tmp|\.temp|\.swp|\.swo|\.bak)$/i
const textExtensions = new Set([
  '.css', '.html', '.java', '.js', '.json', '.jsx', '.md', '.mjs', '.properties',
  '.scss', '.sql', '.svg', '.ts', '.tsx', '.txt', '.vue', '.xml', '.yaml', '.yml',
])

function isExcludedDirectory(name) {
  return excludedDirectoryNames.has(name.toLowerCase()) || archiveDirectoryPattern.test(name)
}

function isTextFile(file) {
  return textExtensions.has(path.extname(file).toLowerCase()) || path.basename(file).startsWith('.env')
}

async function collectFiles(entryPath, shallow = false) {
  const entryStat = await stat(entryPath)
  if (entryStat.isFile()) return isTextFile(entryPath) && !ignoredFilePattern.test(entryPath) ? [entryPath] : []
  if (!entryStat.isDirectory()) return []

  const entries = await readdir(entryPath, { withFileTypes: true })
  const nested = await Promise.all(entries.map(async (entry) => {
    if (entry.isDirectory()) {
      if (shallow || isExcludedDirectory(entry.name)) return []
      return collectFiles(path.join(entryPath, entry.name))
    }
    const file = path.join(entryPath, entry.name)
    return isTextFile(file) && !ignoredFilePattern.test(entry.name) ? [file] : []
  }))
  return nested.flat()
}

function lineNumberAt(content, index) {
  let line = 1
  for (let cursor = 0; cursor < index; cursor += 1) {
    if (content.charCodeAt(cursor) === 10) line += 1
  }
  return line
}

function configurationKey(file, content, index) {
  const extension = path.extname(file).toLowerCase()
  if (!['.properties', '.yaml', '.yml'].includes(extension) && !path.basename(file).startsWith('.env')) return null
  const lineStart = content.lastIndexOf('\n', index - 1) + 1
  const lineEndCandidate = content.indexOf('\n', index)
  const lineEnd = lineEndCandidate === -1 ? content.length : lineEndCandidate
  const line = content.slice(lineStart, lineEnd)
  const match = extension === '.properties' || path.basename(file).startsWith('.env')
    ? line.match(/^\s*([A-Za-z_][A-Za-z0-9_.-]*)\s*=/)
    : line.match(/^\s*([A-Za-z_][A-Za-z0-9_.-]*)\s*:/)
  return match?.[1] ?? '[unresolved-key]'
}

async function scanRange(range) {
  const uniqueFiles = new Set()
  for (const entry of range.entries) {
    for (const file of await collectFiles(entry.path, entry.shallow)) uniqueFiles.add(path.resolve(file))
  }

  const matches = []
  for (const file of [...uniqueFiles].sort()) {
    const content = await readFile(file, 'utf8')
    for (const rule of rules) {
      rule.expression.lastIndex = 0
      for (const match of content.matchAll(rule.expression)) {
        matches.push({
          file,
          line: lineNumberAt(content, match.index ?? 0),
          rule: rule.id,
          configurationKey: configurationKey(file, content, match.index ?? 0),
        })
      }
    }
  }

  return { name: range.name, files: uniqueFiles.size, matches }
}

const rootConfigNames = new Set([
  'index.html', 'package.json', 'package-lock.json', 'tsconfig.json', 'tsconfig.app.json',
  'tsconfig.node.json', 'vite.config.ts', 'vitest.config.ts',
])
const projectConfigEntries = (await readdir(projectRoot, { withFileTypes: true }))
  .filter((entry) => entry.isFile() && (rootConfigNames.has(entry.name) || entry.name.startsWith('.env')))
  .map((entry) => ({ path: path.join(projectRoot, entry.name), shallow: true }))

const ranges = {
  predeploy: [
    { name: 'admin-src', entries: [{ path: path.join(projectRoot, 'src') }] },
    { name: 'admin-public', entries: [{ path: path.join(projectRoot, 'public') }] },
    { name: 'admin-entry-config', entries: projectConfigEntries },
    { name: 'admin-scripts', entries: [{ path: path.join(projectRoot, 'scripts') }] },
    { name: 'admin-dist', entries: [{ path: path.join(projectRoot, 'dist') }] },
  ],
  backend: [
    { name: 'backend-active', entries: [{ path: backendRoot }] },
  ],
}

const requestedScope = process.argv[process.argv.indexOf('--scope') + 1] || 'predeploy'
const selectedRanges = requestedScope === 'all'
  ? [...ranges.predeploy, ...ranges.backend]
  : ranges[requestedScope]

if (!selectedRanges) {
  console.error(`[brand-check] FAILED: unsupported scope ${requestedScope}`)
  process.exitCode = 2
} else {
  console.log(`[brand-check] scope=${requestedScope}`)
  console.log('[brand-check] exclusions=node_modules,.git,target,.idea,.vscode,coverage,IDE temp files,archived backup/previous/deploy/failed directories')
  let totalFiles = 0
  let totalMatches = 0
  try {
    for (const range of selectedRanges) {
      const result = await scanRange(range)
      totalFiles += result.files
      totalMatches += result.matches.length
      console.log(`[brand-check] ${result.name}: files=${result.files}, matches=${result.matches.length}`)
      for (const match of result.matches) {
        const relativePath = path.relative(workspaceRoot, match.file).replaceAll(path.sep, '/')
        const key = match.configurationKey ? `, key=${match.configurationKey}` : ''
        console.error(`[brand-check] MATCH ${relativePath}:${match.line}, rule=${match.rule}${key}`)
      }
    }
    console.log(`[brand-check] total: files=${totalFiles}, matches=${totalMatches}`)
    if (totalMatches > 0) {
      console.error('[brand-check] FAILED')
      process.exitCode = 1
    } else {
      console.log('[brand-check] PASS')
    }
  } catch (error) {
    console.error(`[brand-check] FAILED: ${error instanceof Error ? error.message : String(error)}`)
    process.exitCode = 2
  }
}
