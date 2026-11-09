package dev.ampolatech.adapter.in.web.rest;

import dev.ampolatech.adapter.in.web.rest.model.RoleDto;
import dev.ampolatech.adapter.in.web.rest.model.SignedInUserDto;
import dev.ampolatech.domain.Role;
import dev.ampolatech.domain.User;
import dev.ampolatech.port.in.GetSignedInUserQuery;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
class UserWebMapper {

  private final PharmacyWebMapper pharmacyMapper;

  UserWebMapper(PharmacyWebMapper pharmacyMapper) {
    this.pharmacyMapper = pharmacyMapper;
  }

  SignedInUserDto toDto(GetSignedInUserQuery.Result signedIn) {
    User user = signedIn.user();
    return new SignedInUserDto(
        user.name(), roleDtos(user.roles()), pharmacyMapper.toDto(signedIn.pharmacy()));
  }

  // The spec promises the roles in its Role schema's order, and the domain's set promises none. An
  // EnumSet iterates in RoleDto's declaration order, which the generator takes from that schema, so
  // the API's order is made here.
  private static Set<RoleDto> roleDtos(Set<Role> roles) {
    return roles.stream()
        .map(UserWebMapper::roleDto)
        .collect(Collectors.toCollection(() -> EnumSet.noneOf(RoleDto.class)));
  }

  // Each constant written out rather than read with RoleDto.valueOf, so a constant the domain adds
  // fails the compile here until someone decides what the API says about it, and renaming one
  // leaves the API's value as it is.
  private static RoleDto roleDto(Role role) {
    return switch (role) {
      case PHARMACIST -> RoleDto.PHARMACIST;
      case SALES_AGENT -> RoleDto.SALES_AGENT;
    };
  }
}
