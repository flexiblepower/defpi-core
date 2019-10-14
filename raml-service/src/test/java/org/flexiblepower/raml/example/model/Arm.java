package org.flexiblepower.raml.example.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Map;

@JsonDeserialize(
    as = ArmImpl.class
)
public interface Arm extends Limb {
  @JsonProperty(
      value = "fingers",
      defaultValue = "5"
  )
  int getFingers();

  @JsonProperty(
      value = "fingers",
      defaultValue = "5"
  )
  void setFingers(int fingers);

  @JsonAnyGetter
  Map<String, Object> getAdditionalProperties();

  @JsonAnySetter
  void setAdditionalProperties(String key, Object value);
}
