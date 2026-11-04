package dev.ampolatech.adapter.in.web.rest;

import dev.ampolatech.adapter.in.web.rest.model.PharmacyDto;
import dev.ampolatech.domain.Pharmacy;
import org.springframework.stereotype.Component;

@Component
class PharmacyWebMapper {

  PharmacyDto toDto(Pharmacy pharmacy) {
    return new PharmacyDto(pharmacy.name());
  }
}
