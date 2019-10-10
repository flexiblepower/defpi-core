package org.flexiblepower.raml.example.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("fingers")
public class ArmImpl implements Arm {
  @JsonProperty(
      value = "fingers",
      defaultValue = "5"
  )
  private int fingers;

  @JsonIgnore
  private Map<String, Object> additionalProperties = new ExcludingMap();

  @JsonProperty(
      value = "fingers",
      defaultValue = "5"
  )
  public int getFingers() {
    return this.fingers;
  }

  @JsonProperty(
      value = "fingers",
      defaultValue = "5"
  )
  public void setFingers(int fingers) {
    this.fingers = fingers;
  }

  @JsonAnyGetter
  public Map<String, Object> getAdditionalProperties() {
    return additionalProperties;
  }

  @JsonAnySetter
  public void setAdditionalProperties(String key, Object value) {
    this.additionalProperties.put(key, value);
  }
}
