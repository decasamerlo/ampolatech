package dev.ampolatech;

import dev.ampolatech.domain.Pharmacy;
import dev.ampolatech.domain.PharmacyId;
import dev.ampolatech.domain.Role;
import dev.ampolatech.domain.User;
import dev.ampolatech.domain.UserId;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Inserts rows directly, as the owner, since the app may only read them. Each test creates the
 * pharmacies it writes to, so the tests share one database without cleaning it: another test's rows
 * are only more pharmacies the signed-in user must not see. A test that touches data has at least
 * two pharmacies.
 */
public class TestData {

  private final JdbcClient owner = TestDatabase.owner();

  public Pharmacy pharmacy(String name) {
    var pharmacy = new Pharmacy(PharmacyId.of(UUID.randomUUID()), name);
    owner
        .sql("INSERT INTO pharmacy (id, name) VALUES (?, ?)")
        .params(pharmacy.id().value(), pharmacy.name())
        .update();
    return pharmacy;
  }

  public User user(Pharmacy pharmacy, String name, Role role) {
    var user = new User(UserId.of(UUID.randomUUID()), pharmacy.id(), name, role);
    owner
        .sql("INSERT INTO app_user (id, pharmacy_id, name, role) VALUES (?, ?, ?, ?)")
        .params(user.id().value(), user.pharmacyId().value(), user.name(), user.role().name())
        .update();
    return user;
  }
}
