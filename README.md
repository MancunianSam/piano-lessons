# Piano lessons site

## Run the app

Set the following environment variables and run with sbt
```bash
export AUTH_URL={URL_OF_OIDC_PROVIDER};
export AUTH_CLIENT_ID={OIDC_CLIENT_ID};
export SENDGRID_API_KEY={SENDGRID_API_KEY};
export AUTH_SECRET={OIDC_CLIENT_SECRET};
export BASE_URL=http://localhost:9000;
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account-credentials

sbt run
```

## Testing
```bash
sbt test
```

## Production build
```bash
sbt dist
docker build -t your-tag-name .
docker push
```
