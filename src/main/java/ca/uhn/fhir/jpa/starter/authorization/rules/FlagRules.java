package ca.uhn.fhir.jpa.starter.authorization.rules;

import ca.uhn.fhir.jpa.starter.Util.Search;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.Flag;

import java.util.List;

public class FlagRules extends PatientRules {
  public FlagRules() {
    this.denyMessage = "cant access Flag";
    this.type = Flag.class;
  }

  @Override
  public List<IAuthRule> handleGet() {
    switch (this.searchParamType)
    {
      case Patient: return super.handleGet();
    }

    if (!this.idsParamValues.isEmpty()) {
      List<Flag> flags = Search.getResourcesByIds(this.idsParamValues, Flag.class);
      this.idsParamValues.clear();
      for (var flag : flags) {
        var subject = flag.getSubject();
        if (subject != null && subject.hasReference() && subject.getReferenceElement().getIdPart() != null) {
          this.idsParamValues.add(subject.getReferenceElement().getIdPart());
        }
      }

      return super.handleGet();
    }

    return new RuleBuilder().denyAll("Flag Rule").build();
  }

  @Override
  public List<IAuthRule> handlePost() {
    var resource = (Flag)this.requestResource;
    var subject = resource.getSubject();
    if (subject != null && subject.hasReference() && subject.getReferenceElement().getIdPart() != null) {
      this.idsParamValues.add(subject.getReferenceElement().getIdPart());
      return super.handleGet();
    }

    return new RuleBuilder().denyAll("Cannot Create Flag for this patient").build();
  }

  @Override
  public List<IAuthRule> handleDelete() {
    return this.handleUpdate();
  }

  @Override
  public List<IAuthRule> handleUpdate()
  {
    var id = this.idsParamValues.get(0);
    Flag flag = Search.getResourceById(id, Flag.class);
    var subject = flag.getSubject();
    if (subject != null && subject.hasReference() && subject.getReferenceElement().getIdPart() != null)
    {
      this.idsParamValues.clear();
      this.idsParamValues.add(subject.getReferenceElement().getIdPart());
      return super.handleGet();
    }

    return new RuleBuilder().allowAll("Cannot alter Flag for this patient").build();
  }
}
