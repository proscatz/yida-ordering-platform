import { mkdir, readFile, writeFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'
import { useCascaderAreaData } from '@vant/area-data'

const scriptDirectory = dirname(fileURLToPath(import.meta.url))
const packageMetadata = JSON.parse(await readFile(
  fileURLToPath(import.meta.resolve('@vant/area-data/package.json')),
  'utf8',
))
const output = resolve(
  scriptDirectory,
  '../../../SkySpring/sky-take-out/sky-server/src/main/resources/administrative-divisions.json',
)

const document = {
  source: '@vant/area-data',
  version: packageMetadata.version,
  levels: useCascaderAreaData(),
}

await mkdir(dirname(output), { recursive: true })
await writeFile(output, `${JSON.stringify(document)}\n`, 'utf8')
console.log(`Administrative division data synchronized: ${output}`)
