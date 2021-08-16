package ca.uhn.fhir.jpa.starter.authorization.rules;

import ca.uhn.fhir.jpa.starter.Models.UserType;
import ca.uhn.fhir.jpa.starter.Util.CareTeamSearch;
import ca.uhn.fhir.jpa.starter.Util.Search;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import org.hl7.fhir.instance.model.api.IIdType;
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

  // sec rules updates
  // We need to check if the request body to see if the subject for the observation is referencing the sending user
  @Override
  public List<IAuthRule> handlePost() {
    var allowedUsers = this.GetAllowedUsersToAddAnObs();

    if (this.requestResource != null )
    {
      Observation resource = (Observation) this.requestResource;
      if (resource.hasSubject())
      {
        var subject = resource.getSubject().getReferenceElement().getIdPart();
        var exists = false;
        for (var allowedId : allowedUsers) {
          exists = exists || allowedId.compareTo(subject) == 0;
        }

        if (exists)
        {
          return new RuleBuilder().allowAll().build();
        }
      }
    }

    return new RuleBuilder().denyAll("This user can not add observation to this patient").build();
  }

  @Override
  public List<IAuthRule> handleDelete() {
   if (this.requestResourceId == null)
   {
     throw InvalidRequestException.newInstance(400, "\"No Observation Id");
   }

   var observation = Search.getResourceById(this.requestResourceId.getIdPart(), Observation.class);
   if (observation == null) {
     throw InvalidRequestException.newInstance(404, "\"No Observation found");
   }

    if(observation.hasSubject())
    {
      var subjectId = observation.getSubject().getReferenceElement().getIdPart();
      var allowedIds = this.GetAllowedUsersToAddAnObs();
      if (allowedIds.contains(subjectId))
      {
        return new RuleBuilder().allowAll().build();
      }
    }

    return new RuleBuilder().denyAll("Not allowed to delete observation").build();
  }

  // sec rules updates
  // override handleUpdate and give it a deny all for an observation
  private List<String> GetAllowedUsersToAddAnObs()
  {
    List<IIdType> userIds = new ArrayList<>();
    if (this.userType == UserType.organizationAdmin || this.userType == UserType.patient)
    {
      var organizationUsers = Search.getAllPatientsInOrganization(getAllowedOrganization().getIdPart());
      userIds.addAll(organizationUsers);
    }
    else if (this.userType == UserType.practitioner)
    {
      var allowedCareTeams = CareTeamSearch.getAllowedCareTeamAsParticipant(this.userId).stream().map(e -> e.getIdPart()).collect(Collectors.toList());
      var usersInCareTeam = CareTeamSearch.getAllUsersInCareTeams(allowedCareTeams)
        .stream().filter(e -> e.getResourceType().compareTo("Patient") == 0).collect(Collectors.toList());

      userIds.addAll(usersInCareTeam);
    }
    else
    {
      userIds.add(RuleBase.toIdType(this.userId, "Patient"));
    }

    return userIds.stream().map(e -> e.getIdPart()).collect(Collectors.toList());
  }
}