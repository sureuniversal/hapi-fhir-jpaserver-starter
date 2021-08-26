package ca.uhn.fhir.jpa.starter.ValidationRules;

import ca.uhn.fhir.rest.api.server.RequestDetails;

public interface ValidationRule {
  default boolean applyRule(RequestDetails requestDetails) {
    return false;
  }
}
