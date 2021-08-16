package ca.uhn.fhir.jpa.starter.authorization.rules;

import ca.uhn.fhir.jpa.starter.Util.CareTeamSearch;
import ca.uhn.fhir.jpa.starter.Models.UserType;
import ca.uhn.fhir.jpa.starter.Util.Search;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.IdType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class RuleBase {
  protected String denyMessage;
  protected String userId;
  protected UserType userType;
  protected IBaseResource requestResource;
  protected IIdType requestResourceId;

  public RequestTypeEnum requestType;
  protected List<String> idsParamValues;
  private String[] allowedRequestParams = new String[]{
    "subject",
    "participant",
    "_has:PractitionerRole:practitioner:organization",
    "organization",
    "_has:CareTeam:patient:subject",
    "_has:CareTeam:patient:participant"};

  public Class<? extends IBaseResource> type;

  public RuleBase() {}

  public abstract List<IAuthRule> handleGet();

  public abstract List<IAuthRule> handlePost();

  public abstract List<IAuthRule> handleUpdate();

  public abstract List<IAuthRule> handleDelete();

  protected List<IAuthRule> commonRulesGet() {
    return new RuleBuilder()
      .allow().metadata().andThen().allow().transaction().withAnyOperation().andApplyNormalRules().andThen()
      .allow().patch().allRequests()
      .build();
  }

  protected List<IAuthRule> commonRulesPost() {
    return new RuleBuilder()
      .allow().metadata().andThen()
      .allow().patch().allRequests().andThen()
      .allow().create().resourcesOfType(type).withAnyId()
      .build();
  }

  protected List<IAuthRule> denyRule() {
    return new RuleBuilder()
      .denyAll(denyMessage)
      .build();
  }

  protected static IIdType toIdType(String id, String resourceType) {
    return new IdType(resourceType, id);
  }

  protected IIdType getAllowedOrganization()
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

    if (userOrganization == null)
    {
      throw InvalidRequestException.newInstance(400, "User Has no organization");
    }

    return userOrganization;
  }

  // This might has a security hole in same id for different resources
  public void setRequestParams(RequestDetails theRequestDetails)
  {
    var params = theRequestDetails.getParameters();
    this.idsParamValues = new ArrayList<>();
    if (params != null && !params.isEmpty())
    {
      for(var name : this.allowedRequestParams)
      {
        var value = params.get(name);
        if (value != null)
        {
          var arr = value[0].split(",");
          var valArr = Arrays.asList(arr);
          this.idsParamValues.addAll(valArr);
        }
      }
    }
    else if (theRequestDetails.getId() != null)
    {
      var id = theRequestDetails.getId();
      this.idsParamValues.add(id.getIdPart());
    }
  }

  public void setupUser(String userId, UserType userType)
  {
    this.userId = userId;
    this.userType = userType;
  }

  public void setOperation(RequestTypeEnum requestType)
  {
    this.requestType = requestType;
  }

  protected List<IIdType> handleCareTeam()
  {
    List<IIdType> userIds = new ArrayList<>();
    var allowedIds = CareTeamSearch.GetAllowedCareTeamsForUser(this.userId);
    var ids = new ArrayList<String>();
    for (var entry : allowedIds)
    {
      var id = entry.getIdPart();
      if (id != null) {
        ids.add(entry.getIdPart());
      }
    }

    if (!ids.isEmpty()) {
      var allowedToReadUsers = CareTeamSearch.getAllUsersInCareTeams(ids);
      userIds.addAll(allowedToReadUsers);
    }

    return userIds;
  }

  public void SetRequestResource(IBaseResource resource)
  {
    this.requestResource = resource;
  }

  public void SetRequestId(RequestDetails theRequestDetails)
  {
    this.requestResourceId = theRequestDetails.getId();
  }
}