package dev.ampolatech.adapter.in.web.rest;

import dev.ampolatech.adapter.in.web.rest.api.UsersApi;
import dev.ampolatech.adapter.in.web.rest.model.SignedInUserDto;
import dev.ampolatech.domain.CurrentActor;
import dev.ampolatech.port.in.GetSignedInUserQuery;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class SignedInUserController implements UsersApi {

  private final CurrentActor currentActor;
  private final GetSignedInUserQuery getSignedInUser;
  private final UserWebMapper mapper;

  @Override
  public ResponseEntity<SignedInUserDto> getSignedInUser() {
    GetSignedInUserQuery.Result signedIn = getSignedInUser.getSignedInUser(currentActor.get());
    return ResponseEntity.ok(mapper.toDto(signedIn));
  }
}
