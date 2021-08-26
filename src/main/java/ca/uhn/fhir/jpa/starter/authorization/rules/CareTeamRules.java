package ca.uhn.fhir.jpa.starter.authorization.rules;

import ca.uhn.fhir.jpa.starter.Models.UserType;
import ca.uhn.fhir.jpa.starter.Util.CareTeamSearch;
import ca.uhn.fhir.jpa.starter.Util.Search;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.CareTeam;
import org.hl7.fhir.r4.model.Device;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CareTeamRules extends PatientRules {
  public CareTeamRules() {
    this.type = CareTeam.class;
  }

  @Override
  public List<IAuthRule> handleGet() {
    switch (this.searchParamType)
    {
      case Patient: return super.handleGet();
    }

    if (!this.idsParamValues.isEmpty())
    {
      List<CareTeam> careTeams = Search.getResourcesByIds(this.idsParamValues, CareTeam.class);
      this.idsParamValues.clear();
      for (var careTeam : careTeams)
      {
        var subject = careTeam.getSubject();
        if (subject != null && subject.hasReference() && subject.getReferenceElement().getIdPart() != null)
        {
          this.idsParamValues.add(subject.getReferenceElement().getIdPart());
        }
      }

      return super.handleGet();
    }

    return new RuleBuilder().denyAll("Device Rule").build();
  }

  @Override
  public List<IAuthRule> handlePost() {
    var resource = (CareTeam)this.requestResource;
    var subject = resource.getSubject();
    if (subject != null && subject.hasReference() && subject.getReferenceElement().getIdPart() != null)
    {
      this.idsParamValues.add(subject.getReferenceElement().getIdPart());
      return super.handleGet();
    }

    return new RuleBuilder().denyAll("Cannot add CareTeam for this patient").build();
  }


  @Override
  public List<IAuthRule> handleDelete() {

    return this.handleUpdate();
  }

  @Override
  public List<IAuthRule> handleUpdate()
  {
    var id = this.idsParamValues.get(0);
    CareTeam careTeam = Search.getResourceById(id, CareTeam.class);
    var subject = careTeam.getSubject();
    if (subject != null && subject.hasReference() && subject.getReferenceElement().getIdPart() != null)
    {
      this.idsParamValues.clear();
      this.idsParamValues.add(subject.getReferenceElement().getIdPart());
      return super.handleGet();
    }

    return new RuleBuilder().allowAll("Cannot alter CareTeam for this patient").build();
  }
}
