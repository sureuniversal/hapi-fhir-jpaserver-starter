package ca.uhn.fhir.jpa.starter.ValidationRules;

import ca.uhn.fhir.rest.api.server.RequestDetails;

import java.util.ArrayList;
import java.util.List;

public class ValidationBase {
  List<ValidationRule> validationRules = new ArrayList<>();

  public boolean applyRules(RequestDetails requestDetails)
  {
    for (var rule : this.validationRules)
    {
      if(!rule.applyRule(requestDetails))
      {
        return false;
      }
    }

    return true;
  }
}
