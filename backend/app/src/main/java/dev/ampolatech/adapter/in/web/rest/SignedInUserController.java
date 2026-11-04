package dev.ampolatech.adapter.in.web.rest;

import dev.ampolatech.adapter.in.web.rest.api.UsersApi;
import dev.ampolatech.adapter.in.web.rest.model.SignedInUserDto;
import dev.ampolatech.domain.CurrentActor;
import dev.ampolatech.port.in.GetSignedInUserQuery;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SignedInUserController implements UsersApi {

  private final CurrentActor currentActor;
  private final GetSignedInUserQuery getSignedInUser;
  private final UserWebMapper mapper;

  SignedInUserController(
      CurrentActor currentActor, GetSignedInUserQuery getSignedInUser, UserWebMapper mapper) {
    this.currentActor = currentActor;
    this.getSignedInUser = getSignedInUser;
    this.mapper = mapper;
  }

  @Override
  public ResponseEntity<SignedInUserDto> getSignedInUser() {
    GetSignedInUserQuery.Result signedIn = getSignedInUser.getSignedInUser(currentActor.get());
    return ResponseEntity.ok(mapper.toDto(signedIn));
  }
}
