package ca.uhn.fhir.jpa.starter.Util;

import ca.uhn.fhir.jpa.starter.Models.CacheRecord;
import ca.uhn.fhir.jpa.starter.authorization.rules.RuleBase;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Flag;
import org.hl7.fhir.r4.model.Observation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CacheUtil {
  private static final String[] userIdsParamName = new String[]{ "subject", "participant" };

  public static <T extends CacheRecord> T getCacheEntry(Map<String, T> cache, String cacheKey){
    T cachedRule = cache.get(cacheKey);
    if (cachedRule != null) {
      long recordTtl = cachedRule.recordTtl;
      if ((recordTtl - System.currentTimeMillis()) > 999) {
        return cachedRule;
      } else {
        cache.remove(cacheKey);
      }
    }

    return null;
  }

  public static <T extends CacheRecord> void cleanCache(Map<String, T> cache){
    List<String> removeList = new ArrayList<>();
    cache.forEach((k, v) -> {
      if (v.isRecordExpired()) {
        removeList.add(k);
      }
    });

    removeList.forEach(cache::remove);
  }

  public static String getCacheEntryForRequest(RequestDetails theRequestDetails, RuleBase rule, String authHeader)
  {
    StringBuilder askedUsers = new StringBuilder();
    if(theRequestDetails.getRequestType() == RequestTypeEnum.GET || theRequestDetails.getRequestType() == RequestTypeEnum.DELETE) {
      Map<String, String[]> params = theRequestDetails.getParameters();
      if (params != null && !params.isEmpty()) {
        for (String name : userIdsParamName) {
          String[] value = params.get(name);
          if (value != null) {
            String val = value[0];
            askedUsers.append(val);
          }
        }
      }
      else if (theRequestDetails.getId() != null)
      {
        try {
          IIdType id = theRequestDetails.getId();
          askedUsers.append(id.getIdPart());
        } catch (Exception e) {
          return null;
        }
      }
    } else if(theRequestDetails.getResource() != null) {
      IBaseResource resource = theRequestDetails.getResource();
      switch (resource.fhirType()){
        case "Observation":
          askedUsers.append(((Observation)resource).getSubject().getReferenceElement().getIdPart());
          break;
        case "Flag":
          askedUsers.append(((Flag)resource).getSubject().getReferenceElement().getIdPart());
          break;
        case "Patient":
        case "Practitioner":
        default:
          return null;
      }
    } else {
      return null;
    }

    String type = rule.type.getName();
    RequestTypeEnum operation = rule.requestType;
    return authHeader + '-' + type + '-' + operation + '-' + askedUsers;
  }
}
