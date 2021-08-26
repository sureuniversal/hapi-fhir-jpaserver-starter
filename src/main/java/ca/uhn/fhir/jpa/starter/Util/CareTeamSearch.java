package ca.uhn.fhir.jpa.starter.Util;

import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CareTeam;

import java.util.ArrayList;
import java.util.List;

public class CareTeamSearch extends Search {
  public static List<IIdType> GetAllowedCareTeamsForUser(String id)
  {
    var allowedAsParticipant = getAllowedCareTeamAsParticipant(id);
    var allowedAsSubject = getAllowedCareTeamAsSubject(id);

    var result = new ArrayList<IIdType>();
    result.addAll(allowedAsParticipant);
    result.addAll(allowedAsSubject);

    return result;
  }

  public static List<IIdType> getAllowedCareTeamAsParticipant(String id){
    List<IIdType> retVal = new ArrayList<>();
    Bundle careTeamBundle = (Bundle) client.search().forResource(CareTeam.class)
      .where(new ReferenceClientParam("participant").hasId(id))
      .execute();

    for (var item : careTeamBundle.getEntry()) {
      retVal.add(item.getResource().getIdElement().toUnqualifiedVersionless());
    }

    return retVal;
  }

  public static List<IIdType> getSubjectsOfCareTeamsSearchingByParticipant(String id){
    List<IIdType> retVal = new ArrayList<>();
    Bundle careTeamBundle = (Bundle) client.search().forResource(CareTeam.class)
      .where(new ReferenceClientParam("participant").hasId(id))
      .execute();

    for (var item : careTeamBundle.getEntry()) {
      CareTeam careTeam = (CareTeam) item.getResource();
      retVal.add(careTeam.getSubject().getReferenceElement());
    }

    return retVal;
  }

  public static List<IIdType> getAllowedCareTeamAsSubject(String id){
    List<IIdType> retVal = new ArrayList<>();
    Bundle careTeamBundle = (Bundle) client.search().forResource(CareTeam.class)
      .where(new ReferenceClientParam("subject").hasId(id))
      .execute();

    for (var item : careTeamBundle.getEntry()) {
      retVal.add(item.getResource().getIdElement().toUnqualifiedVersionless());
    }

    return retVal;
  }

  public static List<IIdType> getAllUsersInCareTeams(List<String> ids){
    List<IIdType> retVal = new ArrayList<>();
    Bundle bundle = (Bundle) client.search().forResource(CareTeam.class)
      .where(new ReferenceClientParam("_id").hasAnyOfIds(ids))
      .execute();

    for (var item : bundle.getEntry()) {
      var careTeam = (CareTeam) item.getResource();
      var subjectId = careTeam.getSubject().getReferenceElement();
      retVal.add(subjectId);

      var participants = careTeam.getParticipant();
      for (var participant : participants)
      {
        var participantId = participant.getMember().getReferenceElement();
        retVal.add(participantId);
      }
    }

    return retVal;
  }
}
