package ca.uhn.fhir.jpa.starter.authorization.rules;

import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import org.hl7.fhir.r4.model.MetadataResource;

import java.util.List;

public class MetadataRules extends RuleBase {
  public MetadataRules() {
    this.type = MetadataResource.class;
  }

  @Override
  public List<IAuthRule> handleGet() {
    return new RuleBuilder().allowAll().build();
  }

  @Override
  public List<IAuthRule> handlePost() {
    return new RuleBuilder().denyAll().build();
  }

  public List<IAuthRule> handleUpdate()
  {
    return handlePost();
  }

  @Override
  public List<IAuthRule> handleDelete() {
    return new RuleBuilder().allowAll().build();
  }
}
