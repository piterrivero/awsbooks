package books;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Slf4j
public class GetBooksCountHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbEnhancedClient enhancedClient;
    private final DynamoDbTable<Book> bookTable;
    private final String tableName;

    public GetBooksCountHandler() {
        this.tableName = System.getenv("TABLE_NAME");
        DynamoDbClient ddbClient = DynamoDbClient.builder().build();
        this.enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(ddbClient)
                .build();
        this.bookTable = enhancedClient.table(tableName, TableSchema.fromBean(Book.class));
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        log.info("GetBooksCount function started - Request ID: {}", context != null ? context.getAwsRequestId() : "test");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type, Authorization");
        
        // Handle OPTIONS request for CORS preflight
        if ("OPTIONS".equals(input.getHttpMethod())) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(headers)
                    .withBody("");
        }

        try {
            log.info("Counting books in table: {}", tableName);
            
            long count = bookTable.scan().items().stream().count();
            
            log.info("Total books count: {}", count);
            
            String response = "{\"count\": " + count + "}";
            
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(headers)
                    .withBody(response);
                    
        } catch (Exception e) {
            log.error("Error counting books", e);
            
            String errorResponse = "{\"error\": \"Failed to count books\", \"message\": \"" + e.getMessage() + "\"}";
            
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withHeaders(headers)
                    .withBody(errorResponse);
        }
    }
}