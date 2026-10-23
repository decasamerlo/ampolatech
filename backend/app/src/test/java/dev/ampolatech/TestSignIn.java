package dev.ampolatech;

import dev.ampolatech.domain.Actor;
import dev.ampolatech.domain.CurrentActor;
import dev.ampolatech.domain.User;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** The current actor in tests: each test signs in the user it acts as. */
public class TestSignIn implements CurrentActor {

  private final AtomicReference<Actor> actor = new AtomicReference<>();

  public void signIn(User user) {
    Objects.requireNonNull(user, "user must not be null");
    signIn(new Actor(user.id(), user.pharmacyId(), user.role()));
  }

  /** Signs in the actor as given, even one whose pharmacy isn't their user's. */
  public void signIn(Actor actor) {
    this.actor.set(Objects.requireNonNull(actor, "actor must not be null"));
  }

  public void signOut() {
    actor.set(null);
  }

  @Override
  public Actor get() {
    Actor signedIn = actor.get();
    if (signedIn == null) {
      throw new IllegalStateException("No user is signed in: the test must call signIn first");
    }
    return signedIn;
  }
}
