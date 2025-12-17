package books;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
public class SearchBooksByAuthorHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbEnhancedClient enhancedClient;
    private final DynamoDbTable<Book> bookTable;
    private final ObjectMapper objectMapper;
    private final String tableName;

    public SearchBooksByAuthorHandler() {
        this.tableName = System.getenv("TABLE_NAME");
        DynamoDbClient ddbClient = DynamoDbClient.builder().build();
        this.enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(ddbClient)
                .build();
        this.bookTable = enhancedClient.table(tableName, TableSchema.fromBean(Book.class));
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
        this.objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        log.info("SearchBooksByAuthor function started - Request ID: {}", context != null ? context.getAwsRequestId() : "test");
        
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
            String authorQuery = input.getQueryStringParameters() != null ? 
                input.getQueryStringParameters().get("author") : null;
                
            if (authorQuery == null || authorQuery.trim().isEmpty()) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withHeaders(headers)
                        .withBody("{\"error\": \"Author parameter is required\"}");
            }
            
            log.info("Searching books by author: {}", authorQuery);
            
            List<BookResponse> books = bookTable.scan()
                    .items()
                    .stream()
                    .filter(book -> book.getAuthor() != null && 
                           book.getAuthor().toLowerCase().contains(authorQuery.toLowerCase()))
                    .map(this::convertToBookResponse)
                    .sorted((b1, b2) -> b1.getTitle().compareToIgnoreCase(b2.getTitle()))
                    .collect(Collectors.toList());
            
            log.info("Found {} books by author: {}", books.size(), authorQuery);
            
            String jsonResponse = objectMapper.writeValueAsString(books);
            
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(headers)
                    .withBody(jsonResponse);
                    
        } catch (Exception e) {
            log.error("Error searching books by author", e);
            
            String errorResponse = "{\"error\": \"Failed to search books\", \"message\": \"" + e.getMessage() + "\"}";
            
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withHeaders(headers)
                    .withBody(errorResponse);
        }
    }
    
    private BookResponse convertToBookResponse(Book book) {
        BookResponse response = new BookResponse();
        response.setId(book.getId());
        response.setTitle(book.getTitle());
        response.setAuthor(book.getAuthor());
        response.setPublicationYear(book.getPublicationYear());
        response.setLanguage(book.getLanguage());
        response.setFormat(book.getFormat());
        response.setFinishDate(book.getFinishDate() != null ? LocalDate.parse(book.getFinishDate()) : null);
        response.setReadYear(book.getReadYear());
        response.setReadingTimeInDays(book.getReadingTimeInDays());
        return response;
    }
}