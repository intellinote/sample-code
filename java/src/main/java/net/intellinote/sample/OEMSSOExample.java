package net.intellinote.sample;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.Random;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.util.EntityUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

public class OEMSSOExample {

  public static void main(String[] args) throws ClientProtocolException, IOException {
    String[] pair = RESTClient.execute();
    String email = pair[0];
    int workspaceId = Integer.parseInt(pair[1]);
    String url = SSOURLGenerator.generateURL(email, workspaceId);
    System.out.println(url);
    System.exit(0);
  }

}

class RESTClient {
  static String     restServer       = "https://sandbox.intellinote.net";
  static String     restBasePath     = "/api/rest";
  static String     restClientId     = "CLIENT-ID-GOES-HERE";  // TODO change this value
  static String     restClientSecret = "CLIENT-SECRET-GOES-HERE"; // TODO change this value
  static String     workspaceId      = null;
  static JsonParser jp               = new JsonParser();
  static HttpClient httpClient       = RESTClient.wrapClient(new DefaultHttpClient());

  public static String[] execute() throws ClientProtocolException, IOException {
    String appAccessToken = RESTClient.loginAsApp();
    String user1email = "sso-test-" + new Date().getTime() % 10000 + "-" + Math.round(Math.random() * 10000) + "@example.org";
    String[] user1pair = RESTClient.createUser(user1email, "Admin", "SSODemo", "Password1234", appAccessToken);
    String user2email = "2nd-user-" + new Date().getTime() % 10000 + "-" + Math.round(Math.random() * 10000) + "@example.org";
    String[] user2pair = RESTClient.createUser(user2email, "User", "SSODemo", "Password4567", appAccessToken);
    String user1AccessToken = RESTClient.getAccessTokenForRefreshToken(user1pair[1]);
    String orgId = RESTClient.createOrg("SSO Demo Org", user1AccessToken);
    String workspaceId = RESTClient.createWorkspace("The Shared Workspace", orgId, user1AccessToken);
    RESTClient.addUserToOrg(orgId, user2pair[0], "FULL", user1AccessToken);
    RESTClient.addUserToWorkspace(orgId, workspaceId, user2pair[0], "FULL", user1AccessToken);
    String[] result = new String[2];
    result[0] = user2email;
    result[1] = workspaceId;
    return result;
  }

  public static String loginAsApp() throws ClientProtocolException, IOException {
    /*********************************************************************************/
    System.out.println("Logging-in for userless, application-level API access.");
    /*********************************************************************************/
    String accessToken = null;
    JsonObject payload = new JsonObject();
    payload.addProperty("client_id", RESTClient.restClientId);
    payload.addProperty("client_secret", RESTClient.restClientSecret);
    payload.addProperty("grant_type", "client_credentials");
    HttpPost req = new HttpPost(RESTClient.restServer + RESTClient.restBasePath + "/auth/oauth2/access");
    StringEntity params = new StringEntity(payload.toString());
    req.addHeader("content-type", "application/json");
    req.setEntity(params);
    HttpResponse res = httpClient.execute(req);
    if (res.getStatusLine().getStatusCode() != 200) {
      System.err.println("Access Request Failed with HTTP status code " + res.getStatusLine().getStatusCode() + ".");
      System.exit(1);
    } else {
      JsonElement root = jp.parse(new InputStreamReader((res.getEntity().getContent())));
      JsonObject rootObj = root.getAsJsonObject();
      accessToken = rootObj.get("access_token").getAsString();
      if (accessToken == null) {
        System.err.println("Access Request Failed. Expected an access_token value but found " + rootObj.toString() + ".");
        System.exit(1);
      }
    }
    /*********************************************************************************/
    System.out.println("Authenticated with application-level API access.");
    /*********************************************************************************/
    return accessToken;
  }

