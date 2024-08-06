
# Services Observability and Monitoring

open4goods is built as a "modulith," meaning we are not using a microservices approach but aim to maintain good functional isolation between services. To support this intention, it is mandatory to have monitoring and observability mechanisms, especially at the service layer.

## Overview

**Observability** is addressed through standard Spring mechanisms, using [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html) and [Micrometer](https://docs.micrometer.io/micrometer/reference/). We focus on two main approaches:
- **Metrics**: Allow consistent comprehension of the execution duration of critical code.
- **Health Checks**: Expose the state (working/not working) of the services and aggregate them into an application HealthCheck, providing the global state of the application.

**Monitoring** is addressed through the use of a [Spring Boot Admin](https://codecentric.github.io/spring-boot-admin/current/) server.

This short documentation explains how to apply these principles to open4goods services. This documentation applies to both the UI and the API components.


## Adding a Custom Health Check

Each open4goods service must implement its custom health check. This process is straightforward:

* The service must implement the `HealthIndicator` interface.
* The service must override the `health()` method.
* The `health()` method must return the state of the service, either `Health.up()` if all is fine, or `Health.down()` if the service is no longer able to do its job.
*  In case of `Health.down()`, the associated description message MUST be expplicit.


### Computing the HealthCheck State

Computing the state is the -not so- tricky part and is completely dependent on the service's purpose. It can depend on various conditions such as exceptions, external dependencies, or resource availability.

Depending on the service behavior, you will probably use one of the following approaches:

#### Stateless Monitoring

If you can deduce the health status from existing resources, whose state does not need to be maintained in memory, this is considered stateless monitoring. This can be applied, for example, to checking the presence or minimum size of a file, or whether an external URL is responding.

#### Stateful Monitoring

In most cases, you will need to deduce the HealthCheck from the internal state of the service. For example, you might need to check that an instance variable is set or contains valid values (e.g., an internal hashmap or list is not empty).

It can also apply if you want to monitor exceptions that will cause the service to fail definitively. In this case, you would maintain an internal counter to record the number of critical exceptions thrown.

#### Mixed Approach

Of course, you can combine stateful and stateless checks. For instance, you might need to check that a file is present and that an internal map has some minimal values. In this scenario, ensure that you raise the appropriate message on `Health.down()` to provide good visibility on what went wrong for the monitors.



### Performance Concern

Health checks are queried quite often, meaning it is not advised to have long computations in the `health()` method. Ensure that health checks are efficient and do not introduce significant overhead.

## Adding a Custom Metric

Adding custom metrics is simple with Micrometer. Use the `@Timed` annotation on the method you want to monitor. Follow these rules:

- The name MUST be set and explicit.
- A description SHOULD be provided.
- A tag "service" MUST be provided to allow easier monitoring by administrators.

## Code Sample

Here is a dummy service that illustrates how to implement custom Metrics and custom Health Checks.

```java
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import io.micrometer.core.annotation.Timed;

@Component
public class MyTestService implements HealthIndicator {

    @Override
    public Health health() {
        return Health
            .up()
            .withDetail("MyTestService", "No problem")
            .build();
    }

    @Timed(value = "TestServiceJob", description = "This service does nothing", extraTags = {"service", "test"})
    public void doTheServiceJob() {
        System.out.println("I am a dummy service");
    }
}
```
### Explanation

**Health Check**: The `health()` method in the `MyTestService` class returns `Health.up()` with a detail message indicating no problems. Modify this logic to perform actual health checks relevant to your service.

**Custom Metric**: The `doTheServiceJob()` method is annotated with `@Timed` to record execution duration. The `value` parameter sets the metric name, the `description` provides additional context, and `extraTags` adds a tag for easier identification.

By following these guidelines, you can ensure that each service in open4goods is properly monitored and observable, aiding in the maintenance and reliability of the application.
