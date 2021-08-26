package ca.uhn.fhir.jpa.starter.ValidationRules;

import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;

public class PractitionerValidation extends ValidationBase{

  public PractitionerValidation()
  {
    ValidationRule get_request_no_params_validation = new ValidationRule(){
      @Override
      public boolean applyRule(RequestDetails requestDetails) {
        if (requestDetails.getRequestType() == RequestTypeEnum.GET && requestDetails.getParameters().isEmpty() && requestDetails.getId() == null)
        {
          return false;
        }

        return true;
      }
    };

    this.validationRules.add(get_request_no_params_validation);
  }
}
