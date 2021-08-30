package ca.uhn.fhir.jpa.starter.authorization.rules;

import ca.uhn.fhir.jpa.starter.Models.UserType;
import ca.uhn.fhir.jpa.starter.Util.CareTeamSearch;
import ca.uhn.fhir.jpa.starter.Util.Search;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Practitioner;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    var userIds = this.GetAllowedPractitioners();
    var existCounter = this.idsParamValues.stream().filter(e -> e != null && userIds.contains(e)).collect(Collectors.toList()).size();
    if(existCounter >= this.idsParamValues.size())
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

  private List<String> GetAllowedPractitioners()
  {
    IIdType userOrganization = Search.getPractitionerOrganization(this.userId);
    return Search.getAllPractitionersInOrganization(userOrganization.getIdPart()).stream().map(e -> e.getIdPart()).collect(Collectors.toList());
  }

  @Override
  public List<IAuthRule> handleDelete() {
    return new RuleBuilder().denyAll().build();
  }
}
