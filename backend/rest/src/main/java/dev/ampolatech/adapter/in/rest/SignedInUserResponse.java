package dev.ampolatech.adapter.in.rest;

import dev.ampolatech.domain.Role;
import dev.ampolatech.port.in.GetSignedInUserQuery;

record SignedInUserResponse(String name, Role role, PharmacyResponse pharmacy) {

  static SignedInUserResponse from(GetSignedInUserQuery.Result signedIn) {
    var user = signedIn.user();
    var pharmacy = signedIn.pharmacy();
    return new SignedInUserResponse(
        user.name(), user.role(), new PharmacyResponse(pharmacy.name()));
  }

  record PharmacyResponse(String name) {}
}
