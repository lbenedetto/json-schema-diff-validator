### JSON Schema Diff Validator

This project is a JSON Schema Diff Validator library for the JVM. It helps validate that changes to JSON schemas maintain backward compatibility, which is crucial for ensuring that API changes don't break existing clients.

It is "forked" from https://www.npmjs.com/package/json-schema-diff-validator, but is rewritten in Kotlin and has new features and more configuration options to allow more lenient validation of changes.

Namely, the ability to allow removing optional fields or allow making optional fields required.

### Core Features

- Validates that a new JSON schema is backward compatible with an old schema
- Configurable validation rules for different types of changes
- Detailed validation results with categorized issues (info, warnings, errors)

It is recommended to use this in combination with something like [com.github.victools:jsonschema-generator](https://github.com/victools/jsonschema-generator) in order to generate JSON schemas of your Java/Kotlin classes. 

This library was created to help ensure that changes to a Java object will not break deserialization of cached JSON data, and as such the validation defaults are optimized for that use case.

### How It Works

The validator compares two JSON schemas (old and new) and identifies changes that might break backward compatibility. It uses the [zjsonpatch](https://github.com/flipkart-incubator/zjsonpatch) library to compute the differences between schemas.

The validator checks for various types of changes, including:

- Removing optional/required fields
- Adding optional/required fields
- Changing field types
- Adding new enum values
- Reordering anyOf options
- Adding new oneOf/anyOf options

### Configuration Options

The validator can be configured with different compatibility levels for various types of changes:

- `newOneOf`: Whether new oneOf/anyOf items are a backwards compatible change (`FORBIDDEN` by default)
- `newEnumValue`: Whether new enum values are a backwards compatible change (`ALLOWED` by default)
- `anyOfReordering`: Whether reordering of anyOf items are a backwards compatible change (`ALLOWED` by default)
- `removingOptionalFields`: Whether removing optional fields is a backwards compatible change (`ALLOWED` by default)
- `makingFieldsRequired`: Whether making fields required is a backwards compatible change (`DISCOURAGED` by default)

Each option can be set to one of three compatibility levels:
- `ALLOWED`: Changes are permitted
- `DISCOURAGED`: Changes are allowed but generate warnings
- `FORBIDDEN`: Changes are not allowed and generate errors

### Usage Example

Basic usage with default configuration:
```kotlin
val result = Validator.validate("/path/to/schema-v1.json", "/path/to/schema-v2.json")
```

Usage with custom configuration:
```kotlin
// Custom configuration
val result = Validator.validate(oldSchemaPath, newSchemaPath, Config(
  newOneOf = Compatibility.ALLOWED,
  newEnumValue = Compatibility.ALLOWED,
  reorder = Compatibility.ALLOWED,
  removingOptionalFields = Compatibility.ALLOWED,
  makingFieldsRequired = Compatibility.DISCOURAGED
))
```

Java usage with custom configuration:
```java
var result = Validator.validate(oldSchemaPath, newSchemaPath, new Config()
    .makingFieldsRequired(Compatibility.DISCOURAGED));
```

### Dependencies

- Jackson for JSON processing
- zjsonpatch for computing JSON differences
