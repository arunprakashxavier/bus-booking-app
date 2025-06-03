//package com.guvi.busapp.service;
//
//import com.guvi.busapp.model.User;
//import com.guvi.busapp.repository.UserRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class UserDetailsServiceImplTest {
//
//    @Mock
//    private UserRepository userRepository;
//
//    @InjectMocks // Inject mock userRepository into the actual UserDetailsServiceImpl instance
//    private UserDetailsServiceImpl userDetailsService;
//
//    private User testUser;
//    private String userEmail = "test@example.com";
//    private String encodedPassword = "encodedPassword123";
//
//    @BeforeEach
//    void setUp() {
//        testUser = new User();
//        testUser.setId(1L);
//        testUser.setEmail(userEmail);
//        testUser.setPassword(encodedPassword);
//        testUser.setRole(User.Role.ROLE_USER); // Assuming standard user role
//        testUser.setFirstName("Test"); // Add other fields if needed by UserDetails impl, though usually not
//    }
//
//    @Test
//    void loadUserByUsername_Success() {
//        // Arrange: Mock repository to find the user
//        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
//
//        // Act: Call the method under test
//        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
//
//        // Assert: Check the returned UserDetails object
//        assertNotNull(userDetails);
//        assertEquals(userEmail, userDetails.getUsername());
//        assertEquals(encodedPassword, userDetails.getPassword());
//        assertNotNull(userDetails.getAuthorities());
//        assertEquals(1, userDetails.getAuthorities().size()); // Expecting one role
//        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));
//        assertTrue(userDetails.isEnabled()); // Default UserDetails checks
//        assertTrue(userDetails.isAccountNonExpired());
//        assertTrue(userDetails.isAccountNonLocked());
//        assertTrue(userDetails.isCredentialsNonExpired());
//
//        // Verify repository was called
//        verify(userRepository, times(1)).findByEmail(userEmail);
//    }
//
//    @Test
//    void loadUserByUsername_UserNotFound() {
//        // Arrange: Mock repository to NOT find the user
//        String unknownEmail = "unknown@example.com";
//        when(userRepository.findByEmail(unknownEmail)).thenReturn(Optional.empty());
//
//        // Act & Assert: Expect UsernameNotFoundException
//        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
//            userDetailsService.loadUserByUsername(unknownEmail);
//        });
//
//        // Check the exception message (adjust if your service implementation formats it differently)
//        assertEquals("User Not Found with email: " + unknownEmail, exception.getMessage());
//
//        // Verify repository was called
//        verify(userRepository, times(1)).findByEmail(unknownEmail);
//    }
//
//    @Test
//    void loadUserByUsername_HandlesAdminRole() {
//        // Arrange: Setup an admin user
//        testUser.setRole(User.Role.ROLE_ADMIN);
//        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
//
//        // Act: Call the method under test
//        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
//
//        // Assert: Check the authority is ROLE_ADMIN
//        assertNotNull(userDetails);
//        assertEquals(userEmail, userDetails.getUsername());
//        assertEquals(1, userDetails.getAuthorities().size());
//        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
//
//        // Verify repository was called
//        verify(userRepository, times(1)).findByEmail(userEmail);
//    }
//}