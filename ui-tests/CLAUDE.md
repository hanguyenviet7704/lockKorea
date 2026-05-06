# UI Test Module - CLAUDE.md

## Overview

This is a Selenium WebDriver-based UI automation framework for the LockerKorea e-commerce application.

## Architecture

### Technology Stack
- **Java 17** with Maven
- **Selenium WebDriver 4.21** for browser automation
- **WebDriverManager 5.6** for automatic driver management
- **JUnit 5** for test execution and assertions
- **MySQL Connector** for database verification
- **Apache Commons Lang3** for utilities

### Project Structure
```
ui-tests/
├── pom.xml
├── README.md                    # Setup and usage guide
├── CLAUDE.md                    # This file
└── src/
    └── test/
        ├── java/com/lockerkorea/ui/
        │   ├── pages/          # Page Object Model (POM) classes
        │   │   ├── BasePage.java
        │   │   ├── LoginPage.java
        │   │   ├── RegisterPage.java
        │   │   ├── HomePage.java
        │   │   ├── ProductListPage.java
        │   │   ├── ProductDetailPage.java
        │   │   ├── CartPage.java
        │   │   ├── OrderPage.java
        │   │   ├── UserProfilePage.java
        │   │   ├── HistoryOrderPage.java
        │   │   ├── ChangePasswordPage.java
        │   │   ├── ForgotPasswordPage.java
        │   │   ├── ProductManagePage.java
        │   │   ├── UserManagePage.java
        │   │   ├── OrderManagePage.java
        │   │   ├── OrderDetailPage.java
        │   │   ├── CategoryManagePage.java
        │   │   ├── VoucherManagePage.java
        │   │   ├── NewsManagePage.java
        │   │   ├── BannerManagePage.java
        │   │   └── AiManagementPage.java
        │   ├── utils/          # Framework utilities
        │   │   ├── BaseTest.java
        │   │   ├── ConfigReader.java
        │   │   ├── DBConnector.java
        │   │   └── CSVDataProvider.java
        │   ├── tests/          # Test classes (customer-facing)
        │   │   ├── SmokeTest.java
        │   │   ├── LoginTest.java
        │   │   ├── RegistrationTest.java
        │   │   ├── ProductBrowseTest.java
        │   │   ├── CartTest.java
        │   │   ├── UserJourneyTest.java
        │   │   ├── UserProfileTest.java
        │   │   ├── ForgotPasswordTest.java
        │   │   ├── ChangePasswordTest.java
        │   │   └── admin/       # Admin-specific tests
        │   │       ├── AdminProductTest.java
        │   │       ├── AdminUserManagementTest.java
        │   │       └── AdminOrderManagementTest.java
        │   └── runner/          # (optional) Test runners
        └── resources/
            ├── config/
            │   └── test-config.properties  # Test configuration
            ├── test-data/
            │   ├── users.csv
            │   ├── products.csv
            │   └── orders.csv
            ├── sql/
            │   ├── cleanup.sql
            │   └── seed.sql
            └── reports/         # Generated reports directory
```

## Design Principles

1. **Page Object Model (POM)**: All page interactions encapsulated in page classes
2. **Single Responsibility**: Each page class handles one page/component
3. **DRY**: Common utilities in BaseTest and utility classes
4. **Readability**: Clear test names with @DisplayName annotations
5. **Maintainability**: Locators centralized, easy to update when UI changes
6. **Reusability**: Page objects can be reused across multiple test classes

## Key Components

### BaseTest
- JUnit 5 lifecycle management (@BeforeEach, @AfterEach)
- WebDriver setup/teardown
- Automatic screenshots on failure
- Database connector initialization
- Common utility methods (click, type, wait, scroll, etc.)

### ConfigReader
- Reads configuration from `test-config.properties`
- Provides typed getters (String, int, boolean)
- Supports environment overrides via system properties

### DBConnector
- MySQL connection management
- SQL execution (queries and updates)
- Database cleanup/verification
- Transaction support (optional)
- Used for test data validation and rollback

