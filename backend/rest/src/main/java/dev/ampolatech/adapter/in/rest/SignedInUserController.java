package dev.ampolatech.adapter.in.rest;

import dev.ampolatech.domain.CurrentActor;
import dev.ampolatech.port.in.GetSignedInUserQuery;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// TODO(#12): implement the interface generated from the OpenAPI spec (ADR 0006).
@RestController
class SignedInUserController {

  private final CurrentActor currentActor;
  private final GetSignedInUserQuery getSignedInUser;

  SignedInUserController(CurrentActor currentActor, GetSignedInUserQuery getSignedInUser) {
    this.currentActor = currentActor;
    this.getSignedInUser = getSignedInUser;
  }

  @GetMapping("/api/me")
  SignedInUserResponse signedInUser() {
    return SignedInUserResponse.from(getSignedInUser.getSignedInUser(currentActor.get()));
  }
}
