package com.intellinote.sample;

import java.io.InputStreamReader;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CreateUser {
	public static void main(String[] args) {
		String server =       "https://sandbox.intellinote.net";
		String baseUrl =      "/api/rest";
	    String clientId =     "p5eqWS1oF0n6COmn";
	    String clientSecret = "CMe09Ge9F0n6F0Jh";
	    String scope =        "read,write";
	    String redirectUri =  "/";
	    
	    String accessToken = null;	    
	    String refreshToken = null;
	    
	    // Setup the Gson parser
	    JsonParser jp = new JsonParser();
		
	    // Create the payload with Gson
	    JsonObject payload = new JsonObject();
	    payload.addProperty("client_id", clientId);
	    payload.addProperty("client_secret", clientSecret);
	    payload.addProperty("grant_type", "client_credentials");
	    
	    HttpClient httpClient = CreateUser.wrapClient(new DefaultHttpClient()); // Bypass the Keystore 
//	    HttpClient httpClient = new DefaultHttpClient(); // Use the Keystore
	    
	    try {
	    	/** Log in via client_credentials grant to obtain the access and refresh tokens. **/
	    	
	    	/*
	    	 * To perform the client_credentials "log in", we submit a POST
	    	 * request to:
	    	 *
	    	 *     {BASE-URL}/auth/oauth2/access
	    	 *
	    	 * passing the following parameters in a JSON document in the
	    	 * request body:
	    	 *
	    	 *   - our client_id (as assigned by Intellinote)
	    	 *   - our client_secret (as assigned by Intellinote)
	    	 *   - the value `"grant_type": "client_credentials"
	    	 *
	    	 * If all goes well, Intellinote will respond with a JSON document
	    	 * containing an `access_token` and a `refresh_token`.
	    	 */
	        HttpPost accessRequest = new HttpPost(server + baseUrl + "/auth/oauth2/access");
	        StringEntity accessParams = new StringEntity(payload.toString());
	        accessRequest.addHeader("content-type", "application/json"); //application/x-www-form-urlencoded"
	        accessRequest.setEntity(accessParams);
	        HttpResponse accessResponse = httpClient.execute(accessRequest);
		    if (accessResponse.getStatusLine().getStatusCode() != 200) {
				throw new RuntimeException("Access Request Failed : HTTP error code : "
				   + accessResponse.getStatusLine().getStatusCode());
			}
		    
		    // Convert to a JSON object to print data using Gson
		    JsonElement root = jp.parse(new InputStreamReader((accessResponse.getEntity().getContent())));
		    JsonObject rootObj = root.getAsJsonObject(); 
		    accessToken = rootObj.get("access_token").getAsString();
		    refreshToken = rootObj.get("refresh_token").getAsString();
		    if (accessToken == null) {
				throw new RuntimeException("Access Request Failed : Response did not contain an access token");
		    }
		    if (refreshToken == null) {
				throw new RuntimeException("Access Request Failed : Response did not contain a refresh token");
		    }
		    
		    /** Test that the access token works **/
		    
		    HttpGet testAccessRequest = new HttpGet(server + baseUrl + "/v2.0/ping/authed");
		    testAccessRequest.setHeader("Authorization", "Bearer " + accessToken);
		    HttpResponse testAccessResponse = httpClient.execute(testAccessRequest);
		    if (testAccessResponse.getStatusLine().getStatusCode() != 200) {
				throw new RuntimeException("Test Access Request Failed : HTTP error code : "
				   + testAccessResponse.getStatusLine().getStatusCode());
			}
		    // Convert to a JSON object to print data using Gson
		    root = jp.parse(new InputStreamReader((testAccessResponse.getEntity().getContent())));
		    rootObj = root.getAsJsonObject(); 
		    String accessTimestamp = rootObj.get("timestamp").getAsString();
		    if (accessTimestamp == null) {
				throw new RuntimeException("Test Access Request Failed : Response did not contain a timestamp");
		    }
		    
		    /** Create a new user **/

		    // To avoid conflicts, let's create a random email address for our new user.
		    String email = new Date().getTime() + "-" + Math.round(Math.random() * 10000) + "@example.org";

		    /*
		     * When we create the new user, we should get back:
		     *
		     *  - the `user_id` assigned to this new user
		     *
		     *  - a `refresh_token` that allows us to interact with
		     *    Intellinote on behalf of that user.
		     */
		    String userId = null;
		    String userRefreshToken = null;
		    
		    // Creating the user is just a matter of POSTing certain attributes to the `/v2.0/user` API method.
		    JsonObject user = new JsonObject();
	        user.addProperty("given_name", "Demo");
	        user.addProperty("family_name", "User");
	        user.addProperty("password", "DemoPasswd1234");
	        user.addProperty("email", email);
	        user.addProperty("job_title", "Product Demonstrator");
		    user.addProperty("tel_work", "(212) 853-5987");
		    
	        HttpPost createUserRequest = new HttpPost(server + baseUrl + "/v2.0/user");
	        StringEntity createUserParams = new StringEntity(user.toString());
	        createUserRequest.addHeader("Authorization", "Bearer " + accessToken);
	        createUserRequest.addHeader("content-type", "application/json"); //application/x-www-form-urlencoded"
	        createUserRequest.setEntity(createUserParams);
	        HttpResponse createUserResponse = httpClient.execute(createUserRequest);
		    if (createUserResponse.getStatusLine().getStatusCode() != 201) {
				throw new RuntimeException("Create User Failed : HTTP error code : "
				   + createUserResponse.getStatusLine().getStatusCode());
			}
		    // Convert to a JSON object to print data using Gson
		    root = jp.parse(new InputStreamReader((createUserResponse.getEntity().getContent())));
		    rootObj = root.getAsJsonObject(); 
		    userId = rootObj.get("user_id").getAsString();
		    userRefreshToken = rootObj.get("refresh_token").getAsString();
		    if (userId == null) {
				throw new RuntimeException("Create User Failed : Response did not contain a user id");
		    }
		    if (userRefreshToken == null) {
				throw new RuntimeException("Create User Failed : Response did not contain a refresh token");
		    }
		    
		    /** Obtain an access token for the user using the pre-authorized refresh token **/

		    // We can trade the `refresh_token` we got in the last call to obtain an `access_token` in the usual way.
		    String userAccessToken = null;
		    // Create the userPayload with Gson
		    JsonObject userPayload = new JsonObject();
		    userPayload.addProperty("client_id", clientId);
		    userPayload.addProperty("client_secret", clientSecret);
		    userPayload.addProperty("grant_type", "refresh_token");
		    userPayload.addProperty("refresh_token", userRefreshToken);
		    
		    HttpPost userAcessRequest = new HttpPost(server + baseUrl + "/auth/oauth2/access");
	        StringEntity userAcessParams = new StringEntity(userPayload.toString());
	        userAcessRequest.addHeader("Authorization", "Bearer " + accessToken);
	        userAcessRequest.addHeader("content-type", "application/json");
	        userAcessRequest.setEntity(userAcessParams);
	        HttpResponse userAcessResponse = httpClient.execute(userAcessRequest);
		    if (userAcessResponse.getStatusLine().getStatusCode() != 200) {
				throw new RuntimeException("Get User Access Failed : HTTP error code : "
				   + userAcessResponse.getStatusLine().getStatusCode());
			}
		    // Convert to a JSON object to print data using Gson
		    root = jp.parse(new InputStreamReader((userAcessResponse.getEntity().getContent())));
		    rootObj = root.getAsJsonObject(); 
		    userAccessToken = rootObj.get("access_token").getAsString();
		    if (userAccessToken == null) {
				throw new RuntimeException("Get User Access Failed : Response did not contain an access token");
		    }
		    
		    /** Hit a test method to demonstrate that the user access token works **/
		    
		    HttpGet testUserAccessRequest = new HttpGet(server + baseUrl + "/v2.0/ping/authed");
		    testUserAccessRequest.setHeader("Authorization", "Bearer " + userAccessToken);
		    HttpResponse testUserAccessResponse = httpClient.execute(testUserAccessRequest);
		    if (testUserAccessResponse.getStatusLine().getStatusCode() != 200) {
				throw new RuntimeException("Test User Access Failed : HTTP error code : "
				   + testUserAccessResponse.getStatusLine().getStatusCode());
			}
		    // Convert to a JSON object to print data using Gson
		    root = jp.parse(new InputStreamReader((testUserAccessResponse.getEntity().getContent())));
		    rootObj = root.getAsJsonObject(); 
		    String userAccessTimestamp = rootObj.get("timestamp").getAsString();
		    if (userAccessTimestamp == null) {
				throw new RuntimeException("Test User Access Failed : Response did not contain a timestamp");
		    }
		    
			/** Fetch a list of orgs (should be empty) **/
		    
		    // TODO: Pick back up here
		    
//		    S.next(function(next){
//		    	  console.log("Get a list of the orgs the user has access to (should be empty).");
//		    	  var url = "/v2.0/orgs";
//		    	  http.getJSON(url,function(err,response,body){
//		    	    /* test the response */
//		    	    assert.ok( !err );
//		    	    assert.ok( response.statusCode === 200 );
//		    	    assert.ok( Array.isArray(body) );
//		    	    assert.equal( body.length, 0 );
//		    	    /* move on */
//		    	    next();
//		    	  });
//		    	});
		    
			/** Create a new org for that user **/
		    
//		    var orgId = null;
//		    S.next(function(next) {
//		      console.log("Create a new org for that user.");
//		      var url = "/v2.0/org";
//		      var payload = {
//		        "name":email+"'s Demo Org"
//		      };
//		      http.post(url,{json:payload},function(err,response,body){
//		        /* test the response */
//		        assert.ok( !err );
//		        assert.ok( response.statusCode === 201 );
//		        assert.ok(body && body.org_id,"Expected a org_id value here, but found:" +JSON.stringify(body));
//		        /* collect the data we'll need later */
//		        orgId = body.org_id;
//		        /* move on */
//		        next();
//		      });
//		    });
		    
			/** Fetch a list of orgs (should be non-empty) **/
		    
//		    S.next(function(next){
//		    	  console.log("List the orgs again (should be non-empty).");
//		    	  var url = "/v2.0/orgs";
//		    	  http.getJSON(url,function(err,response,body){
//		    	    /* test the response */
//		    	    assert.ok( !err );
//		    	    assert.ok( response.statusCode === 200 );
//		    	    assert.ok( Array.isArray(body) );
//		    	    assert.equal( body.length, 1 );
//		    	    assert.equal( body[0].org_id, orgId );
//		    	    /* move on */
//		    	    next();
//		    	  });
//		    	});
		    
			/** Fetch a list of workspaces **/
		    
//		    var workspaceId = null
//		    		S.next(function(next){
//		    		  console.log("List the workspaces in that org.");
//		    		  var url = "/v2.0/org/"+orgId+"/workspaces";
//		    		  http.getJSON(url,function(err,response,body){
//		    		    /* test the response */
//		    		    assert.ok( !err );
//		    		    assert.ok( response.statusCode === 200 );
//		    		    assert.ok( Array.isArray(body) );
//		    		    assert.ok(body[0].workspace_id,"Expected a workspace_id value here, but found:" +JSON.stringify(body));
//		    		    /* collect the data we'll need later */
//		    		    workspaceId = body[0].workspace_id
//		    		    /* move on */
//		    		    next();
//		    		  });
//		    		});
		    
			/** Fetch a list of notes **/
		    
//		    S.next(function(next){
//		    	  console.log("List notes in that workspace.");
//		    	  var url = "/v2.0/org/"+orgId+"/workspace/"+workspaceId+"/notes";
//		    	  http.getJSON(url,function(err,response,body){
//		    	    /* test the response */
//		    	    assert.ok( !err );
//		    	    assert.ok( response.statusCode === 200 );
//		    	    assert.ok( Array.isArray(body) );
//		    	    assert.ok(body[0].note_id,"Expected a note_id value here, but found:" +JSON.stringify(body));
//		    	    /* move on */
//		    	    next();
//		    	  });
//		    	});
		    
		    
		    System.out.println("Completed successfully!");
	    }catch (Exception ex) {
		    ex.printStackTrace();
	    } finally {
	        httpClient.getConnectionManager().shutdown();
	    }
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
			ctx.init(null, new TrustManager[]{tm}, null);
			SSLSocketFactory ssf = new SSLSocketFactory(ctx);
			ssf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			ClientConnectionManager ccm = base.getConnectionManager();
			SchemeRegistry sr = ccm.getSchemeRegistry();
			sr.register(new Scheme("https", ssf, 443));
			return new DefaultHttpClient(ccm, base.getParams());
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

/*
		form: {
          refresh_token:'h4iu8zs29zy9cnmi840ub5bete3o9a4i'
          client_id:'2BBUIn2kPTr0s7YVF'
          client_secret:'Qbcfbcfdd4224'
          grant_type:'refresh_token'
        }
*/
}