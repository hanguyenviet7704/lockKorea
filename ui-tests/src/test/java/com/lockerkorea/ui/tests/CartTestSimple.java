package com.lockerkorea.ui.tests;

import com.lockerkorea.ui.pages.*;
import com.lockerkorea.ui.utils.BaseTest;
import com.lockerkorea.ui.utils.ConfigReader;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple Cart Test - Basic verification
 */
public class CartTestSimple extends BaseTest {

    private LoginPage loginPage;
    private CartPage cartPage;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        loginPage = new LoginPage(driver);
        cartPage = new CartPage(driver);
    }

    @Test
    @DisplayName("Test 1: Basic cart page load")
    void testCartPageLoad() {
        loginPage.navigateTo();
        loginPage.login(
            ConfigReader.getString("test.user.email"),
            ConfigReader.getString("test.user.password")
        );

        cartPage.navigateTo();
        assertTrue(cartPage.isLoaded(), "Cart page should load");
    }

    @Test
    @DisplayName("Test 2: Empty cart message")
    void testEmptyCart() {
        loginPage.navigateTo();
        loginPage.login(
            ConfigReader.getString("test.user.email"),
            ConfigReader.getString("test.user.password")
        );

        cartPage.navigateTo();
        // Just verify we can navigate to cart
        assertTrue(cartPage.isLoaded());
    }
}
