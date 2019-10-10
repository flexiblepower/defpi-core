package org.flexiblepower.raml.example.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Date;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("person")
@JsonPropertyOrder({
    "humanType",
    "limbs",
    "timeOfArrival",
    "timeOfBirth",
    "dateOfBirth",
    "instantOfBirth",
    "requestTime",
    "actualGender",
    "name",
    "weight",
    "siblings"
})
public class PersonImpl implements Person {
  @JsonProperty("humanType")
  private final String humanType = _DISCRIMINATOR_TYPE_NAME;

  @JsonProperty("limbs")
  private List<Limb> limbs;

  @JsonProperty("timeOfArrival")
  @JsonFormat(
      shape = JsonFormat.Shape.STRING,
      pattern = "yyyy-MM-dd'T'HH:mm:ss"
  )
  private Date timeOfArrival;

  @JsonFormat(
      shape = JsonFormat.Shape.STRING,
      pattern = "HH:mm:ss"
  )
  @JsonProperty("timeOfBirth")
  private Date timeOfBirth;

  @JsonFormat(
      shape = JsonFormat.Shape.STRING,
      pattern = "yyyy-MM-dd"
  )
  @JsonProperty("dateOfBirth")
  private Date dateOfBirth;

  @JsonProperty("instantOfBirth")
  @JsonFormat(
      shape = JsonFormat.Shape.STRING,
      pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
  )
  @JsonDeserialize(
      using = TimestampDeserializer.class
  )
  private Date instantOfBirth;

  @JsonProperty("requestTime")
  @JsonFormat(
      shape = JsonFormat.Shape.STRING,
      pattern = "EEE, dd MMM yyyy HH:mm:ss z"
  )
  private Date requestTime;

  @JsonProperty("actualGender")
  private Gender actualGender;

  @JsonProperty("name")
  private String name;

  @JsonProperty("weight")
  private int weight;

  @JsonProperty("siblings")
  private List<Human> siblings;

  @JsonIgnore
  private Map<String, Object> additionalProperties = new ExcludingMap();

  @JsonProperty("humanType")
  public String getHumanType() {
    return this.humanType;
  }

  @JsonProperty("limbs")
  public List<Limb> getLimbs() {
    return this.limbs;
  }

  @JsonProperty("limbs")
  public void setLimbs(List<Limb> limbs) {
    this.limbs = limbs;
  }

  @JsonProperty("timeOfArrival")
  public Date getTimeOfArrival() {
    return this.timeOfArrival;
  }

  @JsonProperty("timeOfArrival")
  public void setTimeOfArrival(Date timeOfArrival) {
    this.timeOfArrival = timeOfArrival;
  }

  @JsonProperty("timeOfBirth")
  public Date getTimeOfBirth() {
    return this.timeOfBirth;
  }

  @JsonProperty("timeOfBirth")
  public void setTimeOfBirth(Date timeOfBirth) {
    this.timeOfBirth = timeOfBirth;
  }

  @JsonProperty("dateOfBirth")
  public Date getDateOfBirth() {
    return this.dateOfBirth;
  }

  @JsonProperty("dateOfBirth")
  public void setDateOfBirth(Date dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
  }

  @JsonProperty("instantOfBirth")
  public Date getInstantOfBirth() {
    return this.instantOfBirth;
  }

  @JsonProperty("instantOfBirth")
  public void setInstantOfBirth(Date instantOfBirth) {
    this.instantOfBirth = instantOfBirth;
  }

  @JsonProperty("requestTime")
  public Date getRequestTime() {
    return this.requestTime;
  }

  @JsonProperty("requestTime")
  public void setRequestTime(Date requestTime) {
    this.requestTime = requestTime;
  }

  @JsonProperty("actualGender")
  public Gender getActualGender() {
    return this.actualGender;
  }

  @JsonProperty("actualGender")
  public void setActualGender(Gender actualGender) {
    this.actualGender = actualGender;
  }

  @JsonProperty("name")
  public String getName() {
    return this.name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  @JsonProperty("weight")
  public int getWeight() {
    return this.weight;
  }

  @JsonProperty("weight")
  public void setWeight(int weight) {
    this.weight = weight;
  }

  @JsonProperty("siblings")
  public List<Human> getSiblings() {
    return this.siblings;
  }

  @JsonProperty("siblings")
  public void setSiblings(List<Human> siblings) {
    this.siblings = siblings;
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
