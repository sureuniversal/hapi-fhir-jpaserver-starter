package ca.uhn.fhir.jpa.starter.interactor;

import ca.uhn.fhir.jpa.starter.Models.TokenRecord;

import java.sql.*;

public class DBInteractorPostgres implements IDBInteractor {

  private final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(DBInteractorPostgres.class);

  private Connection postgresCon;
  private final Connector connector;
  private class Connector {
    public String connectionString;
    public String postgresUser;
    public String postgresPass;

    public Connector(String connectionString, String postgresUser, String postgresPass) {
      this.connectionString = connectionString;
      this.postgresUser = postgresUser;
      this.postgresPass = postgresPass;
    }

    public void connect(){
      try {
        Class.forName("org.postgresql.Driver");
        postgresCon = DriverManager.getConnection(connectionString, postgresUser, postgresPass);
      } catch (SQLException | ClassNotFoundException e) {
        e.printStackTrace();
      }
    }

  }

  public DBInteractorPostgres(String connectionString, String postgresUser, String postgresPass) {
    connector = new Connector(connectionString,postgresUser,postgresPass);
    connector.connect();
  }

  @Override
  public TokenRecord getTokenRecord(String token) {
    try {
      if(postgresCon.isClosed()){
        connector.connect();
      }
      PreparedStatement postgresStm = postgresCon.prepareStatement(
        "select u.id, u.ispractitioner, u.status, o.accesstoken, o.issuedat, o.expiresin, o.scopes " +
          "from public.oauthaccesstoken o " +
          "join public.user u on o.uid = u.id " +
          "where o.accesstoken = '" + token + "';"
        );

      ResultSet resultSet = postgresStm.executeQuery();
      if (!resultSet.next()) return null;
      String userId = resultSet.getString("id");
      boolean isPractitioner = resultSet.getBoolean("ispractitioner");
      long issued = -1;
      long expire = -1;
     // String[] scopes = resultSet.getString("scopes").replaceAll("[\\]\\[\"]","").split(",");
      String status = resultSet.getString("status");
      postgresStm.close();
      return new TokenRecord(userId, token, isPractitioner, issued, expire, null, status);
    } catch (SQLException e) {
      ourLog.error("postgreSQL error:", e);
      if(e.getCause().getClass() == java.net.SocketException.class){
        return getTokenRecord(token);
      }
      return null;
    }
  }
}
