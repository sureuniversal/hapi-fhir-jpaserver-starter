package ca.uhn.fhir.jpa.starter.authorization.rules;

import ca.uhn.fhir.jpa.starter.Util.Search;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import org.hl7.fhir.r4.model.Device;

import java.util.List;

public class DeviceRules extends PatientRules {

  public DeviceRules() {
    this.denyMessage = "Device Rule";
    this.type = Device.class;
  }

  @Override
  public List<IAuthRule> handleGet() {
    if (this.searchParamType == SearchParamType.Patient)
    {
      return super.handleGet();
    }

    if (!this.idsParamValues.isEmpty()) {
      List<Device> devices = Search.getResourcesByIds(this.idsParamValues, Device.class);
      this.idsParamValues.clear();
      for (var device : devices) {
        var devicePatient = device.getPatient();
        if (devicePatient != null && devicePatient.hasReference() && devicePatient.getReferenceElement().getIdPart() != null) {
          this.idsParamValues.add(devicePatient.getReferenceElement().getIdPart());
        }
      }

      return super.handleGet();
    }

    return new RuleBuilder().denyAll("Device Rule").build();
  }

  @Override
  public List<IAuthRule> handlePost() {
    var resource = (Device)this.requestResource;
    var devicePatient = resource.getPatient();
    if (devicePatient != null && devicePatient.hasReference() && devicePatient.getReferenceElement().getIdPart() != null)
    {
      this.idsParamValues.add(devicePatient.getReferenceElement().getIdPart());
      return super.handleGet();
    }

    return new RuleBuilder().denyAll("Cannot add device for this patient").build();
  }

  @Override
  public List<IAuthRule> handleDelete() {
    return this.handleUpdate();
  }

  @Override
  public List<IAuthRule> handleUpdate()
  {
    var id = this.idsParamValues.get(0);
    Device device = Search.getResourceById(id, Device.class);
    var devicePatient = device.getPatient();
    if (devicePatient != null && devicePatient.hasReference() && devicePatient.getReferenceElement().getIdPart() != null)
    {
      this.idsParamValues.clear();
      this.idsParamValues.add(devicePatient.getReferenceElement().getIdPart());
      return super.handleGet();
    }

    return new RuleBuilder().allowAll("Cannot alter device for this patient").build();
  }
}
