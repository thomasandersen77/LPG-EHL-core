# ApplicationContext Failure Threshold Investigation Report

**Date:** 2026-02-16  
**Issue:** ApplicationContext failure threshold exceeded in lpg-ehl-api test classes  
**Status:** In Progress - Dependency conflict identified  

## Executive Summary

Integration tests in the `lpg-ehl-webapp` module are failing with "ApplicationContext failure threshold (1) exceeded" errors. Root cause identified as a missing Apache HttpClient 5 dependency required by Spring Boot 3.2.5's HTTP client autoconfiguration.

## Problem Description

When running `mvn clean install`, all 18 integration tests in the lpg-ehl-webapp module fail with:

```
java.lang.IllegalStateException: ApplicationContext failure threshold (1) exceeded: 
skipping repeated attempt to load context
```

This error occurs because Spring Boot fails to initialize the ApplicationContext once, and then refuses to retry for subsequent tests.

## Root Cause Analysis

### Primary Issue: Missing Apache HttpClient 5 Dependency

The actual cause of the ApplicationContext failure is:

```
Caused by: java.lang.NoClassDefFoundError: org/apache/hc/client5/http/ssl/TlsSocketStrategy
    at org.springframework.boot.http.client.HttpComponentsHttpClientBuilder.<init>
    at org.springframework.boot.http.client.HttpComponentsClientHttpRequestFactoryBuilder.<init>
```

**Analysis:**
- Spring Boot 3.2.5's `ImperativeHttpClientAutoConfiguration` attempts to auto-detect and configure HTTP client libraries
- It finds the Apache HttpComponents classes on the classpath but is missing the `httpclient5` artifact
- The `TlsSocketStrategy` class is part of `org.apache.httpcomponents.client5:httpclient5`
- Without this dependency, the bean creation fails, causing the entire ApplicationContext to fail to load

### Secondary Issue: Bean Definition Override Conflict

An additional issue was discovered during investigation:

```
BeanDefinitionOverrideException: Invalid bean definition with name 'restClientSsl'
```

Two autoconfiguration classes are trying to register the same bean:
- `org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration`
- `org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration`

This appears to be a known issue in Spring Boot 3.2.x versions.

## Changes Made

### 1. Spring Boot Version Upgrade
**File:** `pom.xml`  
**Change:** Upgraded from Spring Boot 3.2.1 → 3.2.5

```xml
<spring-boot.version>3.2.5</spring-boot.version>
```

**Rationale:** Newer patch version may have fixes for autoconfiguration issues.

### 2. Added Bean Definition Override Property
**File:** `lpg-ehl-webapp/src/test/resources/application-test.yaml`  
**Change:** Added property to allow bean definition overriding

```yaml
spring:
  main:
    allow-bean-definition-overriding: true
```

**Rationale:** Workaround for the RestClientSsl bean conflict.

### 3. Added Apache HttpClient 5 Dependency
**File:** `lpg-ehl-webapp/pom.xml`  
**Change:** Added httpclient5 dependency

```xml
<!-- Apache HttpClient 5 - Required by Spring Boot HTTP client autoconfiguration -->
<dependency>
    <groupId>org.apache.httpcomponents.client5</groupId>
    <artifactId>httpclient5</artifactId>
</dependency>
```

**Rationale:** Provides the missing `TlsSocketStrategy` class required by Spring Boot's HTTP client builder.

## Current Status

**❌ NOT RESOLVED**

After adding the httpclient5 dependency, the `NoClassDefFoundError` persists:
```
Caused by: java.lang.NoClassDefFoundError: org/apache/hc/client5/http/ssl/TlsSocketStrategy
```

This suggests one of the following:
1. Maven dependency resolution issue - the artifact is not being downloaded
2. Version conflict - httpclient5 version from Spring Boot BOM might be incompatible
3. Additional transitive dependencies are missing (e.g., httpcore5)
4. Classpath issue during test execution

## Test Results

### Before Fix
- **Tests Run:** 18
- **Failures:** 0  
- **Errors:** 18 (all failed due to ApplicationContext not loading)
- **Skipped:** 0

### After Adding httpclient5 Dependency
- **Tests Run:** 18
- **Failures:** 0
- **Errors:** 18 (same error persists)
- **Skipped:** 0

## Affected Test Classes

1. `no.cloudberries.lpg.api.integration.DiagnosticsIntegrationTest` (7 tests)
   - testClearFault
   - testRecordCriticalFaultAppearInDiagnostics
   - testRecordWarningFault
   - testGetDispensersWithFaults
   - testGetCriticalFaults
   - testGetDiagnosticsForUnknownDispenser
   - testGetDiagnosticsEmpty

