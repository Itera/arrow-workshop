# 8. Resource safety

Builds on stages 1–7. A session-based courier client whose session is released on success, typed error, exception,
caller cancellation, and sibling cancellation.

- `providers/ProviderSession.kt`: `Resource<ProviderSession>`, `withSession` using `resourceScope`, and `SessionCourierProvider`.

```bash
./gradlew :08-resource-safety:test
```
