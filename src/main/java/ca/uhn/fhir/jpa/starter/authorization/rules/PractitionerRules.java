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

public class PractitionerRules extends RuleBase {
  public PractitionerRules() {
    this.type = Practitioner.class;
    this.denyMessage = "Practitioner Rule";
  }

  @Override
  public List<IAuthRule> handleGet() {
    var ids = this.GetAllowedPractitioners();
    var allowedOrganization = this.GetUserOrganization();

    var existCounter = 0;
    for (var allowedId : this.idsParamValues) {
      var filtered = ids.stream().filter(e -> e != null && allowedId.contains(e));
      if(filtered.count() > 0)
      {
        existCounter++;
      }

      if (allowedOrganization != null && allowedId.contains(allowedOrganization.getIdPart()))
      {
        existCounter++;
      }
    }

    if (existCounter >= this.idsParamValues.size())
    {
      var allow = new RuleBuilder().allow().read().allResources().withAnyId();

      List<IAuthRule> patientRule = allow.build();
      List<IAuthRule> commonRules = commonRulesGet();
      List<IAuthRule> denyRule = denyRule();

      List<IAuthRule> ruleList = new ArrayList<>();
      ruleList.addAll(patientRule);
      ruleList.addAll(commonRules);
      ruleList.addAll(denyRule);

      return ruleList;
    }

    return denyRule();
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
    if (this.userType == UserType.organizationAdmin || this.userType == UserType.practitioner)
    {
      IIdType userOrganization = Search.getPractitionerOrganization(this.userId);
      var practitioners =
        Search.getAllPractitionersInOrganization(userOrganization.getIdPart()).stream().map(e -> e.getIdPart()).collect(Collectors.toList());
      return practitioners;
    }
    else
    {
      var careTeams = CareTeamSearch.getAllowedCareTeamAsSubject(this.userId).stream().map(e -> e.getIdPart()).collect(Collectors.toList());
      var usersInCareTeam = CareTeamSearch.getAllUsersInCareTeams(careTeams)
        .stream().filter(e -> e != null)
        .map(e -> e.getIdPart()).collect(Collectors.toList());

      var userOrganization = Search.getPatientOrganization(this.userId);
      var practitioners =
        Search.getAllPractitionersInOrganization(userOrganization.getIdPart()).stream().map(e -> e.getIdPart()).collect(Collectors.toList());
      practitioners.addAll(usersInCareTeam);
      return practitioners;
    }
  }

  @Override
  public List<IAuthRule> handleDelete() {
    return new RuleBuilder().denyAll().build();
  }

  private IIdType GetUserOrganization()
  {
    IIdType userOrganization;
    if (this.userType == UserType.organizationAdmin || this.userType == UserType.practitioner)
    {
      userOrganization = Search.getPractitionerOrganization(this.userId);
    }
    else
    {
      userOrganization = Search.getPatientOrganization(this.userId);
    }

    return userOrganization;
  }
}
