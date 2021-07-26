package ca.uhn.fhir.jpa.starter.interactor;

import ca.uhn.fhir.jpa.starter.Models.TokenRecord;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;

public class DBInteractorLoopback implements IDBInteractor{

  private static class LoopbackUserInfo{
    public final String userId;
    public final Boolean isPractitioner;
    public final String status;


    private LoopbackUserInfo(@JsonProperty("userId")String userId,
                             @JsonProperty("isPractitioner")Boolean isPractitioner,
                             @JsonProperty("status")String status) {
      this.userId = userId;
      this.isPractitioner = isPractitioner;
      this.status = status;
    }
  }

  private final String loopbackUrl;
  private static final CloseableHttpClient client = HttpClients.createMinimal();
  public DBInteractorLoopback(String loopbackUrl) {
    this.loopbackUrl = loopbackUrl;
  }

  @Override
  public TokenRecord getTokenRecord(String token) {
    try {
      HttpGet request = new HttpGet(loopbackUrl+"getUserInfoByAccessToken?access_token="+token);

      HttpResponse httpResponse = client.execute(request);
      if(httpResponse.getStatusLine().getStatusCode() == 401){
        byte [] buff = new byte[300];
        httpResponse.getEntity().getContent().read(buff,0,300);
        return new TokenRecord(null,null,false,0,0,null,new String(buff).trim());
      }
      LoopbackUserInfo response = new ObjectMapper().readValue(httpResponse.getEntity().getContent(), LoopbackUserInfo.class);

      return new TokenRecord(response.userId, token, response.isPractitioner, 0,0,null, response.status);
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }
}
