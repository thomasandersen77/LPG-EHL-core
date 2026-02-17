# Manual Tests for Nets Cloud Connect Terminal

## NetsCloudConnectTerminalClientTestManual

This test suite is designed to test the Nets Cloud Connect terminal client against **real hardware**. It is disabled by default to prevent accidental execution in CI/CD pipelines.

### Prerequisites

1. **Terminal Hardware**: Ensure the Nets payment terminal is connected and powered on
2. **Network Access**: The terminal must be able to reach `https://connectcloud.aws.nets.eu`
3. **Physical Card**: Some tests require a physical payment card to tap on the terminal

### Configuration

The tests use configuration from `application-nets-cloud.yaml` or environment variables:

```bash
export NETS_BASE_URL="https://connectcloud.aws.nets.eu"
export NETS_USERNAME="your_username"
export NETS_PASSWORD="your_password"
export NETS_TERMINAL_ID="your_terminal_id"
```

Default values (from application-nets-cloud.yaml):
- Username: `cloudberries_shared`
- Password: `B8PnVjmVq-SMM9QD`
- Terminal ID: `42696609`

### Running the Tests

#### Option 1: Remove @Disabled annotation
Comment out or remove the `@Disabled` annotation in the test file, then run:

```bash
mvn test -Dtest=NetsCloudConnectTerminalClientTestManual
```

#### Option 2: Run with IDE
Most IDEs (IntelliJ IDEA, etc.) allow you to run individual tests even if they're disabled by right-clicking and selecting "Run Test".

### Test Coverage

The manual test suite includes 10 tests that cover:

1. **Health Check (Before Opening)** - Verifies health status when terminal is closed
2. **Status Check (Before Opening)** - Verifies connection status when terminal is closed
3. **Open Terminal** - Tests establishing connection to the real terminal
4. **Health Check (After Opening)** - Verifies health status when terminal is open
5. **Status Check (After Opening)** - Verifies connection status when terminal is open
6. **Purchase (100 NOK)** - Tests a real purchase transaction
   - ⚠️ Requires physical card tap within 60 seconds
7. **Reversal** - Tests reversing a previous transaction
8. **Purchase Timeout** - Tests timeout scenario (DO NOT tap card)
9. **Close Terminal** - Tests closing the terminal connection
10. **Full Workflow** - Tests complete open → purchase → close flow
    - ⚠️ Requires physical card tap

### Important Notes

- **Purchase Tests**: Tests requiring card payment (#6, #7, #10) will wait 60 seconds for card tap
- **Test Order**: Tests are ordered using `@TestMethodOrder` to ensure proper execution sequence
- **Cleanup**: Each test includes cleanup in `@AfterEach` to close connections
- **Logging**: Extensive logging is included to help diagnose issues with real hardware

### Expected Output

When running successfully, you should see:
- Terminal opening with WebSocket connection
- Terminal ready confirmation
- Purchase prompts on the terminal display
- Transaction results including receipts
- Proper cleanup and closure

### Troubleshooting

If tests fail, check:
1. Terminal is powered on and connected
2. Network connectivity to Nets Cloud Connect servers
3. Credentials are correct
4. Terminal is not already in use by another application
5. Physical card is tapped within the timeout period for purchase tests
