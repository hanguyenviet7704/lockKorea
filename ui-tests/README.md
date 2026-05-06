# UI Test Suite - Maven Project

This directory contains the Selenium WebDriver UI automation tests for the LockerKorea e-commerce application.

## Quick Start

### 1. Setup Environment
```bash
# Ensure services are running
docker-compose up -d  # From project root

# Wait for services to be ready (especially frontend on port 80)
# Check: http://localhost should show the application
```

### 2. Configure Tests
Edit `src/test/resources/config/test-config.properties` if needed:
- Update `test.user.email` and `test.user.password` with valid test credentials
- Ensure database credentials match your Docker setup (default: root/admin on port 3308)
- Choose browser: chrome, firefox, edge
- Set `headless=true` for CI/CD environments

### 3. Run Tests
```bash
cd ui-tests

# Compile and run all tests
mvn clean test

# Run specific test
mvn test -Dtest=SmokeTest

# Run with headless Chrome (for CI)
mvn test -Dbrowser=chrome -Dheadless=true

# Run with Firefox
mvn test -Dbrowser=firefox

# Run admin tests
mvn test -Dtest=admin.AdminProductTest
```

## Test Organization

### Test Classes
- **SmokeTest**: Basic smoke tests for all critical paths
- **LoginTest**: Authentication scenarios (valid/invalid, remember me, navigation)
- **RegistrationTest**: User signup with validation tests
- **ProductBrowseTest**: Search, filter, sort, pagination, product details
- **CartTest**: Add/remove items, quantity updates, checkout flow
- **UserJourneyTest**: Complete E2E flow (login → browse → cart → checkout → order)
- **UserProfileTest**: Profile viewing, navigation to history/change password
- **ForgotPasswordTest**: Password reset flow
- **ChangePasswordTest**: Change password with validation
- **admin/**: Admin-specific tests for management features

### Page Objects
All UI interactions are in `src/main/java/com/lockerkorea/ui/pages/`:
- BasePage: Common page functionality
- LoginPage, RegisterPage, HomePage
- ProductListPage, ProductDetailPage, CartPage, OrderPage
- UserProfilePage, HistoryOrderPage, ChangePasswordPage, ForgotPasswordPage
- Admin pages: ProductManagePage, UserManagePage, OrderManagePage, etc.

### Utilities
- **BaseTest**: Setup/teardown, WebDriver management, screenshots
- **ConfigReader**: Read configuration from properties file
- **DBConnector**: Database operations for verification
- **CSVDataProvider**: Read test data from CSV files

## Test Data

### CSV Files (src/test/resources/test-data/)
- `users.csv`: Test user credentials (email, password, role)
- `products.csv`: Sample product data
- `orders.csv`: Order test fixtures

### Database
Tests can verify database state using `DBConnector`:
```java
boolean exists = dbConnector.recordExists("orders", "user_id = ?", userId);
List<Map<String, Object>> results = dbConnector.executeQuery("SELECT * FROM users WHERE email = ?", email);
```

Cleanup SQL in `src/test/resources/sql/cleanup.sql` removes test orders/cart items.

## Framework Features

### Browser Management
- Automatic driver download via WebDriverManager
- Support for Chrome, Firefox, Edge
- Headless mode for CI/CD
- Configurable timeouts (implicit, explicit)
- Auto-maximize window (configurable)

### Waiting Strategy
- **Explicit waits** preferred (use `waitForVisibility()`, `waitForClickable()`)
- Page load detection via `document.readyState`
- No hardcoded `Thread.sleep()` in page objects

### Screenshots
- Automatic on test failure (configurable)
- Saved to `target/screenshots/` with timestamp
- Filename includes test name for easy identification

### Database Integration
- Connect to MySQL for verification
- Run cleanup scripts before/after tests
- Execute queries to validate UI actions
- Transaction rollback support (configurable)

## Writing New Tests

### Step 1: Create/Use a Page Object
```java
public class MyPage extends BasePage {
    private static final By MY_ELEMENT = By.id("myElement");

    public void navigateTo() {
        driver.get(System.getProperty("frontend.url") + "/my-page");
    }

    @Override
    public boolean isLoaded() {
        return isElementDisplayed(MY_ELEMENT);
    }

    public void doAction() {
        click(MY_ELEMENT);
    }
}
```

### Step 2: Write Test Class
```java
public class MyTest extends BaseTest {

    @Test
    @DisplayName("My test case description")
    void testMyFeature() {
        MyPage page = new MyPage(driver);
        page.navigateTo();
        assertTrue(page.isLoaded(), "Page should load");

        page.doAction();

        // Verify outcome
        assertTrue(someCondition, "Action should have expected result");
    }
}
```

### Step 3: Add Test Data (Optional)
Add entries to `users.csv` or create CSV in `test-data/` and read using `CSVDataProvider`.

### Step 4: Run and Debug
```bash
mvn test -Dtest=MyTest
```

## Configuration Options

All config in `test-config.properties`:

| Property | Default | Description |
|----------|---------|-------------|
| `frontend.url` | http://localhost | Frontend application URL |
| `backend.url` | http://localhost:8089 | Backend API URL |
| `db.url` | jdbc:mysql://localhost:3308/... | Database JDBC URL |
| `browser` | chrome | Browser: chrome, firefox, edge |
| `headless` | false | Run browser without UI |
| `implicit.wait` | 10 | Implicit wait in seconds |
| `explicit.wait` | 20 | Explicit wait in seconds |
| `screenshot.on.failure` | true | Capture screenshot on failure |

## Common Issues & Solutions

### "Connection refused" to database
- Check MySQL is running: `docker-compose ps mysql`
- Verify port is 3308 (host) / 3306 (container)
- Check credentials match `docker-compose.yml`

### "Element not found" errors
- Page may not be fully loaded - use `waitForVisibility()`
- Selector might be wrong - verify in browser DevTools
- Element inside iframe? Use `switchToFrame()` first
- Dynamic content? Add explicit wait for the element

### Tests fail on first run
- Ensure application is fully started before running tests
- Wait at least 60 seconds after `docker-compose up -d` for all services
- Verify frontend accessible: http://localhost

### WebDriver download fails
- Check internet connection
- Manually download driver and add to PATH
- Or use local driver: set `webdriver.chrome.driver` system property

### Tests are flaky (intermittent failures)
- Increase `explicit.wait` in config
- Add explicit waits for AJAX calls
- Check for animations that need to complete
- Consider adding retry logic

## Advanced Usage

### Parameterized Tests
```java
@ParameterizedTest(name = "Login with {0} and {1}")
@CsvSource({
    "valid@test.com, correctpass, should succeed",
    "invalid@test.com, wrongpass, should fail"
})
void testLogin(String email, String password, String expected) {
    // Test logic
}
```

### Database Verification
```java
@Test
void testOrderInDatabase() throws SQLException {
    // Place order via UI
    orderPage.placeOrder();
    String orderId = orderPage.getOrderIdFromConfirmation();

    // Verify in database
    boolean exists = dbConnector.recordExists("orders", "id = ?", orderId);
    assertTrue(exists, "Order should exist in database");
}
```

### Custom Screenshots
```java
@Test
void testWithCustomScreenshot() {
    // Your test
    takeScreenshot("after-action");  // From BaseTest
}
```

## Performance Tips

1. Use `headless=true` for CI (faster, no GUI)
2. Reuse browser session for related tests (currently creates new session per test)
3. Consider parallel execution for large test suites (requires test isolation)
4. Avoid unnecessary navigation (use direct URLs or reuse page objects)

## CI/CD Integration

Example GitHub Actions workflow:
```yaml
- name: Run UI tests
  run: |
    docker-compose up -d
    sleep 60  # Wait for services
    cd ui-tests
    mvn clean test -Dheadless=true
```

Reports are in `ui-tests/target/surefire-reports/`:
- `*.xml` - JUnit XML (CI integration)
- Console output - Test results summary

## Contributing

When adding new tests:
1. Follow existing naming conventions (XXXTest.java)
2. Use Page Object Model (don't use raw Selenium in tests)
3. Add meaningful `@DisplayName` for test reports
4. Keep tests independent (no dependencies between tests)
5. Use explicit waits, not Thread.sleep()
6. Add assertions for verification (don't just test for no exception)
7. Update this README if adding new test categories

## Resources

- [Selenium Documentation](https://www.selenium.dev/documentation/)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [WebDriverManager](https://github.com/bonigarcia/webdrivermanager)
- Project structure: See `CLAUDE.md` for full application context

## Support

Issues? Check:
1. Application is running (docker-compose up -d)
2. Test credentials in `test-config.properties` are valid
3. Database is accessible (port 3308)
4. Frontend URL is correct (port 80 for Docker, 4200 for dev)
5. Browser is installed (Chrome/Firefox/Edge)
6. WebDriverManager can download drivers (check firewall/proxy)

For framework issues, review `CLAUDE.md` and this README.
