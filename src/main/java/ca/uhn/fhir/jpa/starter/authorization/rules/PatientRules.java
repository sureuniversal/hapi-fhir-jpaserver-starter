package ca.uhn.fhir.jpa.starter.authorization.rules;

import ca.uhn.fhir.jpa.starter.Models.UserType;
import ca.uhn.fhir.jpa.starter.Util.CareTeamSearch;
import ca.uhn.fhir.jpa.starter.Util.Search;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.DeviceMetric;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PatientRules extends PractitionerRules {

  public PatientRules() {
    this.denyMessage = "Patient Rule";
    this.type = Patient.class;
  }

  @Override
  public List<IAuthRule> handleGet() {
    if (this.searchParamType == SearchParamType.Organization || this.searchParamType == SearchParamType.Practitioner)
    {
      return super.handleGet();
    }

    var userIds = this.setupAllowedUserIdList();
    var existCounter = this.idsParamValues.stream().filter(e -> e != null && userIds.contains(e)).collect(Collectors.toList()).size();
    if(existCounter >= this.idsParamValues.size())
    {
      return new RuleBuilder().allowAll().build();
    }

    return new RuleBuilder().denyAll("Cannot Get Patient Resource").build();
  }

  @Override
  public List<IAuthRule> handlePost() {
    var resource = (Patient)this.requestResource;
    var organization = resource.getManagingOrganization();
    if (organization != null && organization.hasReference() && organization.getReferenceElement().getIdPart() != null)
    {
      this.idsParamValues.add(organization.getReferenceElement().getIdPart());
      return super.handleGet();
    }

    return new RuleBuilder().denyAll("The added patient organization is not equal to the user organization").build();
  }

  public List<IAuthRule> handleUpdate()
  {
    var id = this.idsParamValues.get(0);
    Patient patient = Search.getResourceById(id, Patient.class);
    var organization = patient.getManagingOrganization();
    if (organization != null && organization.hasReference() && organization.getReferenceElement().getIdPart() != null)
    {
      this.idsParamValues.clear();
      this.idsParamValues.add(organization.getReferenceElement().getIdPart());
      return super.handleGet();
    }

    return new RuleBuilder().allowAll("Cannot alter device for this patient").build();
  }

  @Override
  public List<IAuthRule> handleDelete() {
    return this.handleUpdate();
  }

  private List<String> setupAllowedUserIdList()
  {
    List<IIdType> userIds = new ArrayList<>();
    if (this.userType == UserType.organizationAdmin || this.userType == UserType.patient)
    {
      var organizationUsers = Search.getAllPatientsInOrganization(getUserOrganization().getIdPart());
      userIds.addAll(organizationUsers);
    }

    if (this.userType == UserType.patient)
    {
      var careTeams =
        CareTeamSearch
          .getAllowedCareTeamAsSubject(this.userId)
          .stream().map(IIdType::getIdPart).collect(Collectors.toList());

      userIds.addAll(CareTeamSearch.getAllUsersInCareTeams(careTeams));
      userIds.add(RuleBase.toIdType(this.userId, "Patient"));
    }

    if (this.userType == UserType.practitioner)
    {
      userIds.addAll(CareTeamSearch.getSubjectsOfCareTeamsSearchingByParticipant(this.userId));
    }

    var idsList = userIds.stream().map(e -> e.getIdPart()).collect(Collectors.toList());
    return idsList;
  }
}
