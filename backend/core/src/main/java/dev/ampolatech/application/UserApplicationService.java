package dev.ampolatech.application;

import dev.ampolatech.domain.Actor;
import dev.ampolatech.domain.Pharmacy;
import dev.ampolatech.domain.User;
import dev.ampolatech.port.in.GetSignedInUserQuery;
import dev.ampolatech.port.out.PharmacyRepositoryPort;
import dev.ampolatech.port.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserApplicationService implements GetSignedInUserQuery {

  private final UserRepositoryPort users;
  private final PharmacyRepositoryPort pharmacies;

  @Override
  public GetSignedInUserQuery.Result getSignedInUser(Actor actor) {
    return new GetSignedInUserQuery.Result(userOf(actor), pharmacyOf(actor));
  }

  private User userOf(Actor actor) {
    return users
        .findById(actor.userId(), actor.pharmacyId())
        .orElseThrow(() ->
            new IllegalStateException(
                "The signed-in user "
                    + actor.userId().value()
                    + " does not exist in pharmacy "
                    + actor.pharmacyId().value()));
  }

  private Pharmacy pharmacyOf(Actor actor) {
    return pharmacies
        .findById(actor.pharmacyId())
        .orElseThrow(() ->
            new IllegalStateException(
                "The signed-in user's pharmacy " + actor.pharmacyId().value() + " does not exist"));
  }
}
