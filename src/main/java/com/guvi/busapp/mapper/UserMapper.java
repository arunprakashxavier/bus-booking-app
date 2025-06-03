package com.guvi.busapp.mapper;

import com.guvi.busapp.dto.RegisterDto;
import com.guvi.busapp.dto.UserProfileDto;
import com.guvi.busapp.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE, // Ignores unmapped target properties
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE // Ignores null source properties during updates
)
public interface UserMapper {

    /**
     * Maps RegisterDto to User entity.
     * Note: Password should be encoded by the service after this mapping.
     * Role is typically set by the service.
     *
     * @param registerDto the registration data transfer object
     * @return the User entity
     */
    @Mapping(target = "id", ignore = true) // ID is generated
    @Mapping(target = "password", source = "password") // Map password, service will encode
    @Mapping(target = "bookings", ignore = true) // Bookings are managed separately
    @Mapping(target = "role", ignore = true) // Role will be set by UserService
    User registerDtoToUser(RegisterDto registerDto);

    /**
     * Maps User entity to UserProfileDto.
     *
     * @param user the User entity
     * @return the UserProfileDto
     */
    UserProfileDto userToUserProfileDto(User user);

    /**
     * Updates an existing User entity from UserProfileDto.
     * Ignores 'email' from DTO as it should not be updated here.
     * Password is not handled by this DTO/mapper.
     *
     * @param userProfileDto the user profile data transfer object
     * @param user           the user entity to be updated (annotated with @MappingTarget)
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true) // Email should not be updated from UserProfileDto
    @Mapping(target = "password", ignore = true) // Password is not updated here
    @Mapping(target = "role", ignore = true) // Role is not updated here
    @Mapping(target = "bookings", ignore = true)
    void updateUserFromUserProfileDto(UserProfileDto userProfileDto, @MappingTarget User user);
}
