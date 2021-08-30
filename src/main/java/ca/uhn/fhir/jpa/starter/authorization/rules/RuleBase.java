package ca.uhn.fhir.jpa.starter.authorization.rules;

import ca.uhn.fhir.jpa.starter.Models.UserType;
import ca.uhn.fhir.jpa.starter.Util.ParameterParser;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.IdType;

import java.util.*;

public abstract class RuleBase {
  protected String denyMessage;
  protected String userId;
  protected UserType userType;
  protected IBaseResource requestResource;

  public RequestTypeEnum requestType;
  protected List<String> idsParamValues = new ArrayList<>();
  public RequestDetails theRequestDetails;
  private String[] allowedRequestParams = new String[]{
    "_id",
    "device",
    "subject",
    "patient",
    "participant",
    "_has:CareTeam:patient:subject",
    "_has:CareTeam:patient:participant",
    "_has:PractitionerRole:practitioner:organization",
    "organization"};

  protected Map<String, SearchParamType> allowedRequestParamsMap = new HashMap<>(){
    {
      put("_id", SearchParamType.Id);
      put("device", SearchParamType.Device);
      put("subject", SearchParamType.Patient);
      put("patient", SearchParamType.Patient);
      put("_has:CareTeam:patient:subject", SearchParamType.Patient);
      put("_has:CareTeam:patient:participant", SearchParamType.Practitioner);
      put("participant", SearchParamType.Practitioner);
      put("_has:PractitionerRole:practitioner:organization", SearchParamType.Organization);
      put("organization", SearchParamType.Organization);
    }
  };

  protected SearchParamType searchParamType;

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

  protected List<IAuthRule> denyRule() {
    return new RuleBuilder()
      .denyAll(denyMessage)
      .build();
  }

  protected static IIdType toIdType(String id, String resourceType) {
    return new IdType(resourceType, id);
  }

  public void setRequestParams(String urlPath)
  {
    var idRegex = this.type.getSimpleName() + "\\/" + "([-A-Za-z0-9]+)";
    var id = ParameterParser.GetParameterFromString(urlPath, idRegex);
    if (id != null)
    {
      this.idsParamValues.add(id);
      this.searchParamType = SearchParamType.Id;
    }
    else
    {
      for(var name : this.allowedRequestParams)
      {
        var regex = name + "=" + "([-A-Za-z0-9,\\/]+)";
        var value = ParameterParser.GetParameterFromString(urlPath, regex);
        if(value == null)
        {
          continue;
        }

        var arr = value.split(",");
        var valArr = Arrays.asList(arr);
        this.idsParamValues.addAll(valArr);
        this.searchParamType = allowedRequestParamsMap.get(name);
        break;
      }
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

  public void SetRequestResource(IBaseResource resource)
  {
    this.requestResource = resource;
  }

  public void SetRequestId(String urlPath)
  {
    var idRegex = this.type.getSimpleName() + "\\/" + "([-A-Za-z0-9]+)";
    var id = ParameterParser.GetParameterFromString(urlPath, idRegex);
    if (id != null)
    {
      this.idsParamValues.add(id);
    }
  }
}