  public static String[] createUser(String email, String fname, String lname, String passwd, String accessToken) throws ClientProtocolException, IOException {
    /*********************************************************************************/
    System.out.println("Creating user " + email + ".");
    /*********************************************************************************/
    String userId = null;
    String refreshToken = null;
    JsonObject payload = new JsonObject();
    payload.addProperty("given_name", fname);
    payload.addProperty("family_name", lname);
    payload.addProperty("password", passwd);
    payload.addProperty("email", email);
    HttpPost req = new HttpPost(RESTClient.restServer + RESTClient.restBasePath + "/v2.0/user");
    StringEntity params = new StringEntity(payload.toString());
    req.addHeader("Authorization", "Bearer " + accessToken);
    req.addHeader("Content-Type", "application/json");
    req.setEntity(params);
    HttpResponse res = httpClient.execute(req);
    if (res.getStatusLine().getStatusCode() != 201) {
      System.err.println("Create User Request Failed with HTTP status code " + res.getStatusLine().getStatusCode() + ".");
      System.exit(1);
    } else {
      JsonElement root = jp.parse(new InputStreamReader((res.getEntity().getContent())));
      JsonObject rootObj = root.getAsJsonObject();
      userId = rootObj.get("user_id").getAsString();
      if (userId == null) {
        System.err.println("Create User Request Failed. Expected a user_id value but found " + rootObj.toString() + ".");
        System.exit(1);
      }
      refreshToken = rootObj.get("refresh_token").getAsString();
      if (refreshToken == null) {
        System.err.println("Create User Request Failed. Expected a refresh_token value but found " + rootObj.toString() + ".");
        System.exit(1);
      }
    }
    /*********************************************************************************/
    System.out.println("Created user " + email + " with user_id " + userId + " and refresh_token " + refreshToken + ".");
    /*********************************************************************************/
    String[] result = new String[2];
    result[0] = userId;
    result[1] = refreshToken;
    return result;
  }

  public static String getAccessTokenForRefreshToken(String refreshToken) throws ClientProtocolException, IOException {
    /*********************************************************************************/
    System.out.println("Refreshing access token using refresh token value " + refreshToken + ".");
    /*********************************************************************************/
    String accessToken = null;
    JsonObject payload = new JsonObject();
    payload.addProperty("client_id", RESTClient.restClientId);
    payload.addProperty("client_secret", RESTClient.restClientSecret);
    payload.addProperty("refresh_token", refreshToken);
    payload.addProperty("grant_type", "refresh_token");
    HttpPost req = new HttpPost(RESTClient.restServer + RESTClient.restBasePath + "/auth/oauth2/access");
    StringEntity params = new StringEntity(payload.toString());
    req.addHeader("Content-Type", "application/json");
    req.setEntity(params);
    HttpResponse res = httpClient.execute(req);
    if (res.getStatusLine().getStatusCode() != 200) {
      System.err.println("Access Request Failed with HTTP status code " + res.getStatusLine().getStatusCode() + ".");
      System.exit(1);
    } else {
      JsonElement root = jp.parse(new InputStreamReader((res.getEntity().getContent())));
      JsonObject rootObj = root.getAsJsonObject();
      accessToken = rootObj.get("access_token").getAsString();
      if (accessToken == null) {
        System.err.println("Access Request Failed. Expected an access_token value but found " + rootObj.toString() + ".");
        System.exit(1);
      }
    }
    /*********************************************************************************/
    System.out.println("Obtained refreshed access token " + accessToken + " from refresh token " + refreshToken + ".");
    /*********************************************************************************/
    return accessToken;
  }

  public static String createOrg(String name, String accessToken) throws ClientProtocolException, IOException {
    /*********************************************************************************/
    System.out.println("Creating org " + name + ".");
    /*********************************************************************************/
    String orgId = null;
    JsonObject payload = new JsonObject();
    payload.addProperty("name", name);
    HttpPost req = new HttpPost(RESTClient.restServer + RESTClient.restBasePath + "/v2.0/org");
    StringEntity params = new StringEntity(payload.toString());
    req.addHeader("Authorization", "Bearer " + accessToken);
    req.addHeader("Content-Type", "application/json");
    req.setEntity(params);
    HttpResponse res = httpClient.execute(req);
    if (res.getStatusLine().getStatusCode() != 201) {
      System.err.println("Create Org Request Failed with HTTP status code " + res.getStatusLine().getStatusCode() + ".");
      System.exit(1);
    } else {
      JsonElement root = jp.parse(new InputStreamReader((res.getEntity().getContent())));
      JsonObject rootObj = root.getAsJsonObject();
      orgId = rootObj.get("org_id").getAsString();
      if (orgId == null) {
        System.err.println("Create Org Request Failed. Expected a org_id value but found " + rootObj.toString() + ".");
        System.exit(1);
      }
    }
    /*********************************************************************************/
    System.out.println("Created org " + name + " with org_id " + orgId + ".");
    /*********************************************************************************/
    return orgId;
  }