2. `no.cloudberries.lpg.api.integration.ApiIntegrationTest` (11 tests)
   - should get health status without authentication
   - should allow access to transactions without token in test profile
   - should get empty transactions list with valid token
   - should get transaction by ID
   - should return 404 for non-existent transaction
   - should get transactions with pagination
   - should filter transactions by dispenser address
   - should get all dispensers
   - should get dispenser by address
   - should return 404 for non-existent dispenser
   - should get transaction count

## Recommended Next Steps

### Immediate Actions

1. **Verify Maven Dependency Resolution**
   ```bash
   mvn dependency:tree -pl lpg-ehl-webapp | grep httpclient
   ```
   Check if httpclient5 and its transitive dependencies are being resolved correctly.

2. **Explicitly Add httpcore5 Dependency**
   The httpclient5 library depends on httpcore5. Try adding it explicitly:
   ```xml
   <dependency>
       <groupId>org.apache.httpcomponents.core5</groupId>
       <artifactId>httpcore5</artifactId>
   </dependency>
   ```

3. **Check for Version Conflicts**
   ```bash
   mvn dependency:tree -pl lpg-ehl-webapp -Dverbose
   ```
   Look for version conflicts or exclusions affecting Apache HttpComponents.

4. **Alternative: Exclude HTTP Client Autoconfiguration**
   If the HTTP client is not needed for tests, exclude the autoconfiguration:
   ```yaml
   spring:
     autoconfigure:
       exclude:
         - org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration
         - org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration
   ```

### Long-term Solutions

1. **Upgrade to Spring Boot 3.3.x or 3.4.x**
   - These versions may have better autoconfiguration handling
   - Bean definition override conflicts might be resolved

2. **Review Test Configuration**
   - Consider if full ApplicationContext is needed for all tests
   - Use `@WebMvcTest` or `@DataJpaTest` for focused testing where appropriate
   - Mock HTTP clients if not actually used in tests

3. **Document HTTP Client Requirements**
   - If HTTP client is intentionally used, document why and which features
   - If not needed, explicitly exclude to avoid unnecessary dependencies

## Technical Details

### Module Structure
```
lpg-ehl-parent (Spring Boot 3.2.5)
├── lpg-ehl-core (163 tests passing)
├── lpg-ehl-service
├── lpg-ehl-api (6 tests skipped)
└── lpg-ehl-webapp (18 tests failing)
    ├── ApiIntegrationTest.kt
    └── DiagnosticsIntegrationTest.kt
```

### Test Configuration
- **Profile:** test
- **Database:** H2 in-memory (jdbc:h2:mem:lpg_test)
- **Web Environment:** RANDOM_PORT
- **Base Class:** `BaseIntegrationTest` (extends @SpringBootTest)

### Error Trace Summary
```
Failed to load ApplicationContext
  ↓
BeanCreationException: Error creating bean 'restClientSsl'
  ↓
BeanCreationException: Error creating bean 'clientHttpRequestFactoryBuilder'
  ↓
BeanInstantiationException: Failed to instantiate ClientHttpRequestFactoryBuilder
  ↓
NoClassDefFoundError: org/apache/hc/client5/http/ssl/TlsSocketStrategy
  ↓
ClassNotFoundException: org.apache.hc.client5.http.ssl.TlsSocketStrategy
```

## References

- [Spring Boot 3.2.5 Release Notes](https://github.com/spring-projects/spring-boot/releases/tag/v3.2.5)
- [Apache HttpClient 5 Documentation](https://hc.apache.org/httpcomponents-client-5.x/)
- [Spring Boot HTTP Client Autoconfiguration](https://docs.spring.io/spring-boot/docs/current/reference/html/io.html#io.rest-client)

## Appendix: Commands Used

### Running Tests
```bash
# Test single module
mvn test -pl lpg-ehl-webapp

# Full build
mvn clean install

# Clean and compile
mvn clean compile -pl lpg-ehl-webapp
```

### Debugging
```bash
# Check dependencies
mvn dependency:tree -pl lpg-ehl-webapp

# Verbose dependency resolution
mvn dependency:tree -pl lpg-ehl-webapp -Dverbose

# Check for specific library
mvn dependency:tree -pl lpg-ehl-webapp | grep httpclient
```

---

**Investigation conducted by:** Warp AI Agent  
**Report generated:** 2026-02-16T22:19:00+01:00
