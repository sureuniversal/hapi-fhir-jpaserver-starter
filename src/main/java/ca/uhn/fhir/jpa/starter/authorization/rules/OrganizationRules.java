package ca.uhn.fhir.jpa.starter.authorization.rules;

import ca.uhn.fhir.jpa.starter.CustomLoggingInterceptor;
import ca.uhn.fhir.jpa.starter.Models.UserType;
import ca.uhn.fhir.jpa.starter.Util.Search;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Organization;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OrganizationRules extends RuleBase{
  IIdType allowedOrganization = null;
  public OrganizationRules() {
    this.type = Organization.class;
    this.denyMessage = "Organization Rule";
  }

  @Override
  public List<IAuthRule> handleGet() {
    if (this.userType == UserType.organizationAdmin || this.userType == UserType.patient)
    {
      allowedOrganization = this.getUserOrganization();
    }
    else
    {
      return new RuleBuilder().denyAll("Not allowed to search by this organization user is a regular Practitioner").build();
    }

    CustomLoggingInterceptor.logDebug(theRequestDetails, "user Organization: " + allowedOrganization.getIdPart());
    if (this.idsParamValues.size() > 1)
    {
      return new RuleBuilder().denyAll("Cannot Search my multiple organizations").build();
    }

    var orgParam = this.idsParamValues.get(0).replace("Organization/", "");
    if (orgParam.compareTo(allowedOrganization.getIdPart()) == 0)
    {
      return new RuleBuilder().allowAll().build();
    }

    return new RuleBuilder().denyAll("Not allowed to search by this organization").build();
  }

  @Override
  public List<IAuthRule> handlePost() {
    return new RuleBuilder().allowAll().build();
  }

  @Override
  public List<IAuthRule> handleDelete() {
      return handleUpdate();
  }

  public List<IAuthRule> handleUpdate()
  {
    var id = this.idsParamValues.get(0);
    IIdType allowedOrganization;
    if (this.userType == UserType.organizationAdmin)
    {
      allowedOrganization = this.getUserOrganization();
      if (allowedOrganization != null && allowedOrganization.hasIdPart() && id.compareTo(allowedOrganization.getIdPart()) == 0)
      {
        return new RuleBuilder().allowAll().build();
      }
    }

    return new RuleBuilder().denyAll().build();
  }

  protected IIdType getUserOrganization()
  {
    IIdType userOrganization;
    if (this.userType == UserType.organizationAdmin)
    {
      userOrganization = Search.getPractitionerOrganization(this.userId);
    }
    else
    {
      userOrganization = Search.getPatientOrganization(this.userId);
    }

    if (userOrganization == null)
    {
      throw InvalidRequestException.newInstance(400, "User Has no organization");
    }

    return userOrganization;
  }
}
