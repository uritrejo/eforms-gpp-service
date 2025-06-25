# Configuration Guide

This application uses Spring Boot's configuration system to manage settings. Configuration values are read from `application.properties` and can be overridden using environment-specific files or environment variables.

## Configuration Properties

### CORS Configuration

-   `app.cors.allowed-origins`: Comma-separated list of allowed origins (default: `*`)
-   `app.cors.allowed-methods`: Comma-separated list of allowed HTTP methods (default: `GET,POST,PUT,DELETE,OPTIONS`)
-   `app.cors.allowed-headers`: Comma-separated list of allowed headers (default: `*`)

### API Configuration

-   `app.api.ted-api-key`: API key for TED API authentication (used as Bearer token)

## Environment-Specific Configuration

### Development

1. Copy `application-dev.properties.example` to `application-dev.properties`
2. Update the values as needed
3. Run with: `java -jar app.jar --spring.profiles.active=dev`

### Production

1. Copy `application-prod.properties.example` to `application-prod.properties`
2. Update the values as needed
3. Run with: `java -jar app.jar --spring.profiles.active=prod`

## Environment Variables

You can also override configuration using environment variables. Spring Boot automatically maps environment variables to properties using the following convention:

-   `APP_CORS_ALLOWED_ORIGINS` → `app.cors.allowed-origins`
-   `APP_CORS_ALLOWED_METHODS` → `app.cors.allowed-methods`
-   `APP_CORS_ALLOWED_HEADERS` → `app.cors.allowed-headers`
-   `APP_API_TED_API_KEY` → `app.api.ted-api-key`

Example:

```bash
export APP_API_TED_API_KEY="your-production-api-key"
export APP_CORS_ALLOWED_ORIGINS="https://yourdomain.com,https://api.yourdomain.com"
java -jar eforms-gpp-service.jar
```

## Docker Configuration

When running in Docker, you can pass environment variables:

```bash
docker run -e APP_API_TED_API_KEY="your-api-key" \
           -e APP_CORS_ALLOWED_ORIGINS="https://yourdomain.com" \
           -p 4420:4420 \
           your-app-image
```

## Security Notes

-   Never commit actual API keys to version control
-   Use environment variables or external secret management for sensitive values in production
-   Consider using more restrictive CORS settings in production environments
