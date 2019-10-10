package org.flexiblepower.raml.example.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Date;
import java.util.List;
import java.util.Map;

@JsonTypeName("person")
@JsonDeserialize(
    as = PersonImpl.class
)
public interface Person extends Human {
  String _DISCRIMINATOR_TYPE_NAME = "person";

  @JsonAnyGetter
  Map<String, Object> getAdditionalProperties();

  @JsonAnySetter
  void setAdditionalProperties(String key, Object value);

  @JsonProperty("humanType")
  String getHumanType();

  @JsonProperty("limbs")
  List<Limb> getLimbs();

  @JsonProperty("limbs")
  void setLimbs(List<Limb> limbs);

  @JsonProperty("timeOfArrival")
  Date getTimeOfArrival();

  @JsonProperty("timeOfArrival")
  void setTimeOfArrival(Date timeOfArrival);

  @JsonProperty("timeOfBirth")
  Date getTimeOfBirth();

  @JsonProperty("timeOfBirth")
  void setTimeOfBirth(Date timeOfBirth);

  @JsonProperty("dateOfBirth")
  Date getDateOfBirth();

  @JsonProperty("dateOfBirth")
  void setDateOfBirth(Date dateOfBirth);

  @JsonProperty("instantOfBirth")
  Date getInstantOfBirth();

  @JsonProperty("instantOfBirth")
  void setInstantOfBirth(Date instantOfBirth);

  @JsonProperty("requestTime")
  Date getRequestTime();

  @JsonProperty("requestTime")
  void setRequestTime(Date requestTime);

  @JsonProperty("actualGender")
  Gender getActualGender();

  @JsonProperty("actualGender")
  void setActualGender(Gender actualGender);

  @JsonProperty("name")
  String getName();

  @JsonProperty("name")
  void setName(String name);

  @JsonProperty("weight")
  int getWeight();

  @JsonProperty("weight")
  void setWeight(int weight);

  @JsonProperty("siblings")
  List<Human> getSiblings();

  @JsonProperty("siblings")
  void setSiblings(List<Human> siblings);
}
