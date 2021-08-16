package ca.uhn.fhir.jpa.starter.authorization.rules;

import ca.uhn.fhir.jpa.starter.Models.UserType;
import ca.uhn.fhir.jpa.starter.Util.Search;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Organization;

import java.util.ArrayList;
import java.util.List;

public class OrganizationRules extends RuleBase{

  public OrganizationRules() {
    this.type = Organization.class;
    this.denyMessage = "Organization Rule";
  }

  @Override
  public List<IAuthRule> handleGet() {
    var allowedOrgId = this.getAllowedOrganization();
    List<IAuthRule> OrganizationRule =
      new RuleBuilder().allow().read().allResources().inCompartment("Organization",  allowedOrgId).build();

    List<IAuthRule> ruleList = new ArrayList<>();
    List<IAuthRule> commonRules = commonRulesGet();
    List<IAuthRule> denyRule = denyRule();

    ruleList.addAll(OrganizationRule);
    ruleList.addAll(commonRules);
    ruleList.addAll(denyRule);

    return ruleList;
  }

  @Override
  public List<IAuthRule> handlePost() {
    List<IAuthRule> OrganizationRule =
      new RuleBuilder().allow().write().allResources().inCompartment("Organization",  this.getAllowedOrganization()).build();

    List<IAuthRule> ruleList = new ArrayList<>();
    List<IAuthRule> commonRules = commonRulesPost();
    List<IAuthRule> denyRule = denyRule();

    ruleList.addAll(OrganizationRule);
    ruleList.addAll(commonRules);
    ruleList.addAll(denyRule);

    return ruleList;
  }

  @Override
  public List<IAuthRule> handleDelete() {
    return new RuleBuilder().denyAll().build();
  }

  public List<IAuthRule> handleUpdate()
  {
    return handlePost();
  }

}
