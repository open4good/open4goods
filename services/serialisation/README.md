# Serialisation Service

This module is part of the [Open4Goods project](https://github.com/open4good/open4goods) and provides functionality for serializing and deserializing objects to/from JSON and YAML formats. It also offers methods for Base64 encoding/decoding of strings.

## Features

- **JSON and YAML Serialization:**  
  Serialize objects to JSON or YAML with support for pretty printing.
  
- **Deserialization:**  
  Deserialize JSON or YAML strings back into Java objects.
  
- **Deep Cloning:**  
  Create deep clones of objects using JSON serialization.
  
- **Binary Serialization:**  
  Serialize objects to a binary JSON representation.
  
- **Base64 Compression/Decompression:**  
  Encode strings to Base64 and decode them back.


## How to Use

1. **Include the Dependency**  
   Ensure that your Maven project includes the `serialisation` module as a dependency.

2. **Inject the Service**  
   Use constructor injection in your Spring components:
   ```java
   @Service
   public class MyService {
       private final SerialisationService serialisationService;

       public MyService(SerialisationService serialisationService) {
           this.serialisationService = serialisationService;
       }
       
       public void process() {
           // Example usage:
           MyObject obj = new MyObject(...);
           try {
               String json = serialisationService.toJson(obj, true);
               // Process JSON...
           } catch (SerialisationException e) {
               // Handle exception
           }
       }
   }
   ```

3. **Running Tests**  
   A comprehensive test suite is available. To run tests, use:
   ```
   mvn test
   ```
   The module uses an `application-test.yml` for test-specific configurations.


## Build & Test

Build from this directory:
```bash
mvn clean install
```

Run tests only:
```bash
mvn test
```

You can also build from the repository root:
```bash
mvn -pl services/serialisation -am clean install
```

## Additional Information

For more details, refer to the Javadoc comments in the source code and the unit tests in the `src/test` directory.

---

Happy coding!

For project-wide information see the [main README](../../README.md).

