//package com.guvi.busapp.service;
//
//import com.guvi.busapp.dto.ChangePasswordDto;
//import com.guvi.busapp.dto.RegisterDto;
//import com.guvi.busapp.dto.UserProfileDto;
//import com.guvi.busapp.exception.ResourceNotFoundException;
//// Removed unused SeatUnavailableException import
//import com.guvi.busapp.model.User;
//import com.guvi.busapp.repository.UserRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//import java.time.LocalDate;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class UserServiceImplTest {
//
//    @Mock
//    private UserRepository userRepository;
//
//    @Mock
//    private PasswordEncoder passwordEncoder;
//
//    @InjectMocks
//    private UserServiceImpl userService;
//
//    // Test Data
//    private RegisterDto registerDto;
//    private User testUser;
//    private UserProfileDto userProfileDto;
//    private ChangePasswordDto changePasswordDto;
//    private String userEmail = "test@example.com";
//    private String currentPassword = "password123"; // Plain text current password
//    private String currentEncodedPassword = "encodedPassword123"; // Encoded current password
//    private String newPassword = "newPassword456";
//    private String newEncodedPassword = "encodedNewPassword";
//
//    @BeforeEach
//    void setUp() {
//        // Register DTO
//        registerDto = new RegisterDto();
//        registerDto.setFirstName("Test");
//        registerDto.setLastName("User");
//        registerDto.setEmail(userEmail);
//        registerDto.setPassword(currentPassword);
//        registerDto.setConfirmPassword(currentPassword);
//        registerDto.setAge(30);
//        registerDto.setGender("Male");
//        registerDto.setDateOfBirth(LocalDate.of(1994, 5, 15));
//        registerDto.setPhoneNumber("1234567890");
//
//        // User Entity
//        testUser = new User();
//        testUser.setId(1L);
//        testUser.setFirstName("Test");
//        testUser.setLastName("User");
//        testUser.setEmail(userEmail);
//        testUser.setPassword(currentEncodedPassword);
//        testUser.setAge(30);
//        testUser.setGender("Male");
//        testUser.setDateOfBirth(LocalDate.of(1994, 5, 15));
//        testUser.setPhoneNumber("1234567890");
//        testUser.setRole(User.Role.ROLE_USER);
//
//        // UserProfile DTO
//        userProfileDto = new UserProfileDto();
//        userProfileDto.setFirstName("Test");
//        userProfileDto.setLastName("User");
//        userProfileDto.setEmail(userEmail);
//        userProfileDto.setAge(30);
//        userProfileDto.setGender("Male");
//        userProfileDto.setDateOfBirth(LocalDate.of(1994, 5, 15));
//        userProfileDto.setPhoneNumber("1234567890");
//
//        // ChangePassword DTO
//        changePasswordDto = new ChangePasswordDto();
//        changePasswordDto.setCurrentPassword(currentPassword);
//        changePasswordDto.setNewPassword(newPassword);
//        changePasswordDto.setConfirmNewPassword(newPassword);
//    }
//
//    // --- Tests for registerUser ---
//
//    @Test
//    void testRegisterUser_Success() {
//        when(userRepository.existsByEmail(registerDto.getEmail())).thenReturn(false);
//        when(passwordEncoder.encode(registerDto.getPassword())).thenReturn(currentEncodedPassword);
//        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
//        when(userRepository.save(userCaptor.capture())).thenAnswer(invocation -> {
//            User userToSave = invocation.getArgument(0);
//            userToSave.setId(99L);
//            return userToSave;
//        });
//
//        User registeredUser = userService.registerUser(registerDto);
//
//        assertNotNull(registeredUser);
//        assertEquals(registerDto.getEmail(), registeredUser.getEmail());
//        assertEquals(currentEncodedPassword, registeredUser.getPassword());
//        assertNotNull(registeredUser.getId());
//        verify(userRepository, times(1)).save(any(User.class));
//        assertEquals(currentEncodedPassword, userCaptor.getValue().getPassword());
//    }
//
//    @Test
//    void testRegisterUser_EmailExists() {
//        when(userRepository.existsByEmail(registerDto.getEmail())).thenReturn(true);
//        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> userService.registerUser(registerDto));
//        assertEquals("Error: Email is already in use!", exception.getMessage());
//        verify(userRepository, never()).save(any(User.class));
//    }
//
//    @Test
//    void testRegisterUser_PasswordMismatch() {
//        registerDto.setConfirmPassword("differentPassword");
//        when(userRepository.existsByEmail(registerDto.getEmail())).thenReturn(false);
//        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> userService.registerUser(registerDto));
//        assertEquals("Error: Passwords do not match!", exception.getMessage());
//        verify(userRepository, never()).save(any(User.class));
//    }
//
//    // --- Tests for getUserProfile ---
//
//    @Test
//    void testGetUserProfile_Success() {
//        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
//        UserProfileDto result = userService.getUserProfile(userEmail);
//        assertNotNull(result);
//        assertEquals(testUser.getFirstName(), result.getFirstName());
//        assertEquals(userEmail, result.getEmail());
//        verify(userRepository, times(1)).findByEmail(userEmail);
//    }
//
//    @Test
//    void testGetUserProfile_UserNotFound() {
//        String nonExistentEmail = "nosuchuser@example.com";
//        when(userRepository.findByEmail(nonExistentEmail)).thenReturn(Optional.empty());
//        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> userService.getUserProfile(nonExistentEmail));
//        assertEquals("User not found with email : '" + nonExistentEmail + "'", exception.getMessage());
//        verify(userRepository, times(1)).findByEmail(nonExistentEmail);
//    }
//
//    // --- Tests for updateUserProfile ---
//
//    @Test
//    void testUpdateUserProfile_Success() {
//        UserProfileDto updatedDto = new UserProfileDto();
//        updatedDto.setFirstName("UpdatedFirst"); updatedDto.setLastName("UpdatedLast"); updatedDto.setAge(35);
//        updatedDto.setGender("Female"); updatedDto.setDateOfBirth(LocalDate.of(1989, 1, 1)); updatedDto.setPhoneNumber("0987654321"); updatedDto.setEmail(userEmail);
//
//        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
//        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
//        when(userRepository.save(userCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));
//
//        UserProfileDto result = userService.updateUserProfile(userEmail, updatedDto);
//
//        assertNotNull(result);
//        assertEquals("UpdatedFirst", result.getFirstName());
//        assertEquals("0987654321", result.getPhoneNumber());
//        assertEquals(userEmail, result.getEmail());
//        verify(userRepository, times(1)).save(any(User.class));
//        User capturedUser = userCaptor.getValue();
//        assertEquals("UpdatedFirst", capturedUser.getFirstName());
//        assertEquals("0987654321", capturedUser.getPhoneNumber());
//        assertEquals(currentEncodedPassword, capturedUser.getPassword());
//    }
//
//    @Test
//    void testUpdateUserProfile_UserNotFound() {
//        String nonExistentEmail = "nosuchuser@example.com";
//        when(userRepository.findByEmail(nonExistentEmail)).thenReturn(Optional.empty());
//        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> userService.updateUserProfile(nonExistentEmail, userProfileDto));
//        assertEquals("User not found with email : '" + nonExistentEmail + "'", exception.getMessage());
//        verify(userRepository, never()).save(any(User.class));
//    }
//
//    // --- Tests for changePassword ---
//
//    @Test
//    void testChangePassword_Success() {
//        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
//        when(passwordEncoder.matches(changePasswordDto.getCurrentPassword(), currentEncodedPassword)).thenReturn(true);
//        when(passwordEncoder.matches(changePasswordDto.getNewPassword(), currentEncodedPassword)).thenReturn(false);
//        when(passwordEncoder.encode(changePasswordDto.getNewPassword())).thenReturn(newEncodedPassword);
//        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
//        when(userRepository.save(userCaptor.capture())).thenReturn(testUser);
//
//        assertDoesNotThrow(() -> userService.changePassword(userEmail, changePasswordDto));
//
//        verify(userRepository, times(1)).findByEmail(userEmail);
//        verify(passwordEncoder, times(1)).matches(changePasswordDto.getCurrentPassword(), currentEncodedPassword);
//        verify(passwordEncoder, times(1)).matches(changePasswordDto.getNewPassword(), currentEncodedPassword);
//        verify(passwordEncoder, times(1)).encode(changePasswordDto.getNewPassword());
//        verify(userRepository, times(1)).save(any(User.class));
//        User savedUser = userCaptor.getValue();
//        assertEquals(newEncodedPassword, savedUser.getPassword());
//    }
//
//    @Test
//    void testChangePassword_UserNotFound() {
//        String nonExistentEmail = "nosuchuser@example.com";
//        when(userRepository.findByEmail(nonExistentEmail)).thenReturn(Optional.empty());
//        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> userService.changePassword(nonExistentEmail, changePasswordDto));
//        assertEquals("User not found with email : '" + nonExistentEmail + "'", exception.getMessage());
//        verifyNoInteractions(passwordEncoder);
//        verify(userRepository, never()).save(any(User.class));
//    }
//
//    @Test
//    void testChangePassword_IncorrectCurrentPassword() {
//        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
//        when(passwordEncoder.matches(changePasswordDto.getCurrentPassword(), currentEncodedPassword)).thenReturn(false);
//
//        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> userService.changePassword(userEmail, changePasswordDto));
//        assertEquals("Incorrect current password.", exception.getMessage());
//
//        verify(userRepository, times(1)).findByEmail(userEmail);
//        verify(passwordEncoder, times(1)).matches(changePasswordDto.getCurrentPassword(), currentEncodedPassword);
//        verify(passwordEncoder, never()).encode(anyString());
//        verify(userRepository, never()).save(any(User.class));
//    }
//
//    @Test
//    void testChangePassword_NewPasswordsMismatch() {
//        changePasswordDto.setConfirmNewPassword("differentNewPassword");
//        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
//        when(passwordEncoder.matches(changePasswordDto.getCurrentPassword(), currentEncodedPassword)).thenReturn(true);
//
//        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> userService.changePassword(userEmail, changePasswordDto));
//        assertEquals("New passwords do not match.", exception.getMessage());
//
//        verify(userRepository, times(1)).findByEmail(userEmail);
//        verify(passwordEncoder, times(1)).matches(changePasswordDto.getCurrentPassword(), currentEncodedPassword);
//        verify(passwordEncoder, never()).encode(anyString());
//        verify(userRepository, never()).save(any(User.class));
//    }
//
//    @Test
//    void testChangePassword_NewPasswordSameAsOld() {
//        // Arrange
//        changePasswordDto.setNewPassword(currentPassword);
//        changePasswordDto.setConfirmNewPassword(currentPassword);
//
//        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
//        // Mock BOTH matches calls to return true for this scenario
//        when(passwordEncoder.matches(eq(currentPassword), eq(currentEncodedPassword))).thenReturn(true);
//
//        // Act & Assert
//        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
//            userService.changePassword(userEmail, changePasswordDto);
//        });
//        assertEquals("New password cannot be the same as the old password.", exception.getMessage());
//
//        // Verify
//        verify(userRepository, times(1)).findByEmail(userEmail);
//        // Verify that matches was called exactly TWICE with these arguments
//        verify(passwordEncoder, times(2)).matches(eq(currentPassword), eq(currentEncodedPassword));
//        // Verify encode and save were not called
//        verify(passwordEncoder, never()).encode(anyString());
//        verify(userRepository, never()).save(any(User.class));
//    }
//}