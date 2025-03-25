package viettel.dac.identityservice.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import viettel.dac.identityservice.dto.response.UserResponse;
import viettel.dac.identityservice.entity.User;

/**
 * Mapper for converting between User entity and DTOs
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    /**
     * Convert User entity to UserResponse DTO
     *
     * @param user User entity
     * @return UserResponse DTO
     */
    @Mapping(target = "status", source = "status")
    UserResponse toUserResponse(User user);
}