package ca.uhn.fhir.jpa.starter.authorization.rules;

import ca.uhn.fhir.jpa.starter.Models.UserType;
import ca.uhn.fhir.jpa.starter.Util.Search;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import org.hl7.fhir.r4.model.Patient;

import java.util.List;

public class PatientRules extends PractitionerRules {

  public PatientRules() {
    this.denyMessage = "Patient Rule";
    this.type = Patient.class;
  }

  @Override
  public List<IAuthRule> handleGet() {
    if (this.searchParamType == SearchParamType.Organization || this.searchParamType == SearchParamType.Practitioner)
    {
      return super.handleGet();
    }

    if (this.allExists())
    {
      return new RuleBuilder().allowAll().build();
    }

    return new RuleBuilder().denyAll("Cannot Get Patient Resource").build();
  }

  @Override
  public List<IAuthRule> handlePost() {
    var resource = (Patient)this.requestResource;
    var organization = resource.getManagingOrganization();
    if (organization != null && organization.hasReference() && organization.getReferenceElement().getIdPart() != null)
    {
      this.idsParamValues.add(organization.getReferenceElement().getIdPart());
      this.searchParamType = SearchParamType.Organization;
      return super.handleGet();
    }

    return new RuleBuilder().denyAll("The added patient organization is not equal to the user organization").build();
  }

  public List<IAuthRule> handleUpdate()
  {
    var id = this.idsParamValues.get(0);
    Patient patient = Search.getResourceById(id, Patient.class);
    var organization = patient.getManagingOrganization();
    if (organization != null && organization.hasReference() && organization.getReferenceElement().getIdPart() != null)
    {
      this.idsParamValues.clear();
      this.idsParamValues.add(organization.getReferenceElement().getIdPart());
      this.searchParamType = SearchParamType.Organization;
      return super.handleGet();
    }

    return new RuleBuilder().allowAll("Cannot alter this patient").build();
  }

  @Override
  public List<IAuthRule> handleDelete() {
    return this.handleUpdate();
  }

  private boolean allExists()
  {
    if (this.userType == UserType.organizationAdmin)
    {
      var orgId = getUserOrganization().getIdPart();
      return Search.allPatientsExistsInOrganization(this.idsParamValues, orgId);
    }

    return Search.practitionerExitsForAllPatientsInCareTeam(this.idsParamValues, this.userId);
  }
}
