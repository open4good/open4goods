# Image Processing Service

This service is part of the [open4goods](https://github.com/open4good/open4goods) project. It provides simple image generation through OpenAI and utilities around ImageMagick.

## Overview

Main responsibilities:

- Convert images to PNG format and create thumbnails.
- Analyse basic image metadata such as size or type.
- Optionally generate images via OpenAI.

## Configuration

The service does not expose configuration properties. All parameters are supplied directly to the service methods.

## Usage Example

```java
ImageMagickService service = new ImageMagickService();
service.convertToPng(new File("src.jpg"), new File("target.png"));
service.generateThumbnail(new File("target.png"), new File("thumb.png"), 120);
```

## Build & Test

Build from this directory:

```bash
mvn clean install
```

Run tests only:

```bash
mvn test
```

You can also build it from the repository root:

```bash
mvn -pl services/image-processing -am clean install
```

For project-wide information see the [main README](../../README.md).
