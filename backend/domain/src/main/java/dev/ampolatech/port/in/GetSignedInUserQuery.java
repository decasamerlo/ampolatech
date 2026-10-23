package dev.ampolatech.port.in;

import dev.ampolatech.domain.Actor;
import dev.ampolatech.domain.Pharmacy;
import dev.ampolatech.domain.User;

public interface GetSignedInUserQuery {

  Result getSignedInUser(Actor actor);

  record Result(User user, Pharmacy pharmacy) {}
}
