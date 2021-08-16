package ca.uhn.fhir.jpa.starter.authorization.rules;

import ca.uhn.fhir.jpa.starter.Util.Search;
import ca.uhn.fhir.jpa.starter.Models.UserType;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.DeviceMetric;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DeviceMetricRules extends RuleBase {
  public DeviceMetricRules() {
    this.denyMessage = "DeviceMetric Rule";
    this.type = DeviceMetric.class;
  }

  @Override
  public List<IAuthRule> handleGet() {
//    var allowedDeviceIdRefs = this.setupAllowedIdList();
//    var allowedDeviceMetrics = Search.getDeviceMetricsForDeviceList(allowedDeviceIdRefs);
//    RuleBuilder ruleBuilder = new RuleBuilder();
//    for (var id : allowedDeviceIdRefs) {
//      ruleBuilder.allow().read().allResources().inCompartment("Device", id);
//    }
//
//    for (var id : allowedDeviceMetrics) {
//      ruleBuilder.allow().read().allResources().inCompartment("DeviceMetric", id);
//    }
//    CompartmentDefinition compartmentDefinition = new CompartmentDefinition();
//
//    List<IAuthRule> deviceRule = ruleBuilder.build();
//    List<IAuthRule> commonRules = commonRulesGet();
//    List<IAuthRule> denyRule = denyRule();
//
//    List<IAuthRule> ruleList = new ArrayList<>();
//    ruleList.addAll(deviceRule);
//    ruleList.addAll(commonRules);
//    ruleList.addAll(denyRule);
//
//    return ruleList;

    return new RuleBuilder().allowAll().build();
  }

  @Override
  public List<IAuthRule> handleDelete() {
    return new RuleBuilder().denyAll().build();
  }

  @Override
  public List<IAuthRule> handlePost() {
    var allowedDeviceIdRefs = this.setupAllowedIdList();
    RuleBuilder ruleBuilder = new RuleBuilder();
    for (var id : allowedDeviceIdRefs) {
      ruleBuilder.allow().write().allResources().inCompartment("Device", id);
    }

    List<IAuthRule> deviceRule = ruleBuilder.build();
    List<IAuthRule> commonRules = commonRulesPost();
    List<IAuthRule> denyRule = denyRule();

    List<IAuthRule> ruleList = new ArrayList<>();
    ruleList.addAll(deviceRule);
    ruleList.addAll(commonRules);
    ruleList.addAll(denyRule);

    return ruleList;
  }


  public List<IAuthRule> handleUpdate()
  {
    return handlePost();
  }

  private List<IIdType> setupAllowedIdList()
  {
    List<IIdType> userIds = new ArrayList<>();
    var careTeamUsers = handleCareTeam();
    userIds.addAll(careTeamUsers);

    if (this.userType == UserType.patient)
    {
      userIds.add(RuleBase.toIdType(this.userId, "Patient"));
    }
    else
    {
      var patients = Search.getPatients(this.userId);
      userIds.addAll(patients);
    }

    return Search.getDevices(userIds);
  }
}
