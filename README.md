### JSON Schema Diff Validator

This project is a JSON Schema Diff Validator library for the JVM. It helps validate that changes to JSON schemas maintain backward compatibility, which is crucial for ensuring that API changes don't break existing clients.

It was originally "forked" from https://www.npmjs.com/package/json-schema-diff-validator, but the logic has since been complete rewritten with a different approach which should be more robust and configurable at the same time.

### Core Features

- Validates that a new JSON schema is backward compatible with an old schema
- Configurable validation rules for different types of changes
- Detailed validation results with categorized issues (info, warnings, errors)

It is recommended to use this in combination with something like [com.github.victools:jsonschema-generator](https://github.com/victools/jsonschema-generator) in order to generate JSON schemas of your Java/Kotlin classes. Invalid JsonSchema json produces undefined results.

This library was created to help ensure that changes to a Java object will not break deserialization of cached JSON data, and as such the validation defaults are optimized for that use case.

### How It Works

The validator compares two JSON schemas (old and new) and identifies changes that might break backward compatibility. It uses the [zjsonpatch](https://github.com/flipkart-incubator/zjsonpatch) library to compute the differences between schemas.

### Configuration Options

The validator can be configured with different compatibility levels for various types of changes:

- `addingAnyOf`
- `removingAnyOf`
- `addingEnumValue`
- `removingEnumValue`
- `addingOptionalFields`
- `removingOptionalFields`
- `addingRequiredFields`
- `removingRequiredFields`
- `addingRequired`
- `removingRequired`

View the [Config](lib/src/main/kotlin/com/lbenedetto/Config.kt) class for full documentation of available options and their default values.

Each option can be set to one of three compatibility levels:
- `ALLOWED`: Detected changes will be added to the result as info
- `DISCOURAGED`: Detected changes will be added to the result as warning
- `FORBIDDEN`: Detected changes will be added to the result as error

### Usage Example

Basic usage with default configuration:
```kotlin
val result = Validator.validate("/path/to/schema-v1.json", "/path/to/schema-v2.json")
```

Usage with custom configuration:
```kotlin
// Custom configuration
val result = Validator.validate(oldSchemaPath, newSchemaPath, Config(
  addingRequired = Compatibility.DISCOURAGED
))
```

Java usage with custom configuration:
```java
var result = Validator.validate(oldSchemaPath, newSchemaPath, Config.defaultConfig()
    .addingRequired(Compatibility.DISCOURAGED));
```

### Dependencies

- Jackson for JSON processing
- zjsonpatch for computing JSON differences
