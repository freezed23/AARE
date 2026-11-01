# Retry after a proxy fails before the version request

2026-11-02 — source change, not built or device verified.

The historical AA1/DHU failure was reported as `no version request from head unit`, with both proxy byte counters at 0 B. This is before TLS and differs from a certificate refusal. The raw failing session has not been re-examined in this change.

A one-shot LAN connection previously consumed network mode even when it failed before receiving the HU's initial version request. `CarSession.failedBeforeVersion` now records an error ending in VERSION, and `ProjectionService` keeps the already-bound LAN listener for a fresh proxy retry, like the existing pending-Trust exception. Explicit Stop has no error and still closes one-shot network access; failures after VERSION retain existing behavior. No automatic connection, certificate trust bypass or longer timeout is introduced. Existing network-mode service destruction behavior is retained.

Paired aa-proxy-rs change waits for a real HU request before opening a direct phone-server TCP connection and corrects fixed-prefix version diagnostics. Phone-initiated wireless TCP is a separate path. Neither change establishes the USB/FunctionFS root cause or guarantees vehicle connection.

Based on main e8da04dc422abd36be60a81bb9fc7f782e8d7e84, separate from the audio documentation PR #1. Source inspection, diff checks and Tree-sitter syntax checks for both changed Kotlin files passed; these are not compilation/type checks. no Gradle/tests/APK/CI/install/road test. Commit uses [skip ci] to respect the owner's no-build instruction.
