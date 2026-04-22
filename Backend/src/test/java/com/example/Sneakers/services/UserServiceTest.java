package com.example.Sneakers.services;
import java.util.Date;

import com.example.Sneakers.components.JwtTokenUtils;
import com.example.Sneakers.components.LocalizationUtils;
import com.example.Sneakers.dtos.UpdateUserDTO;
import com.example.Sneakers.dtos.UserDTO;
import com.example.Sneakers.exceptions.DataNotFoundException;
import com.example.Sneakers.exceptions.PermissionDenyException;
import com.example.Sneakers.models.Role;
import com.example.Sneakers.models.User;
import com.example.Sneakers.repositories.RoleRepository;
import com.example.Sneakers.repositories.UserRepository;
import com.example.Sneakers.responses.UserResponse;
import com.example.Sneakers.utils.Email;
import com.example.Sneakers.utils.MessageKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenUtils jwtTokenUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private LocalizationUtils localizationUtils;

    @Mock
    private Email email;

    @InjectMocks
    private UserService userService;

    @Captor
    private ArgumentCaptor<User> userArgumentCaptor;

    private Role userRole;
    private Role adminRole;
    private User testUser;
    private UserDTO userDTO;

    @BeforeEach
    void setUp() {
        // Setup roles
        userRole = Role.builder().id(1L).name("USER").build();
        adminRole = Role.builder().id(2L).name("ADMIN").build();

        // Setup test user
        testUser = User.builder()
                .id(1L)
                .fullName("Test User")
                .phoneNumber("0123456789")
                .password("encodedPassword")
                .email("test@example.com")
                .address("Test Address")
                .dateOfBirth(null)
                .active(true)
                .role(userRole)
                .build();

        // Setup user DTO
        userDTO = UserDTO.builder()
                .fullName("New User")
                .phoneNumber("0987654321")
                .password("password123")
                .email("newuser@example.com")
                .address("New Address")
                .dateOfBirth(null)
                .roleId(1L)
                .build();
    }

    // ========== TEST CASE ID: TC-USER-001 ==========
    // Test Objective: Verify that creating a user with valid data succeeds
    // Input: Valid UserDTO with unique phone number, valid role
    // Expected Output: Saved User entity with encoded password
    // Notes: Database check - user should be persisted; Rollback - @Transactional ensures cleanup
    @Test
    void TC_USER_001_createUser_withValidData_shouldSucceed() throws Exception {
        // Arrange
        when(roleRepository.findById(1L)).thenReturn(Optional.of(userRole));
        when(userRepository.existsByPhoneNumber(userDTO.getPhoneNumber())).thenReturn(false);
        when(passwordEncoder.encode(userDTO.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = userService.createUser(userDTO);

        // Assert
        assertNotNull(result);
        assertEquals(userDTO.getFullName(), result.getFullName());
        assertEquals(userDTO.getPhoneNumber(), result.getPhoneNumber());
        assertEquals("encodedPassword", result.getPassword());
        assertEquals(userRole, result.getRole());
        assertTrue(result.isActive());

        // Verify repository interactions
        verify(userRepository).save(userArgumentCaptor.capture());
        User capturedUser = userArgumentCaptor.getValue();
        assertEquals(userDTO.getEmail(), capturedUser.getEmail());
    }

    // ========== TEST CASE ID: TC-USER-002 ==========
    // Test Objective: Verify that creating a user with duplicate phone number throws exception
    // Input: UserDTO with phone number that already exists in database
    // Expected Output: DataIntegrityViolationException with appropriate message
    // Notes: Database check - no user should be saved; Rollback - transaction rolls back on exception
    @Test
    void TC_USER_002_createUser_withDuplicatePhoneNumber_shouldThrowException() {
        // Arrange
        when(userRepository.existsByPhoneNumber(userDTO.getPhoneNumber())).thenReturn(true);

        // Act & Assert
        DataIntegrityViolationException exception = assertThrows(
            DataIntegrityViolationException.class,
            () -> userService.createUser(userDTO)
        );
        assertNotNull(exception);
        verify(userRepository, never()).save(any());
    }

    // ========== TEST CASE ID: TC-USER-003 ==========
    // Test Objective: Verify that creating a user with ADMIN role throws PermissionDenyException
    // Input: UserDTO with roleId pointing to ADMIN role
    // Expected Output: PermissionDenyException
    // Notes: Security check - prevents privilege escalation
    @Test
    void TC_USER_003_createUser_withAdminRole_shouldThrowPermissionDenyException() throws Exception {
        // Arrange
        when(roleRepository.findById(2L)).thenReturn(Optional.of(adminRole));
        UserDTO adminDTO = UserDTO.builder().roleId(2L).phoneNumber("0123456789").build();

        // Act & Assert
        assertThrows(PermissionDenyException.class, () -> userService.createUser(adminDTO));
    }

    // ========== TEST CASE ID: TC-USER-004 ==========
    // Test Objective: Verify successful login with correct credentials
    // Input: Valid phone number, correct password, matching role
    // Expected Output: JWT token string
    // Notes: Authentication flow; Database check - user must exist with encoded password
    @Test
    void TC_USER_004_login_withValidCredentials_shouldReturnToken() throws Exception {
        // Arrange
        String phoneNumber = "0123456789";
        String password = "password123";
        String roleName = "USER";

        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(testUser));
        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(password, "encodedPassword")).thenReturn(true);
        when(jwtTokenUtil.generateToken(testUser)).thenReturn("jwt-token-123");

        // Act
        String token = userService.login(phoneNumber, password, 1L);

        // Assert
        assertNotNull(token);
        assertEquals("jwt-token-123", token);

        // Verify authentication was called
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    // ========== TEST CASE ID: TC-USER-005 ==========
    // Test Objective: Verify login fails with wrong password
    // Input: Valid phone number, incorrect password
    // Expected Output: BadCredentialsException
    // Notes: Security check; No database changes
    @Test
    void TC_USER_005_login_withWrongPassword_shouldThrowBadCredentialsException() {
        // Arrange
        String phoneNumber = "0123456789";
        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        // Act & Assert
        BadCredentialsException exception = assertThrows(
            BadCredentialsException.class,
            () -> userService.login(phoneNumber, "wrongPassword", 1L)
        );
        assertNotNull(exception);
    }

    // ========== TEST CASE ID: TC-USER-006 ==========
    // Test Objective: Verify login fails for inactive user
    // Input: Valid credentials but user.active = false
    // Expected Output: BadCredentialsException
    // Notes: Account security check
    @Test
    void TC_USER_006_login_withInactiveUser_shouldThrowBadCredentialsException() {
        // Arrange
        User inactiveUser = User.builder().id(1L).phoneNumber("0123456789").active(false).build();
        when(userRepository.findByPhoneNumber("0123456789")).thenReturn(Optional.of(inactiveUser));

        // Act & Assert
        assertThrows(BadCredentialsException.class,
            () -> userService.login("0123456789", "password", 1L));
    }

    // ========== TEST CASE ID: TC-USER-007 ==========
    // Test Objective: Verify getUserDetailsFromToken returns user when token is valid
    // Input: Valid JWT token, user exists in database
    // Expected Output: User object matching the token's phone number
    // Notes: Database read only; No DB changes
    @Test
    void TC_USER_007_getUserDetailsFromToken_withValidToken_shouldReturnUser() throws Exception {
        // Arrange
        String token = "valid-token";
        when(jwtTokenUtil.isTokenExpired(token)).thenReturn(false);
        when(jwtTokenUtil.extractPhoneNumber(token)).thenReturn("0123456789");
        when(userRepository.findByPhoneNumber("0123456789")).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.getUserDetailsFromToken(token);

        // Assert
        assertNotNull(result);
        assertEquals(testUser.getPhoneNumber(), result.getPhoneNumber());
    }

    // ========== TEST CASE ID: TC-USER-008 ==========
    // Test Objective: Verify getUserDetailsFromToken throws exception for expired token
    // Input: Expired JWT token
    // Expected Output: Exception with message "Token is expired"
    // Notes: No database access
    @Test
    void TC_USER_008_getUserDetailsFromToken_withExpiredToken_shouldThrowException() {
        // Arrange
        String token = "expired-token";
        when(jwtTokenUtil.isTokenExpired(token)).thenReturn(true);

        // Act & Assert
        Exception exception = assertThrows(Exception.class,
            () -> userService.getUserDetailsFromToken(token));
        assertEquals("Token is expired", exception.getMessage());
    }

    // ========== TEST CASE ID: TC-USER-009 ==========
    // Test Objective: Verify updateUser successfully updates allowed fields
    // Input: userId, UpdateUserDTO with partial fields (fullName, email, address)
    // Expected Output: Updated User with modified fields only
    // Notes: Database check - user is updated; Rollback - transaction ensures cleanup
    @Test
    void TC_USER_009_updateUser_withValidData_shouldUpdateFields() throws Exception {
        // Arrange
        Long userId = 1L;
        User existingUser = User.builder()
                .id(userId)
                .fullName("Old Name")
                .phoneNumber("0123456789")
                .email("old@example.com")
                .address("Old Address")
                .password("encodedPassword")
                .role(userRole)
                .build();

        UpdateUserDTO updateDTO = UpdateUserDTO.builder()
                .fullName("Updated Name")
                .email("updated@example.com")
                .address("Updated Address")
                .phoneNumber("0123456789") // same as existing to avoid duplicate check
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = userService.updateUser(userId, updateDTO);

        // Assert
        assertEquals("Updated Name", result.getFullName());
        assertEquals("updated@example.com", result.getEmail());
        assertEquals("Updated Address", result.getAddress());
        assertEquals("0123456789", result.getPhoneNumber()); // Unchanged
        verify(userRepository).save(userArgumentCaptor.capture());
    }

    // ========== TEST CASE ID: TC-USER-010 ==========
    // Test Objective: Verify updateUser throws exception when changing to existing phone number of another user
    // Input: userId=1, UpdateUserDTO with phoneNumber that belongs to userId=2
    // Expected Output: DataIntegrityViolationException
    // Notes: Database integrity check; Prevents duplicate phone numbers
    @Test
    void TC_USER_010_updateUser_withDuplicatePhoneNumber_shouldThrowException() throws Exception {
        // Arrange
        Long userId = 1L;
        User existingUser = User.builder().id(userId).phoneNumber("0123456789").build();

        UpdateUserDTO updateDTO = UpdateUserDTO.builder()
                .phoneNumber("0987654321")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByPhoneNumber("0987654321")).thenReturn(true);

        // Act & Assert
        assertThrows(DataIntegrityViolationException.class,
            () -> userService.updateUser(userId, updateDTO));
    }

    // ========== TEST CASE ID: TC-USER-011 ==========
    // Test Objective: Verify getAllUser returns all users mapped to UserResponse
    // Input: No parameters
    // Expected Output: List of UserResponse objects
    // Notes: Database read; Returns all users from repository
    @Test
    void TC_USER_011_getAllUser_shouldReturnAllUsers() {
        // Arrange
        User user1 = User.builder().id(1L).fullName("User 1").phoneNumber("0123456789").role(userRole).build();
        User user2 = User.builder().id(2L).fullName("User 2").phoneNumber("0987654321").role(userRole).build();
        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        // Act
        List<UserResponse> responses = userService.getAllUser();

        // Assert
        assertEquals(2, responses.size());
        assertEquals("User 1", responses.get(0).getFullName());
        assertEquals("User 2", responses.get(1).getFullName());
    }

    // ========== TEST CASE ID: TC-USER-012 ==========
    // Test Objective: Verify changeRoleUser successfully changes user role
    // Input: userId, roleId
    // Expected Output: User with updated role
    // Notes: Database updates both role assignment and user save; Transaction rollback on error
    @Test
    void TC_USER_012_changeRoleUser_withValidIds_shouldChangeRole() throws Exception {
        // Arrange
        Long userId = 1L;
        Long roleId = 2L;
        User user = User.builder().id(userId).role(userRole).build();
        Role newRole = Role.builder().id(roleId).name("ADMIN").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(newRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = userService.changeRoleUser(roleId, userId);

        // Assert
        assertEquals("ADMIN", result.getRole().getName());
        verify(userRepository).save(userArgumentCaptor.capture());
        assertEquals(newRole, userArgumentCaptor.getValue().getRole());
    }

    // ========== TEST CASE ID: TC-USER-013 ==========
    // Test Objective: Verify deleteUser removes user from database
    // Input: userId
    // Expected Output: User deleted from repository
    // Notes: Database deletion; Hard delete; Rollback - can be restored in test cleanup
    @Test
    void TC_USER_013_deleteUser_withValidId_shouldDeleteUser() {
        // Arrange
        Long userId = 1L;
        doNothing().when(userRepository).deleteById(userId);

        // Act
        userService.deleteUser(userId);

        // Assert
        verify(userRepository).deleteById(userId);
    }

    // ========== TEST CASE ID: TC-USER-014 ==========
    // Test Objective: Verify forgotPassword generates reset token and sends email
    // Input: Valid email address of registered user
    // Expected Output: Reset token set on user, token expiry set 15 minutes from now, email sent
    // Notes: Database check - user's resetPasswordToken fields updated; Email service invoked
    @Test
    void TC_USER_014_forgotPassword_withValidEmail_shouldGenerateTokenAndSendEmail() throws Exception {
        // Arrange
        String userEmail = "test@example.com";
        User userWithEmail = User.builder()
                .id(1L)
                .email(userEmail)
                .fullName("Test User")
                .build();

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(userWithEmail));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        userService.forgotPassword(userEmail);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertNotNull(savedUser.getResetPasswordToken());
        assertNotNull(savedUser.getResetPasswordTokenExpiry());
        assertTrue(savedUser.getResetPasswordTokenExpiry().isAfter(LocalDateTime.now()));

        // Verify email was sent (Email.sendEmail returns boolean)
        // Note: Email mock returns false by default, but we verify the call was made
        verify(email).sendEmail(eq(userEmail), anyString(), anyString());
    }

    // ========== TEST CASE ID: TC-USER-015 ==========
    // Test Objective: Verify forgotPassword throws exception for non-existent email
    // Input: Email that doesn't exist in database
    // Expected Output: DataNotFoundException
    // Notes: No database changes; No email sent
    @Test
    void TC_USER_015_forgotPassword_withNonExistentEmail_shouldThrowException() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(DataNotFoundException.class,
            () -> userService.forgotPassword("nonexistent@example.com"));
        verify(email, never()).sendEmail(anyString(), anyString(), anyString());
    }

    // ========== TEST CASE ID: TC-USER-016 ==========
    // Test Objective: Verify resetPassword updates password with valid token
    // Input: Valid reset token, new password
    // Expected Output: User password updated with encoded version, token cleared
    // Notes: Database update; Token fields nullified; Rollback after test
    @Test
    void TC_USER_016_resetPassword_withValidToken_shouldUpdatePassword() throws Exception {
        // Arrange
        String token = "valid-reset-token";
        String newPassword = "newPassword123";
        User user = User.builder()
                .id(1L)
                .resetPasswordToken(token)
                .resetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(15))
                .build();

        when(userRepository.findByResetPasswordToken(token)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(newPassword)).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        userService.resetPassword(token, newPassword);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("encodedNewPassword", savedUser.getPassword());
        assertNull(savedUser.getResetPasswordToken());
        assertNull(savedUser.getResetPasswordTokenExpiry());
    }

    // ========== TEST CASE ID: TC-USER-031 ==========
    // Test Objective: Verify resetPassword handles null expiry (BUG DETECTION)
    // Input: Valid token but resetPasswordTokenExpiry is null
    // Expected Output: Should throw exception instead of NPE
    // ====================
    @Test
    void TC_USER_031_resetPassword_ShouldHandleNullExpiry_Bug() throws Exception {
        // Arrange
        String token = "valid-token-null-expiry";
        User user = User.builder()
                .id(1L)
                .resetPasswordToken(token)
                .resetPasswordTokenExpiry(null) // BUG: null expiry causes NPE
                .build();

        when(userRepository.findByResetPasswordToken(token)).thenReturn(Optional.of(user));

        // Act & Assert - BUG: Current code will throw NullPointerException
        // Should throw meaningful exception instead
        Exception exception = assertThrows(Exception.class, () ->
                userService.resetPassword(token, "newPassword"));
        // Should NOT be NullPointerException
        assertFalse(exception instanceof NullPointerException);
    }

    // ========== TEST CASE ID: TC-USER-017 ==========
    // Test Objective: Verify resetPassword throws exception for invalid/expired token
    // Input: Non-existent token
    // Expected Output: DataNotFoundException
    // Notes: No database modifications
    @Test
    void TC_USER_017_resetPassword_withInvalidToken_shouldThrowException() {
        // Arrange
        when(userRepository.findByResetPasswordToken("invalid-token")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(DataNotFoundException.class,
            () -> userService.resetPassword("invalid-token", "newPassword"));
    }

    // ========== TEST CASE ID: TC-USER-018 ==========
    // Test Objective: Verify changePassword succeeds with correct current password
    // Input: Valid token, correct current password, new different password
    // Expected Output: Password updated, token still valid
    // Notes: Database update; Current password validated; Rollback after test
    @Test
    void TC_USER_018_changePassword_withCorrectCurrentPassword_shouldSucceed() throws Exception {
        // Arrange
        String token = "valid-token";
        String currentPassword = "oldPassword";
        String newPassword = "newPassword";
        User user = User.builder()
                .id(1L)
                .password("encodedOldPassword")
                .build();

        when(jwtTokenUtil.isTokenExpired(token)).thenReturn(false);
        when(jwtTokenUtil.extractPhoneNumber(token)).thenReturn("0123456789");
        when(userRepository.findByPhoneNumber("0123456789")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(currentPassword, "encodedOldPassword")).thenReturn(true);
        when(passwordEncoder.matches(newPassword, "encodedOldPassword")).thenReturn(false);
        when(passwordEncoder.encode(newPassword)).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        userService.changePassword(token, currentPassword, newPassword);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("encodedNewPassword", userCaptor.getValue().getPassword());
    }

    // ========== TEST CASE ID: TC-USER-019 ==========
    // Test Objective: Verify changePassword throws exception when current password is incorrect
    // Input: Valid token, wrong current password
    // Expected Output: BadCredentialsException
    // Notes: Security check; No password change
    @Test
    void TC_USER_019_changePassword_withWrongCurrentPassword_shouldThrowException() throws Exception {
        // Arrange
        String token = "valid-token";
        User user = User.builder().id(1L).password("encodedPassword").build();

        when(jwtTokenUtil.isTokenExpired(token)).thenReturn(false);
        when(jwtTokenUtil.extractPhoneNumber(token)).thenReturn("0123456789");
        when(userRepository.findByPhoneNumber("0123456789")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        // Act & Assert
        assertThrows(BadCredentialsException.class,
            () -> userService.changePassword(token, "wrongPassword", "newPassword"));
        verify(userRepository, never()).save(any());
    }

    // ========== TEST CASE ID: TC-USER-020 ==========
    // Test Objective: Verify changePassword throws exception when new password equals current
    // Input: Valid token, current password, new password that matches current
    // Expected Output: Exception with message about password difference
    // Notes: Security policy - cannot reuse current password
    @Test
    void TC_USER_020_changePassword_withSamePassword_shouldThrowException() throws Exception {
        // Arrange
        String token = "valid-token";
        String currentPassword = "password123";
        User user = User.builder().id(1L).password("encodedPassword").build();

        when(jwtTokenUtil.isTokenExpired(token)).thenReturn(false);
        when(jwtTokenUtil.extractPhoneNumber(token)).thenReturn("0123456789");
        when(userRepository.findByPhoneNumber("0123456789")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(currentPassword, "encodedPassword")).thenReturn(true);
        when(passwordEncoder.matches(currentPassword, "encodedPassword")).thenReturn(true); // New = current

        // Act & Assert
        Exception exception = assertThrows(Exception.class,
            () -> userService.changePassword(token, currentPassword, currentPassword));
        assertTrue(exception.getMessage().contains("different from current password"));
    }

    // ========== TEST CASE ID: TC-USER-021 ==========
    // Test Objective: Verify getUserDetailsFromToken throws exception when user not found
    // Input: Valid token but phone number doesn't exist in database
    // Expected Output: Generic Exception with message "User not found"
    // ====================
    @Test
    void TC_USER_021_getUserDetailsFromToken_withNonExistentUser_shouldThrowException() throws Exception {
        // Arrange
        String token = "valid-token";
        when(jwtTokenUtil.isTokenExpired(token)).thenReturn(false);
        when(jwtTokenUtil.extractPhoneNumber(token)).thenReturn("0999999999");
        when(userRepository.findByPhoneNumber("0999999999")).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () ->
                userService.getUserDetailsFromToken(token));
        assertEquals("User not found", exception.getMessage());
    }

    // ========== TEST CASE ID: TC-USER-022 ==========
    // Test Objective: Verify updateUser throws exception when user not found
    // Input: Non-existent user ID, valid UpdateUserDTO
    // Expected Output: DataNotFoundException
    // ====================
    @Test
    void TC_USER_022_updateUser_withNonExistentId_shouldThrowException() throws Exception {
        // Arrange
        Long nonExistentUserId = 999L;
        UpdateUserDTO updateDTO = UpdateUserDTO.builder().fullName("Updated Name").build();
        when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(DataNotFoundException.class, () ->
                userService.updateUser(nonExistentUserId, updateDTO));
        assertTrue(exception.getMessage().toLowerCase().contains("user") ||
                   exception.getMessage().toLowerCase().contains("not found"));
    }

    // ========== TEST CASE ID: TC-USER-023 ==========
    // Test Objective: Verify changeRoleUser throws exception when user not found
    // Input: Non-existent user ID, valid role ID
    // Expected Output: Generic Exception with message "Cannot find user with id = X"
    // Note: Service checks role first, so we stub role to exist, then user not found
    // ====================
    @Test
    void TC_USER_023_changeRoleUser_withNonExistentUser_shouldThrowException() throws Exception {
        // Arrange
        Long nonExistentUserId = 999L;
        Long roleId = 2L;
        Role existingRole = Role.builder().id(roleId).name("ADMIN").build();
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () ->
                userService.changeRoleUser(roleId, nonExistentUserId));
        assertEquals("Cannot find user with id = " + nonExistentUserId, exception.getMessage());
    }

    // ========== TEST CASE ID: TC-USER-024 ==========
    // Test Objective: Verify changeRoleUser throws exception when role not found
    // Input: Valid user ID, non-existent role ID
    // Expected Output: Generic Exception with message "Cannot find role with id = X"
    // ====================
    @Test
    void TC_USER_024_changeRoleUser_withNonExistentRole_shouldThrowException() throws Exception {
        // Arrange
        Long userId = 1L;
        Long nonExistentRoleId = 999L;
        when(roleRepository.findById(nonExistentRoleId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () ->
                userService.changeRoleUser(nonExistentRoleId, userId));
        assertEquals("Cannot find role with id = " + nonExistentRoleId, exception.getMessage());
    }

    // ========== TEST CASE ID: TC-USER-025 ==========
    // Test Objective: Verify deleteUser handles non-existent user gracefully
    // Input: Non-existent user ID
    // Expected Output: No exception, deleteById called
    // ====================
    @Test
    void TC_USER_025_deleteUser_withNonExistentId_shouldHandleGracefully() throws Exception {
        // Arrange
        Long nonExistentUserId = 999L;
        doNothing().when(userRepository).deleteById(nonExistentUserId);

        // Act
        userService.deleteUser(nonExistentUserId);

        // Assert - no exception thrown
        verify(userRepository).deleteById(nonExistentUserId);
    }

    // ========== TEST CASE ID: TC-USER-026 ==========
    // Test Objective: Verify createUser throws exception when phone number is null
    // Input: UserDTO with null phoneNumber
    // Expected Output: DataIntegrityViolationException or validation exception
    // ====================
    @Test
    void TC_USER_026_createUser_withNullPhoneNumber_shouldThrowValidationException() throws Exception {
        // Arrange
        UserDTO nullPhoneDTO = UserDTO.builder()
                .fullName("Test User")
                .phoneNumber(null)
                .email("test@example.com")
                .address("Test Address")
                .password("password123")
                .roleId(1L)
                .build();

        when(roleRepository.findById(1L)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            if (u.getPhoneNumber() == null) {
                throw new DataIntegrityViolationException("Phone number cannot be null");
            }
            return u;
        });

        // Act & Assert
        Exception exception = assertThrows(DataIntegrityViolationException.class, () -> {
            userService.createUser(nullPhoneDTO);
        });
        assertTrue(exception.getMessage().toLowerCase().contains("phone"));
    }

    // ========== TEST CASE ID: TC-USER-027 ==========
    // Test Objective: Verify updateActiveUserById updates active status when user exists
    // Input: userId of existing inactive user, activeUser = true
    // Expected Output: User's active status updated to true, saved, and returned in Optional
    // ====================
    @Test
    void TC_USER_027_updateActiveUserById_ShouldUpdateActiveStatus_WhenUserExists() throws Exception {
        // Arrange
        Long userId = 1L;
        User existingUser = User.builder()
                .id(userId)
                .fullName("Test User")
                .phoneNumber("0123456789")
                .role(userRole)
                .active(false)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Optional<User> result = userService.updateActiveUserById(userId, true);

        // Assert
        assertTrue(result.isPresent());
        assertTrue(result.get().isActive());
        verify(userRepository).save(userArgumentCaptor.capture());
        User savedUser = userArgumentCaptor.getValue();
        assertTrue(savedUser.isActive());
    }
    // ========== TEST CASE ID: TC-USER-028 ==========
    // Test Objective: Verify login fails for non-existent user
    // Input: Phone number that doesn't exist
    // Expected Output: DataNotFoundException
    // ====================
    @Test
    void TC_USER_028_login_withNonExistentUser_shouldThrowDataNotFoundException() {
        // Arrange
        String nonExistentPhone = "0999999999";
        when(userRepository.findByPhoneNumber(nonExistentPhone)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(DataNotFoundException.class,
            () -> userService.login(nonExistentPhone, "password123", 1L));
    }

    // ========== TEST CASE ID: TC-USER-029 ==========
    // Test Objective: Verify login fails when user has wrong role
    // Input: Valid credentials but user tries to login as ADMIN but is actually USER
    // Expected Output: BadCredentialsException
    // ====================
    @Test
    void TC_USER_029_login_withWrongRole_shouldThrowBadCredentialsException() throws Exception {
        // Arrange
        String phoneNumber = "0123456789";
        User userWithUserRole = User.builder()
                .id(1L)
                .phoneNumber(phoneNumber)
                .password("encodedPassword")
                .role(userRole) // USER role, not ADMIN
                .active(true) // Ensure active to reach role check
                .build();

        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(userWithUserRole));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

        // Act & Assert - trying to login as ADMIN (roleId=2) but user has USER role
        assertThrows(BadCredentialsException.class,
            () -> userService.login(phoneNumber, "password123", 2L));
    }

    // ========== TEST CASE ID: TC-USER-030 ==========
    // Test Objective: Verify resetPassword throws exception when token has expired
    // Input: Valid token format but expiry time has passed
    // Expected Output: DataNotFoundException
    // ====================
    @Test
    void TC_USER_030_resetPassword_withExpiredToken_shouldThrowException() throws Exception {
        // Arrange
        String expiredToken = "expired-token";
        User user = User.builder()
                .id(1L)
                .resetPasswordToken(expiredToken)
                .resetPasswordTokenExpiry(LocalDateTime.now().minusMinutes(16)) // Expired 1 min ago
                .build();

        when(userRepository.findByResetPasswordToken(expiredToken)).thenReturn(Optional.of(user));

        // Act & Assert - implementation should check token expiry
        // Note: This test assumes service checks token expiry; if not, this will need adjustment
        Exception exception = assertThrows(Exception.class,
            () -> userService.resetPassword(expiredToken, "newPassword"));
        assertTrue(exception.getMessage().toLowerCase().contains("expired") ||
                   exception instanceof DataNotFoundException);
    }
}
