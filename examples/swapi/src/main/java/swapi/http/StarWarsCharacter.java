package swapi.http;

import java.time.LocalDate;
import java.util.StringJoiner;

public class StarWarsCharacter {

  private String name;
  private String birth_year; // e.g., "19BBY", "33ABY"
  private String eye_color;
  private String gender;
  private String hair_color;
  private String height; // Stored as String as units (cm) are part of the description
  private String mass; // Stored as String as units (kg) are part of the description
  private String homeworld; // URL of a planet resource
  private LocalDate created; // ISO 8601 date format string
  private LocalDate edited; // ISO 8601 date format string

  public StarWarsCharacter() {}

  public String getName() {
    return name;
  }

  public StarWarsCharacter setName(String name) {
    this.name = name;
    return this;
  }

  public String getBirth_year() {
    return birth_year;
  }

  public StarWarsCharacter setBirth_year(String birth_year) {
    this.birth_year = birth_year;
    return this;
  }

  public String getEye_color() {
    return eye_color;
  }

  public StarWarsCharacter setEye_color(String eye_color) {
    this.eye_color = eye_color;
    return this;
  }

  public String getGender() {
    return gender;
  }

  public StarWarsCharacter setGender(String gender) {
    this.gender = gender;
    return this;
  }

  public String getHair_color() {
    return hair_color;
  }

  public StarWarsCharacter setHair_color(String hair_color) {
    this.hair_color = hair_color;
    return this;
  }

  public String getHeight() {
    return height;
  }

  public StarWarsCharacter setHeight(String height) {
    this.height = height;
    return this;
  }

  public String getMass() {
    return mass;
  }

  public StarWarsCharacter setMass(String mass) {
    this.mass = mass;
    return this;
  }

  public String getHomeworld() {
    return homeworld;
  }

  public StarWarsCharacter setHomeworld(String homeworld) {
    this.homeworld = homeworld;
    return this;
  }

  public LocalDate getCreated() {
    return created;
  }

  public StarWarsCharacter setCreated(LocalDate created) {
    this.created = created;
    return this;
  }

  public LocalDate getEdited() {
    return edited;
  }

  public StarWarsCharacter setEdited(LocalDate edited) {
    this.edited = edited;
    return this;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", StarWarsCharacter.class.getSimpleName() + "[", "]")
        .add("name='" + name + "'")
        .add("birth year='" + birth_year + "'")
        .add("eye color='" + eye_color + "'")
        .add("gender='" + gender + "'")
        .add("hair color='" + hair_color + "'")
        .add("height='" + height + "'")
        .add("mass='" + mass + "'")
        .toString();
  }
}
