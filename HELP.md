# Read Me First

The following was discovered as part of building this project:

-   The original package name 'it.polimi.gpplib.eforms-gpp-service' is invalid and this project uses 'it.polimi.gpplib.eforms_gpp_service' instead.

# eForms GPP Service

This service provides REST API endpoints for Green Public Procurement (GPP) analysis of eForms notices.

## API Documentation

The service includes comprehensive OpenAPI/Swagger documentation accessible at:

-   **Swagger UI**: http://localhost:4420/swagger-ui.html
-   **OpenAPI JSON**: http://localhost:4420/api-docs

### Available Endpoints

-   `POST /api/v1/analyze-notice` - Analyze a procurement notice for GPP criteria
-   `POST /api/v1/suggest-patches` - Suggest GPP patches based on criteria
-   `POST /api/v1/apply-patches` - Apply GPP patches to a notice
-   `POST /api/v1/visualize-notice` - Convert notice to HTML visualization
-   `POST /api/v1/validate-notice` - Validate notice using TED API

### Using the API

1. Start the application: `mvn spring-boot:run`
2. Open your browser to http://localhost:4420/swagger-ui.html
3. Use the interactive documentation to test endpoints
4. All endpoints accept JSON and return JSON responses

## Getting Started

### Reference Documentation

For further reference, please consider the following sections:

-   [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
-   [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/3.5.0/maven-plugin)
-   [Create an OCI image](https://docs.spring.io/spring-boot/3.5.0/maven-plugin/build-image.html)
-   [Spring Web](https://docs.spring.io/spring-boot/3.5.0/reference/web/servlet.html)
-   [Spring Boot DevTools](https://docs.spring.io/spring-boot/3.5.0/reference/web/servlet.html)
-   [Spring Boot Actuator](https://docs.spring.io/spring-boot/3.5.0/reference/actuator/index.html)
-   [SpringDoc OpenAPI](https://springdoc.org/)

### Guides

The following guides illustrate how to use some features concretely:

-   [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
-   [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
-   [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)
-   [Building a RESTful Web Service with Spring Boot Actuator](https://spring.io/guides/gs/actuator-service/)

### Maven Parent overrides

Due to Maven's design, elements are inherited from the parent POM to the project POM.
While most of the inheritance is fine, it also inherits unwanted elements like `<license>` and `<developers>` from the parent.
To prevent this, the project POM contains empty overrides for these elements.
If you manually switch to a different parent and actually want the inheritance, you need to remove those overrides.
