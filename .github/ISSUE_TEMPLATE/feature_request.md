---
name: Feature request
about: Suggest a new DSL primitive, domain helper, or other improvement
labels: enhancement
---

## Problem or motivation

<!-- What are you trying to match that the current DSL cannot express cleanly? -->

## Proposed API

```kotlin
// How you imagine the new method would look and be used
val p = kexpresso {
    lookahead { literal("!") }
}
p.containsMatchIn("Espresso!") // true
```

## Alternatives considered

<!-- Any other approaches you explored, including raw Regex workarounds. -->

## Additional context

<!-- Links, references, or anything else that would help evaluate the request. -->
