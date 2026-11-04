package dev.ampolatech.adapter.in.web.rest;

import dev.ampolatech.adapter.in.web.rest.api.UsersApi;
import dev.ampolatech.adapter.in.web.rest.model.PharmacyDto;
import dev.ampolatech.adapter.in.web.rest.model.RoleDto;
import dev.ampolatech.adapter.in.web.rest.model.SignedInUserDto;
import dev.ampolatech.domain.CurrentActor;
import dev.ampolatech.domain.Role;
import dev.ampolatech.port.in.GetSignedInUserQuery;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SignedInUserController implements UsersApi {

  private final CurrentActor currentActor;
  private final GetSignedInUserQuery getSignedInUser;

  SignedInUserController(CurrentActor currentActor, GetSignedInUserQuery getSignedInUser) {
    this.currentActor = currentActor;
    this.getSignedInUser = getSignedInUser;
  }

  @Override
  public ResponseEntity<SignedInUserDto> getSignedInUser() {
    GetSignedInUserQuery.Result signedIn = getSignedInUser.getSignedInUser(currentActor.get());

    var user = signedIn.user();
    var pharmacy = signedIn.pharmacy();
    return ResponseEntity.ok(
        new SignedInUserDto(user.name(), roleDto(user.role()), new PharmacyDto(pharmacy.name())));
  }

  private static RoleDto roleDto(Role role) {
    return switch (role) {
      case PHARMACIST -> RoleDto.PHARMACIST;
      case SALES_AGENT -> RoleDto.SALES_AGENT;
    };
  }
}