  public static String createWorkspace(String name, String orgId, String accessToken) throws ClientProtocolException, IOException {
    /*********************************************************************************/
    System.out.println("Creating workspace " + name + " in org " + orgId + ".");
    /*********************************************************************************/
    String workspaceId = null;
    JsonObject payload = new JsonObject();
    payload.addProperty("name", name);
    HttpPost req = new HttpPost(RESTClient.restServer + RESTClient.restBasePath + "/v2.0/org/" + orgId + "/workspace");
    StringEntity params = new StringEntity(payload.toString());
    req.addHeader("Authorization", "Bearer " + accessToken);
    req.addHeader("Content-Type", "application/json");
    req.setEntity(params);
    HttpResponse res = httpClient.execute(req);
    if (res.getStatusLine().getStatusCode() != 201) {
      System.out.println(jp.parse(new InputStreamReader((res.getEntity().getContent()))));

      System.err.println("Create Workspace Request Failed with HTTP status code " + res.getStatusLine().getStatusCode() + ".");
      System.exit(1);
    } else {
      JsonElement root = jp.parse(new InputStreamReader((res.getEntity().getContent())));
      JsonObject rootObj = root.getAsJsonObject();
      workspaceId = rootObj.get("workspace_id").getAsString();
      if (workspaceId == null) {
        System.err.println("Create Workspace Request Failed. Expected a workspace_id value but found " + rootObj.toString() + ".");
        System.exit(1);
      }
    }
    /*********************************************************************************/
    System.out.println("Created workspace " + name + " with workspace_id " + workspaceId + ".");
    /*********************************************************************************/
    return workspaceId;
  }

  public static void addUserToOrg(String orgId, String userId, String role, String accessToken) throws ClientProtocolException, IOException {
    /*********************************************************************************/
    System.out.println("Adding user " + userId + " to org " + orgId + ".");
    /*********************************************************************************/
    JsonObject payload = new JsonObject();
    payload.addProperty("access_type", role);
    HttpPost req = new HttpPost(RESTClient.restServer + RESTClient.restBasePath + "/v2.0/org/" + orgId + "/member/" + userId);
    StringEntity params = new StringEntity(payload.toString());
    req.addHeader("Authorization", "Bearer " + accessToken);
    req.addHeader("Content-Type", "application/json");
    req.setEntity(params);
    HttpResponse res = httpClient.execute(req);
    if (res.getStatusLine().getStatusCode() < 200 || res.getStatusLine().getStatusCode() > 299) {
      System.err.println("Add user to org request Failed with HTTP status code " + res.getStatusLine().getStatusCode() + ".");
      System.exit(1);
    } else {
      EntityUtils.consumeQuietly(res.getEntity());
    }
    /*********************************************************************************/
    System.out.println("Added user " + userId + " to org " + orgId + ".");
    /*********************************************************************************/
  }

  public static void addUserToWorkspace(String orgId, String workspaceId, String userId, String role, String accessToken) throws ClientProtocolException, IOException {
    /*********************************************************************************/
    System.out.println("Adding user " + userId + " to workspace " + workspaceId + ".");
    /*********************************************************************************/
    JsonObject payload = new JsonObject();
    payload.addProperty("access_type", role);
    HttpPost req = new HttpPost(RESTClient.restServer + RESTClient.restBasePath + "/v2.0/org/" + orgId + "/workspace/" + workspaceId + "/member/" + userId);
    StringEntity params = new StringEntity(payload.toString());
    req.addHeader("Authorization", "Bearer " + accessToken);
    req.addHeader("Content-Type", "application/json");
    req.setEntity(params);
    HttpResponse res = httpClient.execute(req);
    if (res.getStatusLine().getStatusCode() < 200 || res.getStatusLine().getStatusCode() > 299) {
      System.err.println("Add user to workspace request Failed with HTTP status code " + res.getStatusLine().getStatusCode() + ".");
      System.exit(1);
    } else {
      EntityUtils.consumeQuietly(res.getEntity());
    }
    /*********************************************************************************/
    System.out.println("Added user " + userId + " to workspace " + workspaceId + ".");
    /*********************************************************************************/
  }

  public static HttpClient wrapClient(HttpClient base) {
    try {
      SSLContext ctx = SSLContext.getInstance("TLS");
      X509TrustManager tm = new X509TrustManager() {
        public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
        }

        public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
        }

        public X509Certificate[] getAcceptedIssuers() {
          return null;
        }
      };
      ClientConnectionManager ccm = base.getConnectionManager();
      ctx.init(null, new TrustManager[] { tm }, null);
      SSLSocketFactory ssf = new SSLSocketFactory(ctx);
      ssf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
      SchemeRegistry sr = ccm.getSchemeRegistry();
      sr.register(new Scheme("https", ssf, 443));
      return new DefaultHttpClient(ccm, base.getParams());
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }

}

/** This class generates a signed HMAC-SSO authentication request based. */
class SSOURLGenerator {
  static String  ssoClientId     = "SSO-CLIENT-ID-GOES-HERE"; // TODO: change this value
  static String  ssoSharedSecret = "SSO-SHARED-SECRET-GOES-HERE"; // TODO: change this value
  static int     ssoKeyNumber    = 123; // TODO: change this value
  static String  baseURL         = "https://sandbox.intellinote.net/api/rest/sso/validate/hmac";
  static String  onSuccess       = "https://wsandbox.intellinote.net/web/#note";
  static boolean setCookies      = true;
  static String  onFailure       = "http://127.0.0.1/?authentication_failed";
  static boolean addWorkspace    = true;

