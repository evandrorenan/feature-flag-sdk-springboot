# Feature Flag SDK Spring Boot

A Spring Boot SDK for managing feature flags with support for dynamic targeting rules using JSON Logic. This SDK implements the OpenFeature specification and provides a custom provider for feature flag management with PostgreSQL persistence.

## Features

- OpenFeature specification compliance
- PostgreSQL-based feature flag storage
- JSON Logic-based targeting rules
- Support for multiple flag types (String, Boolean, Number, Object)
- Hikari connection pool integration
- Spring Boot 3.x compatibility
- MapStruct for efficient object mapping

## Prerequisites

- Java 17 or higher
- PostgreSQL database
- Maven 3.x

## Installation

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>br.com.evandrorenan</groupId>
    <artifactId>feature-flag-sdk-springboot</artifactId>
    <version>2.0.0</version>
</dependency>
```

## Configuration

Create an `application.yml` file with the following configuration:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/flagsDB
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      idle-timeout: 30000
      max-lifetime: 1800000
      connection-timeout: 30000
```

## Usage

### Flag Model

The SDK uses a `Flag` model with the following structure:

```java
public class Flag {
   private Long id;
   private String flagName;
   private State state;           // ENABLED or DISABLED
   private FlagType flagType;     // BOOLEAN, STRING, NUMBER, OBJECT
   private String defaultVariant;
   private Map<String, String> variants;
   private String targeting;      // JSON Logic rules
}
```

### Feature Flag Evaluation

The SDK provides a custom OpenFeature provider that evaluates flags based on:
1. Flag existence
2. Flag state (enabled/disabled)
3. Targeting rules (using JSON Logic)
4. Default variants

Example usage:

```java
@Autowired
private FeatureProvider provider;

public void example() {
    EvaluationContext context = new ImmutableContext();
    ProviderEvaluation<String> result = provider.getStringEvaluation(
        "my-feature",
        "default-value",
        context
    );
    String value = result.getValue();
}
```

### Repository Operations

The SDK provides a `FeatureFlagRepository` interface with the following operations:

```java
public interface FeatureFlagRepository extends CrudRepository<FlagDAO, Long> {
    FlagDAO findByName(String name);
    List<FlagDAO> findAllStringFlags();
}
```

## Building from Source

```bash
mvn clean install
```

## Testing

```bash
mvn test
```

The project uses JaCoCo for code coverage analysis. Reports can be found in `target/site/jacoco/` after running tests.

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

