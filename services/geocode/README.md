# Geocode Service

Offline geocoding microservice backed by the GeoNames `cities5000.txt` dataset
and the MaxMind GeoLite2 City database. The service downloads the datasets at
runtime, caches them using `RemoteFileCachingService`, extracts the archives,
and keeps them ready for fast lookups.

## Dataset download & caching

The GeoNames dataset is fetched from:

```
https://download.geonames.org/export/dump/cities5000.zip
```

The download is cached on disk using `RemoteFileCachingService`. The extracted
`cities5000.txt` file is stored next to the cached zip file and only re-extracted
when the zip is newer.

The MaxMind dataset is fetched from:

```
https://download.maxmind.com/app/geoip_download?edition_id=GeoLite2-City&license_key=${MAXMIND_LICENSE_KEY}&suffix=tar.gz
```

The MaxMind archive is cached on disk using `RemoteFileCachingService`. The
extracted `GeoLite2-City.mmdb` file is stored next to the cached archive and
only re-extracted when the archive is newer.

Configure caching with:

```yaml
geocode:
  cache:
    path: target/geocode-cache
  geonames:
    refresh-in-days: 7
  maxmind:
    refresh-in-days: 7
    database-file-name: GeoLite2-City.mmdb
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

### IP geolocation

`GET /v1/geoloc?ip=81.2.69.142`

```json
{
  "ip": "81.2.69.142",
  "countryName": "United Kingdom",
  "countryIsoCode": "GB",
  "cityName": "London",
  "latitude": 51.5142,
  "longitude": -0.0931,
  "timeZone": "Europe/London"
}
```

## Testing

Tests use local fixture data and never download the GeoNames or MaxMind datasets:

```bash
mvn --offline -pl services/geocode test
```