  static Random  random          = new Random();

  public static String getISO8601Time() {
    return String.format("%tFT%<tRZ", Calendar.getInstance(TimeZone.getTimeZone("Z")));
  }

  public static String generateURL(String userId, int workspaceId) {

    /*********************************************************************************/
    System.out.println("Generating SSO Payload");
    /*********************************************************************************/
    int r = SSOURLGenerator.random.nextInt();
    String ts = SSOURLGenerator.getISO8601Time();
    String payload = "a=login";
    payload += "&c=" + SSOURLGenerator.ssoClientId;
    payload += "&n=" + SSOURLGenerator.ssoKeyNumber;
    payload += "&r=" + r;
    payload += "&t=" + ts;
    payload += "&u=" + userId;
    payload += "&v=100";
    /*********************************************************************************/
    System.out.println("Generated SSO Payload: " + payload);
    /*********************************************************************************/

    /*********************************************************************************/
    System.out.println("Calculating signature for SSO payload.");
    /*********************************************************************************/
    String signature = null;
    try {
      Mac hmac = Mac.getInstance("HmacSHA512");
      SecretKeySpec key = new SecretKeySpec(SSOURLGenerator.ssoSharedSecret.getBytes("UTF-8"), "HmacSHA512");
      hmac.init(key);
      byte[] digest = hmac.doFinal(payload.getBytes("UTF-8"));
      signature = Base64.encodeBase64String(digest);
    } catch (Exception e) {
      System.err.println("Error computing signature: " + e);
      System.exit(1);
    }
    /*********************************************************************************/
    System.out.println("Calculated signature: " + signature);
    /*********************************************************************************/

    /*********************************************************************************/
    System.out.println("Generating query string.");
    /*********************************************************************************/
    String qs = null;
    try {
      qs = "a=login";
      qs += "&c=" + SSOURLGenerator.ssoClientId;
      qs += "&n=" + SSOURLGenerator.ssoKeyNumber;
      qs += "&r=" + r;
      qs += "&t=" + ts;
      qs += "&u=" + URLEncoder.encode(userId, "UTF-8");
      qs += "&v=100";
      qs += "&s=" + URLEncoder.encode(signature, "UTF-8");
    } catch (Exception e) {
      System.err.println("Error computing query string: " + e);
      System.exit(1);
    }
    /*********************************************************************************/
    System.out.println("Generated query string: " + qs);
    /*********************************************************************************/

    /*********************************************************************************/
    System.out.println("Assembling final SSO URL.");
    /*********************************************************************************/
    String url = SSOURLGenerator.baseURL;
    // core qs
    url = url + "?" + qs;
    // set_cookies
    if (SSOURLGenerator.setCookies) {
      url = url + "&set_cookies=true";
    } else {
      url = url + "&set_cookies=false";
    }
    // on_success
    String successURL = SSOURLGenerator.onSuccess;
    if (SSOURLGenerator.addWorkspace && workspaceId > 0) {
      /*********************************************************************************/
      System.out.println("Calculating Base62 representation of workspace id " + workspaceId + ".");
      /*********************************************************************************/
      String b62str = Base62Encoder.encode(workspaceId);
      successURL = successURL + "/p=" + b62str + "&nb=Notes";
      /*********************************************************************************/
      System.out.println("Added " + b62str + ", the Base62 representation of workspace id " + workspaceId + ", to the on_success URL.");
      /*********************************************************************************/
    }
    try {
      url = url + "&on_success=" + URLEncoder.encode(successURL, "UTF-8");
    } catch (Exception e) {
      System.err.println("Error encoding on_success parameter " + e);
      System.exit(1);
    }
    /*********************************************************************************/
    // on_failure
    try {
      url = url + "&on_failure=" + URLEncoder.encode(SSOURLGenerator.onFailure, "UTF-8");
    } catch (Exception e) {
      System.err.println("Error encoding on_failure parameter " + e);
      System.exit(1);
    }
    /*********************************************************************************/
    System.out.println("Final SSO URL:\n\t" + url);
    /*********************************************************************************/

    return url;

  }
}

/** This utility class converts an integer into the Base62 encoding. */
class Base62Encoder {
  static String[] BASE_62_DIGITS = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z" };

  public static String encode(int value) {
    if (value == 0) {
      return "0";
    } else {
      String s = "";
      while (value > 0) {
        s = Base62Encoder.BASE_62_DIGITS[value % 62] + s;
        value = (int) (Math.floor((double) value / 62));
      }
      return s;
    }
  }
}
