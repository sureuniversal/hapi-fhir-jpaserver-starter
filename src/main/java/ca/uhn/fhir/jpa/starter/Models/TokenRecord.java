package ca.uhn.fhir.jpa.starter.Models;

import ca.uhn.fhir.jpa.starter.Util.DBUtils;

public class TokenRecord extends CacheRecord {
  final String id;
  final String token;
  final boolean is_practitioner;
  UserType type = UserType.patient;
  final long issuedDate;
  final long expiresIn;
  final String[] scopes;
  final String status;

  public TokenRecord(String id, String token, boolean is_practitioner, long issuedDate, long expiresIn, String[] scopes, String status) {
    this.id = id;
    this.token = token;
    this.is_practitioner = is_practitioner;
    this.status = status;
    if(is_practitioner)
      type = UserType.practitioner;
    this.issuedDate = issuedDate;
    this.expiresIn = expiresIn;
    this.scopes = scopes;
    this.recordTtl = System.currentTimeMillis() + DBUtils.getCacheTTL();
  }

  public String getId() {
    return id;
  }

  public String getToken() {
    return token;
  }

  public boolean is_practitioner() {
    return is_practitioner;
  }

  public long getIssuedDate() {
    return issuedDate;
  }

  public long getExpiresIn() {
    return expiresIn;
  }

  public boolean isRecordExpired(){
    return ((recordTtl - System.currentTimeMillis()) < 0);
  }

  public String[] getScopes() {
    return scopes;
  }

  public boolean isAdmin(){
    return type == UserType.superAdmin;
  }

  public UserType getType() {
    return type;
  }

  public void setType(UserType type) {
    this.type = type;
  }

  public String getStatus() {
    return status;
  }

}
