# Remote File Caching Service

Utility for downloading remote resources with local caching and optional
archive extraction.

## Features

- Download files with configurable timeouts.
- Refresh files based on a configurable period.
- Supports GZIP decompression when retrieving archives.
- Names cached files after the SHA-256 of their URL (see below).
- Logs only the scheme, host and port of a URL, followed by the cache file
  name; `getResource(url, ttl, logLabel)` logs a caller-supplied label instead.
  Download failures are rethrown without the JDK message, which quotes the URL;
  only the HTTP status code is kept.

## Cache file names

Remote URLs can carry credentials: Effinity and Awin take the API key as a path
segment, MaxMind as a query value. A cached file is therefore named
`RemoteFileCachingService.cacheFileName(url)`, the lowercase hexadecimal
SHA-256 of the full URL, with no extension and no fragment of the URL. The
temporary download file is `tmp-<sha256>`. Callers that build a cache path
themselves (`IcecatFileDownloadService`) use
`RemoteFileCachingService.resolveCacheFile(folder, url)`.

Files used to be named after the URL with its punctuation removed
(`IdHelper.getHashedName`), which showed the keys in a directory listing.
Migration is automatic and per resource:

- on access, a copy left under the former name is moved to the hashed name and
  keeps its modification date, so it is not downloaded again before its refresh
  period ends;
- after a download, the copy under the former name and a former `tmp-` file are
  deleted.

Former files of resources that are no longer requested stay behind. Once every
application sharing a cache folder runs this version, Ops can purge them. The
former names start with `http` (the scheme without `://`), files at the root of
a cache folder have no other reason to, and product resources live in
subfolders. Count them without printing their names, which contain the keys,
then delete them:

```bash
# Cache folders: /opt/open4goods/.cached and the geocode cache path
find /opt/open4goods/.cached -maxdepth 1 -type f \( -name 'http*' -o -name 'tmp-http*' \) | wc -l
find /opt/open4goods/.cached -maxdepth 1 -type f \( -name 'http*' -o -name 'tmp-http*' \) -delete
```

Purged resources are downloaded again on their next request.

## Configuration

```yaml
remote-file-caching:
  connectionTimeout: 30000
  readTimeout: 30000
```

## Build & Test

```bash
mvn clean install
mvn test
```

## Project Links

See the [main open4goods project](../../README.md) for details.
This module is released under the [AGPL v3 license](../../LICENSE).