### CSVDataProvider
- Reads test data from CSV files
- Supports parameterized tests
- Returns data as List<Map<String,String>> or Object[][]

## Test Categories

### Customer Flow Tests
- **SmokeTest**: Critical paths (home, products, login, register)
- **LoginTest**: Authentication scenarios
- **RegistrationTest**: User signup validation
- **ProductBrowseTest**: Search, filter, sort, pagination
- **CartTest**: Add, update, remove items
- **UserJourneyTest**: End-to-end flow (login → browse → cart → checkout → order)
- **UserProfileTest**: Profile management
- **ChangePasswordTest**: Password updates
- **ForgotPasswordTest**: Password reset flow

### Admin Flow Tests
- **AdminProductTest**: Product CRUD operations
- **AdminUserManagementTest**: User management (view, search, roles)
- **AdminOrderManagementTest**: Order processing, status updates

## Running Tests

### Prerequisites
1. Application services running (Docker: `docker-compose up -d`)
2. Java 17+ and Maven installed
3. Chrome/Firefox installed (or use headless mode)

### Run All Tests
```bash
cd ui-tests
mvn clean test
```

### Run Specific Test Class
```bash
mvn test -Dtest=LoginTest
mvn test -Dtest=CartTest
mvn test -Dtest=admin.AdminProductTest
```

### Run with Different Configuration
```bash
mvn test -Dtest=LoginTest -Denvironment=dev
mvn test -Dbrowser=firefox -Dheadless=true
```

### Pass Custom Properties
```bash
mvn test -Dfrontend.url=http://localhost:4200 -Ddb.password=secret
```

### Run with TestNG (if configured)
```bash
mvn test -DsuiteXmlFile=testng.xml
```

## Configuration

Edit `src/test/resources/config/test-config.properties`:

```properties
# Application URLs
frontend.url=http://localhost
backend.url=http://localhost:8089

# Database (for verification)
db.url=jdbc:mysql://localhost:3308/lockerkorea
db.username=root
db.password=admin

# Test Users
test.user.email=customer@test.com
test.user.password=Test@123456
test.admin.email=admin@test.com
test.admin.password=Admin@123456

# Browser
browser=chrome
headless=false
implicit.wait=10
explicit.wait=20

# Screenshots
screenshot.on.failure=true
screenshot.dir=target/screenshots
```

## Database Verification & Rollback

The framework includes DB utilities for:
- **Verification**: Check if data exists after UI actions (e.g., order created)
- **Cleanup**: Run SQL script before/after tests to isolate tests
- **Manual queries**: Execute custom SQL for assertions

Example:
```java
// In your test
dbConnector.recordExists("orders", "user_id = ?", userId);
dbConnector.executeUpdate("DELETE FROM cart WHERE user_id = ?", userId);
dbConnector.cleanupDatabase();  // Runs cleanup.sql
```

## Best Practices

1. **Use Page Objects**: Don't interact with raw Selenium in test classes
2. **Independent Tests**: Each test should be runnable alone (no dependencies)
3. **Explicit Waits**: Prefer `waitForVisibility()` over `Thread.sleep()`
4. **Meaningful Names**: Test methods should describe what they verify
5. **Assertions**: Always verify expected outcomes, not just "no exception"
6. **Data Management**: Use CSV for test data, DB for state cleanup
7. **Screenshots**: Failures auto-capture screenshots (configurable)
8. **Logging**: Use `System.out.println()` for debugging (visible in console)

## Test Data Management

- **CSV Files**: `src/test/resources/test-data/*.csv`
  - users.csv: Test user credentials
  - products.csv: Sample product data
  - orders.csv: Order test fixtures

- **Database Fixtures**: SQL scripts in `src/test/resources/sql/`
  - `cleanup.sql`: Removes test data (orders, cart items)
  - `seed.sql`: Ensures minimum baseline data exists

## Locator Strategy

