package auth;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;

@Slf4j
public class LoginHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final CognitoIdentityProviderClient cognitoClient;
    private final ObjectMapper objectMapper;
    private final String userPoolId;
    private final String clientId;

    public LoginHandler() {
        this.cognitoClient = CognitoIdentityProviderClient.builder().build();
        this.objectMapper = new ObjectMapper();
        this.userPoolId = System.getenv("USER_POOL_ID");
        this.clientId = System.getenv("USER_POOL_CLIENT_ID");
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        log.info("Login function started - Request ID: {}", context.getAwsRequestId());
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");

        try {
            // Parse request body
            JsonNode requestBody = objectMapper.readTree(input.getBody());
            String email = requestBody.get("email").asText();
            String password = requestBody.get("password").asText();
            
            log.info("Authenticating user: {}", email);

            // Authenticate with Cognito
            Map<String, String> authParams = new HashMap<>();
            authParams.put("USERNAME", email);
            authParams.put("PASSWORD", password);

            AdminInitiateAuthRequest authRequest = AdminInitiateAuthRequest.builder()
                    .userPoolId(userPoolId)
                    .clientId(clientId)
                    .authFlow(AuthFlowType.ADMIN_NO_SRP_AUTH)
                    .authParameters(authParams)
                    .build();

            AdminInitiateAuthResponse authResponse = cognitoClient.adminInitiateAuth(authRequest);

            String accessToken = authResponse.authenticationResult().accessToken();
            String idToken = authResponse.authenticationResult().idToken();
            
            log.info("User authenticated successfully: {}", email);

            String response = String.format(
                "{\"message\": \"Login successful\", \"accessToken\": \"%s\", \"idToken\": \"%s\"}", 
                accessToken, idToken
            );
            
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(headers)
                    .withBody(response);
                    
        } catch (Exception e) {
            log.error("Error during login", e);
            
            String errorResponse = "{\"error\": \"Login failed\", \"message\": \"Invalid credentials\"}";
            
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(401)
                    .withHeaders(headers)
                    .withBody(errorResponse);
        }
    }
}