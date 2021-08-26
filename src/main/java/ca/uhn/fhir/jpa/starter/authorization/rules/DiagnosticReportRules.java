package ca.uhn.fhir.jpa.starter.authorization.rules;

import ca.uhn.fhir.jpa.starter.Util.Search;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import org.hl7.fhir.r4.model.DiagnosticReport;

import java.util.List;

public class DiagnosticReportRules extends PatientRules {
  public DiagnosticReportRules() {
    this.denyMessage = "cant access diagnostic report";
    this.type = DiagnosticReport.class;
  }


  @Override
  public List<IAuthRule> handleGet() {
    switch (this.searchParamType)
    {
      case Patient: return super.handleGet();
    }

    if (!this.idsParamValues.isEmpty()) {
      List<DiagnosticReport> diagnosticReports = Search.getResourcesByIds(this.idsParamValues, DiagnosticReport.class);
      this.idsParamValues.clear();
      for (var report : diagnosticReports) {
        var subject = report.getSubject();
        if (subject != null && subject.hasReference() && subject.getReferenceElement().getIdPart() != null) {
          this.idsParamValues.add(subject.getReferenceElement().getIdPart());
        }
      }

      return super.handleGet();
    }

    return new RuleBuilder().denyAll("DiagnosticReport Rule").build();
  }

  @Override
  public List<IAuthRule> handlePost() {
    var resource = (DiagnosticReport)this.requestResource;
    var subject = resource.getSubject();
    if (subject != null && subject.hasReference() && subject.getReferenceElement().getIdPart() != null) {
      this.idsParamValues.add(subject.getReferenceElement().getIdPart());
      return super.handleGet();
    }

    return new RuleBuilder().denyAll("Cannot Create DiagnosticReport for this patient").build();
  }

  @Override
  public List<IAuthRule> handleDelete() {
    return this.handleUpdate();
  }

  @Override
  public List<IAuthRule> handleUpdate()
  {
    var id = this.idsParamValues.get(0);
    DiagnosticReport diagnosticReport = Search.getResourceById(id, DiagnosticReport.class);
    var subject = diagnosticReport.getSubject();
    if (subject != null && subject.hasReference() && subject.getReferenceElement().getIdPart() != null)
    {
      this.idsParamValues.clear();
      this.idsParamValues.add(subject.getReferenceElement().getIdPart());
      return super.handleGet();
    }

    return new RuleBuilder().allowAll("Cannot alter DiagnosticReport for this patient").build();
  }
}
