package ca.uhn.fhir.jpa.starter.authorization.rules;

import ca.uhn.fhir.jpa.starter.Util.Search;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Flag;
import org.hl7.fhir.r4.model.Media;

import java.util.List;

public class MediaRule extends PatientRules {

  public MediaRule()
  {
    this.type = Media.class;
    this.denyMessage = "Media Rule";
  }


  @Override
  public List<IAuthRule> handleGet() {
    switch (this.searchParamType)
    {
      case Patient: return super.handleGet();
    }

    if (!this.idsParamValues.isEmpty()) {
      List<Media> mediaList = Search.getResourcesByIds(this.idsParamValues, Media.class);
      this.idsParamValues.clear();
      for (var media : mediaList) {
        var subject = media.getSubject();
        if (subject != null && subject.hasReference() && subject.getReferenceElement().getIdPart() != null) {
          this.idsParamValues.add(subject.getReferenceElement().getIdPart());
        }
      }

      return super.handleGet();
    }

    return new RuleBuilder().denyAll("Media Rule").build();
  }

  @Override
  public List<IAuthRule> handlePost() {
    var resource = (Media)this.requestResource;
    var subject = resource.getSubject();
    if (subject != null && subject.hasReference() && subject.getReferenceElement().getIdPart() != null) {
      this.idsParamValues.add(subject.getReferenceElement().getIdPart());
      return super.handleGet();
    }

    return new RuleBuilder().denyAll("Cannot Create Media for this patient").build();
  }

  @Override
  public List<IAuthRule> handleDelete() {
    return this.handleUpdate();
  }

  @Override
  public List<IAuthRule> handleUpdate()
  {
    var id = this.idsParamValues.get(0);
    Media flag = Search.getResourceById(id, Media.class);
    var subject = flag.getSubject();
    if (subject != null && subject.hasReference() && subject.getReferenceElement().getIdPart() != null)
    {
      this.idsParamValues.clear();
      this.idsParamValues.add(subject.getReferenceElement().getIdPart());
      return super.handleGet();
    }

    return new RuleBuilder().allowAll("Cannot alter Media for this patient").build();
  }
}
