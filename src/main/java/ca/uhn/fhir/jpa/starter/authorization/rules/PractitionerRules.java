package ca.uhn.fhir.jpa.starter.authorization.rules;

import ca.uhn.fhir.jpa.starter.Util.Search;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import org.hl7.fhir.r4.model.Practitioner;

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

  public List<IAuthRule> handleUpdate()
  {
    return handlePost();
  }

  @Override
  public List<IAuthRule> handleDelete() {
    return new RuleBuilder().denyAll().build();
  }

  private boolean allExists()
  {
    var orgId = getUserOrganization().getIdPart();
    return Search.allPractitionersExistsInOrganization(this.idsParamValues, orgId);
  }
}
