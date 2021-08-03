package ca.uhn.fhir.jpa.starter.Util;

import ca.uhn.fhir.jpa.starter.Models.CacheRecord;
import ca.uhn.fhir.jpa.starter.authorization.rules.RuleBase;
import ca.uhn.fhir.rest.api.server.RequestDetails;

import java.util.ArrayList;
import java.util.Arrays;
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
    var params = theRequestDetails.getParameters();
    StringBuilder askedUsers = new StringBuilder();
    if (params != null && !params.isEmpty())
    {
      for(var name : userIdsParamName)
      {
        var value = params.get(name);
        if (value != null)
        {
          var val = value[0];
          askedUsers.append(val);
        }
      }
    }
    else if (theRequestDetails.getId() != null)
    {
      try {
        var id = theRequestDetails.getId();
        askedUsers = new StringBuilder(id.getIdPart());
      } catch (Exception e) {
        askedUsers = new StringBuilder("error-id");
      }
    }

    var type = rule.type.getName();
    var operation = rule.requestType;
    var cacheKey = authHeader + '-' + type + '-' + operation + '-' + askedUsers;
    return cacheKey;
  }
}
