package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.jpa.starter.Models.AuthRulesWrapper;
import ca.uhn.fhir.jpa.starter.Models.TokenRecord;
import ca.uhn.fhir.jpa.starter.Models.UserType;
import ca.uhn.fhir.jpa.starter.Util.CacheUtil;
import ca.uhn.fhir.jpa.starter.Util.DBUtils;
import ca.uhn.fhir.jpa.starter.Util.Search;
import ca.uhn.fhir.jpa.starter.Util.SecurityRulesUtil;
import ca.uhn.fhir.jpa.starter.ValidationRules.ValidationBase;
import ca.uhn.fhir.jpa.starter.authorization.rules.RuleBase;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Interceptor
public class TokenValidationInterceptor extends AuthorizationInterceptor {
  private final static Map<String, AuthRulesWrapper> ruleCache = new ConcurrentHashMap<> ();
  private final static Map<String, TokenRecord> tokenCache = new ConcurrentHashMap<>();
  public static Timer cacheTimer = new Timer("cache Timer",true);

  static
  {
    cacheTimer.schedule(new TimerTask() {
                          @Override
                          public void run() {
                            try {
                              cleanRuleCache();
                              cleanTokenCache();
                            } catch (Exception e) {
                              org.slf4j.LoggerFactory.getLogger("cacheTimer").error("cacheTimer:", e);
                            }
                          }
                        },
      DBUtils.getCacheTTL(),
      DBUtils.getCacheTTL());
  }

  @Override
  public List<IAuthRule> buildRuleList(RequestDetails theRequestDetails) {
    if (theRequestDetails.getCompleteUrl().split("\\?")[0].contains(":8080")) {
      return new RuleBuilder()
        .allowAll("Port 8080")
        .build();
    }

    if (theRequestDetails.getHeaders("Authorization").size() >= 2)
    {
      return new RuleBuilder()
        .denyAll("more than one auth header sent")
        .build();
    }

    String authHeader = theRequestDetails.getHeader("Authorization");
    if (authHeader == null) {
      return new RuleBuilder()
        .denyAll("no authorization header")
        .build();
    }

    String token = authHeader.replace("Bearer ", "");
    CustomLoggingInterceptor.logDebug(theRequestDetails, "checking cache for token: " + token);
    TokenRecord tokenRecord = getCachedTokenIfExists(token);
    if (tokenRecord == null)
    {
      CustomLoggingInterceptor.logDebug(theRequestDetails, "checking cache for token: " + token + " not in cache");

      tokenRecord = DBUtils.getTokenRecord(token);
      if (tokenRecord == null) {
        CustomLoggingInterceptor.logDebug(theRequestDetails, "token: " + token + " invalid token");

        return new RuleBuilder()
          .denyAll("invalid token")
          .build();
      } else if(tokenRecord.getId() == null){
        return new RuleBuilder()
          .denyAll(tokenRecord.getStatus())
          .build();
      }

      if (tokenRecord.getStatus() != null && tokenRecord.getStatus().equalsIgnoreCase("closed")) {
        return new RuleBuilder()
          .denyAll("status: closed")
          .build();
      }

      if(tokenRecord.is_practitioner()){
        UserType userType = Search.getPractitionerType(tokenRecord.getId());
        if (userType == null)
        {
          return new RuleBuilder()
            .denyAll("Practitioner has no Role!")
            .build();
        }

        tokenRecord.setType(userType);
      }
      else {
        tokenRecord.setType(UserType.patient);
      }

      tokenCache.put(token, tokenRecord);
    }

    CustomLoggingInterceptor.logDebug(theRequestDetails,
      "token type is for: " + (tokenRecord.getType() == UserType.organizationAdmin ? "Organization Admin" : "Practitioner"));

    CustomLoggingInterceptor.logDebug(theRequestDetails, "token " + token + " valid. Token cache size: " + tokenCache.size());

    if (tokenRecord.isAdmin())
    {
      return new RuleBuilder()
        .allowAll("Super Admin")
        .build();
    }

    String userId = tokenRecord.getId();
    String[] scopes = tokenRecord.getScopes();

    ValidationBase validationBase;
    List<RuleBase>  ruleBase;
    try {
      validationBase = SecurityRulesUtil.validationRulesFactory(theRequestDetails);
      ruleBase = SecurityRulesUtil.rulesFactory(theRequestDetails);
    } catch (Exception e) {
      throw new IllegalStateException(e.getMessage());
    }

    List<IAuthRule> rulesList = new ArrayList<>();
    for (var rule : ruleBase)
    {
      String cacheKey = CacheUtil.getCacheEntryForRequest(theRequestDetails, rule, authHeader);
      AuthRulesWrapper cachedRule = getCachedRuleIfExists(cacheKey);
      if (cachedRule != null)
      {
        CustomLoggingInterceptor.logDebug(theRequestDetails, "request in cache");
        //return cachedRule.rules;
      }

      UserType userType = tokenRecord.getType();
      rule.setupUser(userId, userType);
      rule.theRequestDetails = theRequestDetails;

      List<IAuthRule> result = HandleRule(rule,scopes);

      if (cacheKey != null)
      {
        ruleCache.put(cacheKey, new AuthRulesWrapper(result));
      }

      rulesList.addAll(result);
    }

    if (rulesList.isEmpty())
    {
      return new RuleBuilder()
        .denyAll("no Operation")
        .build();
    }

    return rulesList;
  }

  private List<IAuthRule> HandleRule(RuleBase rule, String[] scopes)
  {
    switch (rule.requestType) {
      case TRACE:
      case TRACK:
      case HEAD:
      case CONNECT:
      case OPTIONS:
      case GET:
        return rule.handleGet();
      case PUT:
      case PATCH:
        return rule.handleUpdate();
      case DELETE:
        return rule.handleDelete();
      case POST:
        return rule.handlePost();
      default:
        throw new IllegalStateException("Operation Unknown");
    }
  }

  private static TokenRecord getCachedTokenIfExists(String cacheKey) {
    return CacheUtil.getCacheEntry(tokenCache, cacheKey);
  }

  private static AuthRulesWrapper getCachedRuleIfExists(String cacheKey) {
    if(cacheKey == null) return null;
    return CacheUtil.getCacheEntry(ruleCache, cacheKey);
  }

  public static void cleanTokenCache(){
    CacheUtil.cleanCache(tokenCache);
  }

  public static void cleanRuleCache() {
    CacheUtil.cleanCache(ruleCache);
  }

}
