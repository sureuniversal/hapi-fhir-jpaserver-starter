package ca.uhn.fhir.jpa.starter.authorization.rules;

import ca.uhn.fhir.jpa.starter.Models.UserType;
import ca.uhn.fhir.jpa.starter.Util.Search;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;

import java.util.List;

public class PractitionerRules extends OrganizationRules {
  public PractitionerRules() {
    this.type = Practitioner.class;
    this.denyMessage = "Practitioner Rule";
  }

  @Override
  public List<IAuthRule> handleGet() {
    if (this.searchParamType == SearchParamType.Organization)
    {
      return super.handleGet();
    }

    if (this.allExists())
    {
      return new RuleBuilder().allowAll().build();
    }

    return new RuleBuilder().denyAll("Cannot Get Practitioner Resource").build();
  }

  // Need to check the new Practitioner role and match the organization with the adding user organization
  @Override
  public List<IAuthRule> handlePost() {
    return new RuleBuilder().allowAll().build();
  }

  public List<IAuthRule> handleUpdate(){
    var id = this.idsParamValues.get(0);
    PractitionerRole practitionerRole = Search.getPractitionerRole(id);
    if (practitionerRole == null)
    {
      return new RuleBuilder().allowAll("Practitioner has no role assigned").build();
    }

    var organization = practitionerRole.getOrganization();
    if (organization != null && organization.hasReference() && organization.getReferenceElement().getIdPart() != null)
    {
      this.idsParamValues.clear();
      this.idsParamValues.add(organization.getReferenceElement().getIdPart());
      this.searchParamType = SearchParamType.Organization;
      return super.handleGet();
    }

    return new RuleBuilder().allowAll("Cannot alter this Practitioner").build();
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
      return Search.allPractitionersExistsInOrganization(this.idsParamValues, orgId);
    }

    return this.idsParamValues.size() == 1 && this.idsParamValues.get(0).contains(this.userId);
  }
}
