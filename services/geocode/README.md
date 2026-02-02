# Geocode Service

Offline geocoding microservice backed by the GeoNames `cities5000.txt` dataset.
The service downloads the GeoNames archive at runtime, caches it using
`RemoteFileCachingService`, extracts the dataset, and keeps a fully in-memory
index for fast lookups.

## Dataset download & caching

The GeoNames dataset is fetched from:

```
https://download.geonames.org/export/dump/cities5000.zip
```

The download is cached on disk using `RemoteFileCachingService`. The extracted
`cities5000.txt` file is stored next to the cached zip file and only re-extracted
when the zip is newer.

Configure caching with:

```yaml
geocode:
  cache:
    path: target/geocode-cache
  geonames:
    refresh-in-days: 7
```

## Running locally

```bash
mvn --offline -pl services/geocode -am clean install
mvn --offline -pl services/geocode spring-boot:run
```

## Endpoints

### Geocode

`GET /v1/geocode?city=Paris&country=FR`

```json
{
  "city": "Paris",
  "country": "FR",
  "matchedName": "Paris",
  "geonameId": 2988507,
  "latitude": 48.8566,
  "longitude": 2.3522,
  "population": 2148327,
  "matchType": "PRIMARY"
}
```

### Distance

`GET /v1/distance?fromCity=Paris&fromCountry=FR&toCity=Berlin&toCountry=DE`

```json
{
  "from": { "...": "..." },
  "to": { "...": "..." },
  "distanceKm": 878.4,
  "distanceMeters": 878400
}
```

### Health

`GET /actuator/health`

The service reports `UP` only when the GeoNames index is loaded and non-empty.

## Testing

Tests use local fixture data and never download the GeoNames dataset:

```bash
mvn --offline -pl services/geocode test
```
