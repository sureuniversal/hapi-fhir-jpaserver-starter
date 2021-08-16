package ca.uhn.fhir.jpa.starter.authorization.rules;

import ca.uhn.fhir.jpa.starter.Models.UserType;
import ca.uhn.fhir.jpa.starter.Util.CareTeamSearch;
import ca.uhn.fhir.jpa.starter.Util.Search;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PatientRules extends RuleBase {

  public PatientRules() {
    this.denyMessage = "Patient Rule";
    this.type = Patient.class;
  }

  // sec rules updates
  // We need to also check if the request has an organization parameter ex: Patient?organization=48b07dfb-1b3d-4232-b74c-5efec04ee3d7
  // follow how <ref> idsParamValues </ref> is being built in RuleBase see <ref> setUserIdsRequested </ref>
  // then we should allow the request if it has that parameter as well
  @Override
  public List<IAuthRule> handleGet() {
   var allowed = this.isOperationAllowed();
   if(allowed)
   {
     return new RuleBuilder().allowAll().build();
   }

   return new RuleBuilder().denyAll("Cant add Resource").build();
  }

  // sec rules updates
  // We need to check if the request body for creating a Patient has a ManagingOrganization in it
  // and that it equals the organization for the user sending the request same as userId for patient and
  // see <ref> Search.getPractitionerOrganization </ref> for Practitioner
  @Override
  public List<IAuthRule> handlePost() {
    IIdType userOrganization = this.GetUserOrganization();
    if (this.requestResource != null && userOrganization != null)
    {
      Patient resource = (Patient) this.requestResource;
      if (resource.getManagingOrganization() != null && resource.getManagingOrganization().getReferenceElement().getIdPart().compareTo(userOrganization.getIdPart()) == 0)
      {
        return new RuleBuilder().allowAll().build();
      }
    }

    return new RuleBuilder().denyAll("The added patient organization is not equal to the user organization").build();
  }

  // sec rules updates
  // We need to check if the patient being updated has a ManagingOrganization either in the request body or by performing a search for that field in the db
  // if it is not provided, and that it equals the organization for the user sending the request same as userId for patient and
  // see <ref> Search.getPractitionerOrganization </ref> for Practitioner
  public List<IAuthRule> handleUpdate()
  {
    return new RuleBuilder().allowAll().build();
  }

  @Override
  public List<IAuthRule> handleDelete() {
    return new RuleBuilder().denyAll().build();
  }

  protected boolean isOperationAllowed()
  {
    var userIds = this.setupAllowedUserIdList();
    IIdType allowedOrganization = null;
    if (this.userType == UserType.organizationAdmin || this.userType == UserType.patient)
    {
      allowedOrganization = this.GetUserOrganization();
    }

    var existCounter = 0;
    for (var allowedId : this.idsParamValues) {
      // allowedId.contains(e) is a security concern
      var filtered = userIds.stream().filter(e -> e != null && allowedId.contains(e));
      if(filtered.count() > 0)
      {
        existCounter++;
      }

      if (allowedOrganization != null && allowedOrganization.hasIdPart() && allowedId.contains(allowedOrganization.getIdPart()))
      {
        existCounter++;
      }
    }

    if (existCounter >= this.idsParamValues.size())
    {
      return true;
    }

    return false;
  }

  protected IIdType GetUserOrganization()
  {
    return this.getAllowedOrganization();
  }

  private List<String> setupAllowedUserIdList()
  {
    List<IIdType> userIds = new ArrayList<>();
    if (this.userType == UserType.organizationAdmin || this.userType == UserType.patient)
    {
      var organizationUsers = Search.getAllPatientsInOrganization(getAllowedOrganization().getIdPart());
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
