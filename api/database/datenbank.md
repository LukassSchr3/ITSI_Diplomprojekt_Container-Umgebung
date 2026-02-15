# PostgreSQL Datenbank

## Starten der Datenbank

```bash
docker-compose up -d
```

## Stoppen der Datenbank

```bash
docker-compose down
```

## Datenbank-Zugangsdaten

- **Host:** localhost
- **Port:** 5432
- **Datenbank:** itsi_db
- **User:** itsi_user
- **Passwort:** itsi_password

## Verbindungsstring

```
postgresql://itsi_user:itsi_password@localhost:5432/itsi_db
```

## Logs anzeigen

```bash
docker-compose logs -f postgres
```

## Datenbank zurücksetzen

```bash
docker-compose down -v
docker-compose up -d
```
