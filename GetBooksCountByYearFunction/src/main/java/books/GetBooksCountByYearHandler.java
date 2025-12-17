package books;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Slf4j
public class GetBooksCountByYearHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbEnhancedClient enhancedClient;
    private final DynamoDbTable<Book> bookTable;
    private final ObjectMapper objectMapper;
    private final String tableName;

    public GetBooksCountByYearHandler() {
        this.tableName = System.getenv("TABLE_NAME");
        DynamoDbClient ddbClient = DynamoDbClient.builder().build();
        this.enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(ddbClient)
                .build();
        this.bookTable = enhancedClient.table(tableName, TableSchema.fromBean(Book.class));
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        log.info("GetBooksCountByYear function started - Request ID: {}", context != null ? context.getAwsRequestId() : "test");
        
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
            String yearParam = input.getQueryStringParameters() != null ? 
                input.getQueryStringParameters().get("year") : null;
                
            if (yearParam == null || yearParam.trim().isEmpty()) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withHeaders(headers)
                        .withBody("{\"error\": \"Year parameter is required\"}");
            }
            
            int year = Integer.parseInt(yearParam);
            log.info("Counting books read in year: {}", year);
            
            long count = bookTable.scan()
                    .items()
                    .stream()
                    .filter(book -> book.getReadYear() == year)
                    .count();
            
            log.info("Found {} books read in year: {}", count, year);
            
            Map<String, Object> response = new HashMap<>();
            response.put("year", year);
            response.put("count", count);
            
            String jsonResponse = objectMapper.writeValueAsString(response);
            
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(headers)
                    .withBody(jsonResponse);
                    
        } catch (NumberFormatException e) {
            log.error("Invalid year format", e);
            
            String errorResponse = "{\"error\": \"Invalid year format\", \"message\": \"Year must be a valid integer\"}";
            
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withHeaders(headers)
                    .withBody(errorResponse);
        } catch (Exception e) {
            log.error("Error counting books by year", e);
            
            String errorResponse = "{\"error\": \"Failed to count books\", \"message\": \"" + e.getMessage() + "\"}";
            
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withHeaders(headers)
                    .withBody(errorResponse);
        }
    }
}