package ca.uhn.fhir.jpa.starter.authorization.rules;

import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import org.hl7.fhir.r4.model.Device;

import java.util.List;

public class DeviceRules extends RuleBase {
  public DeviceRules() {
    this.denyMessage = "Device Rule";
    this.type = Device.class;
  }

  @Override
  public List<IAuthRule> handleGet() {
    return new RuleBuilder().allowAll().build();
  }

  @Override
  public List<IAuthRule> handlePost() {
    return new RuleBuilder().allowAll().build();
  }

  @Override
  public List<IAuthRule> handleDelete() {
    return new RuleBuilder().allowAll().build();
  }

  @Override
  public List<IAuthRule> handleUpdate()
  {
    return handlePost();
  }
}
