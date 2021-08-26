package ca.uhn.fhir.jpa.starter.Util;

import ca.uhn.fhir.jpa.starter.ValidationRules.PatientValidation;
import ca.uhn.fhir.jpa.starter.ValidationRules.PractitionerValidation;
import ca.uhn.fhir.jpa.starter.ValidationRules.ValidationBase;
import ca.uhn.fhir.jpa.starter.authorization.rules.*;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;

import java.util.ArrayList;
import java.util.List;

public class SecurityRulesUtil {
  public static List<RuleBase> rulesFactory(RequestDetails theRequestDetails) throws Exception {
    List<RuleBase> rulesList = new ArrayList<>();
    if(theRequestDetails.getRestOperationType() == RestOperationTypeEnum.TRANSACTION){
      var bundleRes = ((Bundle)theRequestDetails.getResource()).getEntry();

      for (var item : bundleRes)
      {
        String method = item.getRequest().getMethod().getDisplay();
        RequestTypeEnum operation = convertToRequestType(method);
        String resName = item.getRequest().getUrl().split("[/\\?]")[0];
        RuleBase rule = rulesFactory(resName);

        SecurityRulesUtil.setRequestParams(rule, item.getRequest().getUrl(), operation);
        SecurityRulesUtil.setRequestResourceId(rule, item.getRequest().getUrl(), operation);
        SecurityRulesUtil.setRequestResourceBody(rule, item.getResource(), operation);

        rule.setOperation(operation);
        rulesList.add(rule);
      }
    }
    else
    {
      String compartmentName = theRequestDetails.getRequestPath().split("/")[0];
      var operation = theRequestDetails.getRequestType();
      var rule = rulesFactory(compartmentName);

      SecurityRulesUtil.setRequestParams(rule, theRequestDetails.getCompleteUrl(), operation);
      SecurityRulesUtil.setRequestResourceId(rule, theRequestDetails.getCompleteUrl(), operation);
      SecurityRulesUtil.setRequestResourceBody(rule, theRequestDetails.getResource(), operation);

      rule.setOperation(operation);
      rulesList.add(rule);
    }

    return rulesList;
  }

  private static RuleBase rulesFactory(String compartmentName) throws Exception {
    switch (compartmentName) {
      case "Flag":  return new FlagRules();
      case "Observation":return new ObservationRules();
      case "CareTeam": return new CareTeamRules();
      case "Patient": return new PatientRules();
      case "Practitioner": return new PractitionerRules();
      case "DeviceMetric": return new DeviceMetricRules();
      case "Device": return new DeviceRules();
      case "metadata": return new MetadataRules();
      case "PractitionerRole": return new PractitionerRoleRules();
      case "Organization": return new OrganizationRules();
      case "DiagnosticReport": return new DiagnosticReportRules();
      case "Media": return new MediaRule();
      default:
        throw new Exception("Method does not exist");
    }
  }

  public static ValidationBase validationRulesFactory(RequestDetails theRequestDetails) throws Exception {
    if(theRequestDetails.getRestOperationType() == RestOperationTypeEnum.TRANSACTION){
      return new ValidationBase();
    }

    String compartmentName = theRequestDetails.getRequestPath().split("/")[0];
    switch (compartmentName) {
      case "Patient": return new PatientValidation();
      case "Practitioner": return new PractitionerValidation();
      case "Flag":
      case "Observation":
      case "CareTeam":
      case "DeviceMetric":
      case "Device":
      case "metadata":
      case "PractitionerRole":
      case "Organization":
      case "DiagnosticReport":
      case "Media": return new ValidationBase();
      default:
        throw new Exception("Method does not exist");
    }
  }

  private static void setRequestResourceId(RuleBase rule, String url, RequestTypeEnum requestTypeEnum)
  {
    switch (requestTypeEnum) {
      case PATCH:
      case DELETE:
      case PUT: rule.SetRequestId(url);
    }
  }

  private static void setRequestResourceBody(RuleBase rule, IBaseResource resource, RequestTypeEnum requestTypeEnum)
  {
    switch (requestTypeEnum) {
      case POST: rule.SetRequestResource(resource);
    }
  }

  private static void setRequestParams(RuleBase rule, String url, RequestTypeEnum requestTypeEnum)
  {
    switch (requestTypeEnum) {
      case GET: rule.setRequestParams(url);
    }
  }

  private static RequestTypeEnum convertToRequestType(String method)
  {
    var methodNormalized = method.toUpperCase();
    switch (methodNormalized)
    {
      case "POST": return RequestTypeEnum.POST;
      case "PATCH": return RequestTypeEnum.PATCH;
      case "DELETE": return RequestTypeEnum.DELETE;
      case "PUT": return RequestTypeEnum.PUT;
      default:
        return RequestTypeEnum.GET;
    }
  }
}
