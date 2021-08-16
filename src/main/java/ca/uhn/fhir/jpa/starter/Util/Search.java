package ca.uhn.fhir.jpa.starter.Util;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.starter.Models.UserType;
import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Search {
  protected static IGenericClient client = null;
  static final String server;

  static {
    server = System.getenv("INTERNAL_SERVER");
  }


  public static IGenericClient getClient() {
    return client;
  }

  public static void setClient(IGenericClient client) {
    Search.client = client;
  }

  public static void setClientByContext(FhirContext ctx) {

    HttpClient httpClient = HttpClientBuilder.create().build();
    ctx.getRestfulClientFactory().setHttpClient(httpClient);

    Search.setClient(ctx.newRestfulGenericClient(server));
  }

  public static <T extends BaseResource> T getResourceById(String id, Class<T> tClass)
  {
    return client.read().resource(tClass).withId(id).execute();
  }

  public static List<IIdType> getDevices(List<IIdType> patientIds) {
    List<IIdType> retVal = new ArrayList<>();
    var ids = new ArrayList<String>();
    for (var id : patientIds) {
      ids.add(id.getIdPart());
    }

    Bundle deviceBundle = (Bundle) client.search().forResource(Device.class)
      .where(new ReferenceClientParam("patient").hasAnyOfIds(ids))
      .execute();

    for (var itm : deviceBundle.getEntry()) {
      retVal.add(itm.getResource().getIdElement().toUnqualifiedVersionless());
    }

    return retVal;
  }

  public static List<IIdType> getDeviceMetricsForDeviceList(List<IIdType> deviceIds)
  {
    List<IIdType> retVal = new ArrayList<>();
    var ids = new ArrayList<String>();
    for (var id : deviceIds) {
      ids.add(id.getIdPart());
    }

    Bundle deviceBundle = (Bundle) client.search().forResource(DeviceMetric.class)
      .where(new ReferenceClientParam("source").hasAnyOfIds(ids))
      .execute();

    for (var itm : deviceBundle.getEntry()) {
      retVal.add(itm.getResource().getIdElement().toUnqualifiedVersionless());
    }

    return retVal;
  }

  public static List<IIdType> getPatients(String practitionerId) {
    List<IIdType> patients = new ArrayList<>();
    Bundle patientBundle = (Bundle) client.search().forResource(Patient.class)
      .where(new ReferenceClientParam("general-practitioner").hasId(practitionerId))
      .execute();
    for (Bundle.BundleEntryComponent item : patientBundle.getEntry()) {
      patients.add(item.getResource().getIdElement().toUnqualifiedVersionless());
    }
    return patients;
  }

  public static UserType getPractitionerType(String practitioner){
    var role = getPractitionerRole(practitioner);

    if (role == null)
    {
      return null;
    }

    if (!role.hasOrganization())
    {
      return UserType.superAdmin;
    }

    if (role.getCode().stream().anyMatch(c-> c.hasCoding("http://snomed.info/sct","56542007"))){
        return UserType.organizationAdmin;
    }

    return UserType.practitioner;
  }

  public static Patient getPatientById(String patientId){
    Bundle bundle = (Bundle) client.search().forResource(Patient.class)
      .where(new TokenClientParam("_id").exactly().code(patientId))
      .execute();

    if (bundle.getEntry().isEmpty())
    {
      return null;
    }

    return ((Patient)(bundle.getEntry().get(0).getResource()));
  }

  public static IIdType getPatientOrganization(String patientId){
    Bundle bundle = (Bundle) client.search().forResource(Patient.class)
      .where(new TokenClientParam("_id").exactly().code(patientId))
      .execute();

    if (bundle.getEntry().isEmpty())
    {
      return null;
    }

    var patientObj = ((Patient)(bundle.getEntry().get(0).getResource()));
    if (patientObj.getManagingOrganization() == null || !patientObj.getManagingOrganization().hasReference())
    {
      return null;
    }

    return patientObj.getManagingOrganization().getReferenceElement();
  }

  public static IIdType getPractitionerOrganization(String practitioner){
    var role = getPractitionerRole(practitioner);
    return role.getOrganization().getReferenceElement();
  }

  public static PractitionerRole getPractitionerRole(String practitioner){
    Bundle role = (Bundle) client.search().forResource(PractitionerRole.class)
      .where(new ReferenceClientParam("practitioner").hasId(practitioner))
      .execute();

    if (role.getEntry().isEmpty())
    {
      return null;
    }

    return (PractitionerRole) role.getEntry().get(0).getResource();
  }

  public static List<IIdType> getAllPatientsInOrganization(String organizationId)
  {
    CacheControlDirective s = new CacheControlDirective();
    s.setNoStore(true);
    s.setNoCache(true);

    Bundle patientsList = (Bundle) client.search().forResource(Patient.class)
      .where(new ReferenceClientParam("organization").hasId(organizationId)).cacheControl(s)
      .execute();

    var patientIds =
      patientsList.getEntry().stream().map(e -> e.getResource().getIdElement().toUnqualifiedVersionless()).collect(Collectors.toList());

    List<IIdType> ids = new ArrayList<>();
    ids.addAll(patientIds);
    return ids;
  }

  public static List<IIdType> getAllPractitionersInOrganization(String organizationId)
  {
    CacheControlDirective s = new CacheControlDirective();
    s.setNoStore(true);
    s.setNoCache(true);

    Bundle practitionerList = (Bundle) client.search().forResource(PractitionerRole.class)
      .where(new ReferenceClientParam("organization").hasId(organizationId)).cacheControl(s)
      .execute();

    var practitionerIds =
      practitionerList.getEntry().stream()
        .map(e -> ((PractitionerRole) e.getResource()).getPractitioner().getReferenceElement().toUnqualifiedVersionless()).collect(Collectors.toList());

    List<IIdType> ids = new ArrayList<>();
    ids.addAll(practitionerIds);
    return ids;
  }
}
