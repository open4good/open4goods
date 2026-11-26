import type {
  OpenDataColumnDto,
  OpenDataDatasetDto,
} from '~~/shared/api-client'

/** A file older than this is shown with a stale banner (the weekly pass runs on Sundays). */
export const OPENDATA_STALE_AFTER_DAYS = 8

export const OPENDATA_LICENSE_URL =
  'https://opendatacommons.org/licenses/odbl/1-0/'
export const OPENDATA_DOI = '10.5281/zenodo.14035436'

/** Public mirrors of the datasets, used as `sameAs` in the JSON-LD (Hugging Face: added with the lot 4 mirrors). */
export const OPENDATA_SAME_AS = [
  'https://www.data.gouv.fr/datasets/base-de-codes-barres-noms-et-categories-produits',
  `https://doi.org/${OPENDATA_DOI}`,
  'https://www.kaggle.com/datasets/goulvenfuret/gtin-ean-products-database',
  'https://figshare.com/articles/dataset/Datasets_of_products_and_books_identified_by_ISBN_GTIN/27605688',
] as const

const DAY_MS = 24 * 60 * 60 * 1000

/**
 * Age in whole days of a file generated at `generatedAt`.
 *
 * @returns null when the date is missing or not an ISO 8601 instant
 */
export const datasetAgeInDays = (
  generatedAt: string | null | undefined,
  now: Date
): number | null => {
  if (!generatedAt) {
    return null
  }
  const time = Date.parse(generatedAt)
  if (Number.isNaN(time)) {
    return null
  }
  return Math.max(0, Math.floor((now.getTime() - time) / DAY_MS))
}

/** True when the file is older than {@link OPENDATA_STALE_AFTER_DAYS}; an unknown date is not "stale". */
export const isDatasetStale = (
  generatedAt: string | null | undefined,
  now: Date
): boolean => {
  const age = datasetAgeInDays(generatedAt, now)
  return age !== null && age > OPENDATA_STALE_AFTER_DAYS
}

/** Fill rate (0 to 1) as a percentage with one decimal at most, without trailing zero ("42", "99.9"). */
export const formatFillRate = (fillRate: number | undefined): string | null => {
  if (fillRate === undefined || Number.isNaN(fillRate)) {
    return null
  }
  const percent = Math.round(Math.min(1, Math.max(0, fillRate)) * 1000) / 10
  return String(percent)
}

export interface ReuseSnippets {
  curl: string
  wget: string
  sha256: string
  pandas: string
  duckdb: string
  r: string
}

/** File name of a download URL ({@code https://data.nudger.fr/gtin-open-data.zip} gives {@code gtin-open-data.zip}). */
export const fileNameOf = (url: string): string =>
  url.split('?')[0]?.split('/').pop() || 'dataset.zip'

/**
 * Shell and code snippets to download with resume, verify and read a zipped CSV.
 *
 * @param url    public URL of the zip
 * @param sha256 manifest sha256 of the zip, null when unknown
 */
export const buildReuseSnippets = (
  url: string,
  sha256: string | null | undefined
): ReuseSnippets => {
  const zip = fileNameOf(url)
  const csv = zip.replace(/\.zip$/, '.csv')
  return {
    curl: `curl -C - -L -O ${url}`,
    wget: `wget -c ${url}`,
    sha256: sha256
      ? `echo "${sha256}  ${zip}" | sha256sum -c -`
      : `sha256sum ${zip}`,
    pandas: `import pandas as pd\n\ndf = pd.read_csv("${zip}", compression="zip", low_memory=False)\nprint(df.head())`,
    duckdb: `SELECT * FROM read_csv_auto('zip://${zip}/${csv}') LIMIT 20;`,
    r: `library(readr)\n\ndf <- read_csv("${zip}")\nhead(df)`,
  }
}

interface JsonLdOptions {
  name: string
  description: string
  pageUrl: string
  inLanguage: string
  organizationName: string
  organizationUrl: string
  /** Datasets (zips) described by the page: one `distribution` per file. */
  datasets: OpenDataDatasetDto[]
  /** Columns published as `variableMeasured` (the union over the datasets when several). */
  columns?: OpenDataColumnDto[]
  /** DOI to publish as `identifier`, when it exists. */
  doi?: string
  spatialCoverage?: string
  /** Fixed list overriding the default mirrors. */
  sameAs?: readonly string[]
}

/** schema.org `contentSize` text with SI units and a dot decimal separator ("6.4 GB"), whatever the page language. */
export const formatContentSize = (bytes: number): string => {
  const units = ['B', 'kB', 'MB', 'GB', 'TB']
  let value = bytes
  let unit = 0
  while (value >= 1000 && unit < units.length - 1) {
    value /= 1000
    unit++
  }
  const digits = unit === 0 || value >= 10 ? 0 : 1
  return `${value.toFixed(digits)} ${units[unit]}`
}

/**
 * Schema.org `Dataset` of an open data page. Every value comes from the manifest-backed DTOs: a missing value is left
 * out of the markup, never replaced with a default.
 */
export const buildDatasetJsonLd = (options: JsonLdOptions) => {
  const dates = options.datasets
    .map(dataset => dataset.generatedAt)
    .filter((value): value is string => Boolean(value))
    .sort()
  const dateModified = dates.length ? dates[dates.length - 1] : undefined
  const totalBytes = options.datasets.reduce(
    (sum, dataset) => sum + (dataset.sizeBytes ?? 0),
    0
  )
  const organization = {
    '@type': 'Organization',
    name: options.organizationName,
    url: options.organizationUrl,
  }
  const distribution = options.datasets
    .filter(dataset => Boolean(dataset.downloadUrl))
    .map(dataset => ({
      '@type': 'DataDownload',
      encodingFormat: 'application/zip',
      contentUrl: dataset.downloadUrl,
      ...(dataset.sizeBytes
        ? { contentSize: formatContentSize(dataset.sizeBytes) }
        : {}),
      ...(dataset.generatedAt ? { dateModified: dataset.generatedAt } : {}),
      ...(dataset.sha256 ? { sha256: dataset.sha256 } : {}),
    }))
  const columns = options.columns ?? []

  return {
    '@context': 'https://schema.org',
    '@type': 'Dataset',
    name: options.name,
    description: options.description,
    url: options.pageUrl,
    inLanguage: options.inLanguage,
    isAccessibleForFree: true,
    license: OPENDATA_LICENSE_URL,
    creator: organization,
    publisher: organization,
    encodingFormat: ['text/csv', 'application/zip'],
    ...(dateModified ? { dateModified } : {}),
    ...(totalBytes > 0 ? { contentSize: formatContentSize(totalBytes) } : {}),
    ...(options.doi ? { identifier: `https://doi.org/${options.doi}` } : {}),
    sameAs: [...(options.sameAs ?? OPENDATA_SAME_AS)],
    ...(options.spatialCoverage
      ? { spatialCoverage: options.spatialCoverage }
      : {}),
    ...(columns.length
      ? {
          variableMeasured: columns.map(column => ({
            '@type': 'PropertyValue',
            name: column.name,
            ...(column.description ? { description: column.description } : {}),
            ...(column.unit ? { unitText: column.unit } : {}),
          })),
        }
      : {}),
    distribution,
  }
}

/** Identifier of the vertical of an impact dataset id ({@code impact-dishwasher} gives {@code dishwasher}). */
export const verticalOfDatasetId = (id: string | null | undefined): string =>
  (id ?? '').replace(/^impact-/, '')
