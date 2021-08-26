package ca.uhn.fhir.jpa.starter.authorization.rules;

import ca.uhn.fhir.jpa.starter.Models.UserType;
import ca.uhn.fhir.jpa.starter.Util.CareTeamSearch;
import ca.uhn.fhir.jpa.starter.Util.Search;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Flag;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ObservationRules extends PatientRules {
  public ObservationRules() {
    this.denyMessage = "cant access Observation";
    this.type = Observation.class;
  }

  @Override
  public List<IAuthRule> handleGet() {
    switch (this.searchParamType)
    {
      case Patient: return super.handleGet();
    }

    if (!this.idsParamValues.isEmpty()) {
      List<Observation> observations = Search.getResourcesByIds(this.idsParamValues, Observation.class);
      this.idsParamValues.clear();
      for (var observation : observations) {
        var subject = observation.getSubject();
        if (subject != null && subject.hasReference() && subject.getReferenceElement().getIdPart() != null) {
          this.idsParamValues.add(subject.getReferenceElement().getIdPart());
        }
      }

      return super.handleGet();
    }

    return new RuleBuilder().denyAll("Observation Rule").build();
  }

  @Override
  public List<IAuthRule> handlePost() {
    var resource = (Observation)this.requestResource;
    var subject = resource.getSubject();
    if (subject != null && subject.hasReference() && subject.getReferenceElement().getIdPart() != null) {
      this.idsParamValues.add(subject.getReferenceElement().getIdPart());
      return super.handleGet();
    }

    return new RuleBuilder().denyAll("Cannot Create Observation for this patient").build();
  }

  @Override
  public List<IAuthRule> handleDelete() {
    return this.handleUpdate();
  }

  @Override
  public List<IAuthRule> handleUpdate()
  {
    var id = this.idsParamValues.get(0);
    Observation observation = Search.getResourceById(id, Observation.class);
    var subject = observation.getSubject();
    if (subject != null && subject.hasReference() && subject.getReferenceElement().getIdPart() != null)
    {
      this.idsParamValues.clear();
      this.idsParamValues.add(subject.getReferenceElement().getIdPart());
      return super.handleGet();
    }

    return new RuleBuilder().allowAll("Cannot alter Observation for this patient").build();
  }
}