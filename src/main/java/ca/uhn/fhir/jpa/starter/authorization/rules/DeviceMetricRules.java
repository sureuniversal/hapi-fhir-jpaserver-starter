package ca.uhn.fhir.jpa.starter.authorization.rules;

import ca.uhn.fhir.jpa.starter.Util.Search;
import ca.uhn.fhir.jpa.starter.Models.UserType;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.DeviceMetric;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DeviceMetricRules extends DeviceRules {
  public DeviceMetricRules() {
    this.denyMessage = "DeviceMetric Rule";
    this.type = DeviceMetric.class;
  }

  @Override
  public List<IAuthRule> handleGet() {
    if (this.searchParamType == SearchParamType.Device) {
      return super.handleGet();
    }

    if (!this.idsParamValues.isEmpty()) {
      List<DeviceMetric> deviceMetrics = Search.getResourcesByIds(this.idsParamValues, DeviceMetric.class);
      this.idsParamValues.clear();
      for (var deviceMetric : deviceMetrics) {
        var device = deviceMetric.getParent();
        if (device != null && device.hasReference() && device.getReferenceElement().getIdPart() != null) {
          this.idsParamValues.add(device.getReferenceElement().getIdPart());
        }
      }
    }

    return super.handleGet();
  }

  @Override
  public List<IAuthRule> handleDelete() {
    return this.handleUpdate();
  }

  @Override
  public List<IAuthRule> handlePost() {
    var resource = (DeviceMetric)this.requestResource;
    var device = resource.getSource();
    if (device != null && device.hasReference() && device.getReferenceElement().getIdPart() != null)
    {
      this.idsParamValues.clear();
      this.idsParamValues.add(device.getReferenceElement().getIdPart());
      return super.handleGet();
    }

    return new RuleBuilder().denyAll("Cannot add device for this patient").build();
  }

  @Override
  public List<IAuthRule> handleUpdate()
  {
    var id = this.idsParamValues.get(0);
    DeviceMetric deviceMetric = Search.getResourceById(id, DeviceMetric.class);
    var device = deviceMetric.getParent();
    if (device != null && device.hasReference() && device.getReferenceElement().getIdPart() != null)
    {
      this.idsParamValues.clear();
      this.idsParamValues.add(device.getReferenceElement().getIdPart());
      return super.handleGet();
    }

    return new RuleBuilder().allowAll("Cannot alter device for this patient").build();
  }
}