Prefer selectors in this order:
1. **ID** (`By.id()`) - Most stable
2. **CSS Selector** (`By.cssSelector()`) - Fast, readable
3. **XPath** (`By.xpath()`) - Last resort (fragile)

Avoid: link text, partial link text, class name (unless unique)

Example selectors from actual UI:
```java
// Login page
USERNAME_INPUT = By.id("userName");
PASSWORD_INPUT = By.id("password");
LOGIN_BUTTON = By.cssSelector(".login-btn");

// Product page
PRODUCT_CARDS = By.cssSelector(".product-card");
DISCOUNT_BADGE = By.cssSelector(".discount-badge");
```

## Troubleshooting

### Element Not Found
- Check if page is fully loaded (use `waitForPageLoad()`)
- Verify selector is correct (use browser DevTools)
- Check for iframes (switch to frame if needed)
- Add explicit wait for dynamic content

### Tests Fail Intermittently
- Use explicit waits, not sleeps
- Increase timeout in config
- Check for AJAX calls that need to complete

### Database Connection Errors
- Verify MySQL is running on configured port (3308 for Docker)
- Check credentials in `test-config.properties`
- Ensure database `lockerkorea` exists

### WebDriver Issues
- WebDriverManager should auto-download drivers
- If fails, manually download driver and add to PATH
- Check browser version compatibility

### Docker Services Not Running
```bash
docker-compose ps  # Check status
docker-compose up -d  # Start all services
```

## Extending the Framework

### Adding a New Page
1. Create class extending `BasePage` in `pages/` package
2. Define locators as `private static final By`
3. Implement `navigateTo()` and `isLoaded()`
4. Add page-specific methods

Example:
```java
public class NewPage extends BasePage {
    private static final By CONTAINER = By.cssSelector(".new-page");

    @Override
    public void navigateTo() {
        driver.get(System.getProperty("frontend.url") + "/new-page");
    }

    @Override
    public boolean isLoaded() {
        return isElementDisplayed(CONTAINER);
    }

    public void doSomething() {
        click(By.id("button"));
    }
}
```

### Adding a New Test
1. Create class in `tests/` or `tests/admin/`
2. Extend `BaseTest`
3. Use `@Test` and `@DisplayName` annotations
4. Use page objects, not raw Selenium
5. Add assertions for verification

Example:
```java
@Test
@Order(1)
@DisplayName("New feature should work correctly")
void testNewFeature() {
    LoginPage login = new LoginPage(driver);
    login.navigateTo();
    login.login("user@test.com", "password");

    HomePage home = new HomePage(driver);
    assertTrue(home.isLoaded(), "Home should load");
}
```

## Integration with CI/CD

### GitHub Actions Example
```yaml
name: UI Tests
on: [push, pull_request]
jobs:
  ui-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Start services
        run: docker-compose up -d
      - name: Wait for services
        run: |
          sleep 60  # Wait for all services
      - name: Run UI tests
        run: |
          cd ui-tests
          mvn clean test
      - name: Archive reports
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: test-reports
          path: ui-tests/target/surefire-reports/
```

## Notes

- Tests are **not** fully isolated by default. For isolation, uncomment `dbConnector.cleanupDatabase()` in `BaseTest.setUp()`.
- Some tests may fail if test data doesn't exist in database. Update `users.csv` with valid credentials that match your test database.
- Admin tests require admin user to exist (update `test-config.properties`).
- The UI uses Angular with PrimeNG components, so selectors must be stable.
- Consider adding Allure or ExtentReports for richer HTML reports (currently using JUnit basic reports).

## Future Improvements

- [ ] Implement Allure reporting
- [ ] Add parallel test execution
- [ ] Create test data builder pattern
- [ ] Add API layer for direct backend verification
- [ ] Implement retry logic for flaky tests
- [ ] Add visual regression testing (Applitools/Screenshot comparison)
- [ ] Dockerize test execution environment
- [ ] Add video recording for test runs
- [ ] Implement data-driven tests with Excel support
- [ ] Add cross-browser testing matrix (Chrome, Firefox, Edge)
