package search;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;
import java.util.stream.Collectors;

public class SearchBooksHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String TABLE_NAME = System.getenv("TABLE_NAME");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        // Handle CORS preflight
        if ("OPTIONS".equals(input.getHttpMethod())) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(Map.of(
                            "Access-Control-Allow-Origin", "*",
                            "Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS",
                            "Access-Control-Allow-Headers", "Content-Type, Authorization"
                    ))
                    .withBody("");
        }
        
        try {
            // Get query parameters
            Map<String, String> queryParams = input.getQueryStringParameters();
            if (queryParams == null) {
                queryParams = new HashMap<>();
            }

            String title = queryParams.get("title");
            String author = queryParams.get("author");
            String year = queryParams.get("year");
            String readYear = queryParams.get("readYear");
            String language = queryParams.get("language");
            String format = queryParams.get("format");

            // Build scan request
            ScanRequest.Builder scanBuilder = ScanRequest.builder()
                    .tableName(TABLE_NAME);

            List<String> filterExpressions = new ArrayList<>();
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();

            // Apply DynamoDB filters for exact matches only
            Map<String, String> expressionAttributeNames = new HashMap<>();
            
            // Only add exact match filters for numeric fields
            if (year != null && !year.trim().isEmpty()) {
                filterExpressions.add("#publicationYear = :year");
                expressionAttributeValues.put(":year", AttributeValue.builder().n(year.trim()).build());
                expressionAttributeNames.put("#publicationYear", "publicationYear");
            }

            if (readYear != null && !readYear.trim().isEmpty()) {
                filterExpressions.add("#readYear = :readYear");
                expressionAttributeValues.put(":readYear", AttributeValue.builder().n(readYear.trim()).build());
                expressionAttributeNames.put("#readYear", "readYear");
            }

            if (format != null && !format.trim().isEmpty()) {
                filterExpressions.add("#format = :format");
                expressionAttributeValues.put(":format", AttributeValue.builder().s(format.trim()).build());
                expressionAttributeNames.put("#format", "format");
            }
            if (!filterExpressions.isEmpty()) {
                scanBuilder.filterExpression(String.join(" AND ", filterExpressions))
                          .expressionAttributeValues(expressionAttributeValues)
                          .expressionAttributeNames(expressionAttributeNames);
            }

            ScanResponse response = dynamoDbClient.scan(scanBuilder.build());

            // Convert to Book objects and apply case-insensitive filters in memory
            List<Map<String, Object>> books = response.items().stream()
                    .map(this::convertToBook)
                    .filter(book -> {
                        // Case-insensitive title filter
                        if (title != null && !title.trim().isEmpty()) {
                            String bookTitle = (String) book.get("title");
                            if (bookTitle == null || !bookTitle.toLowerCase().contains(title.trim().toLowerCase())) {
                                return false;
                            }
                        }
                        
                        // Case-insensitive author filter
                        if (author != null && !author.trim().isEmpty()) {
                            String bookAuthor = (String) book.get("author");
                            if (bookAuthor == null || !bookAuthor.toLowerCase().contains(author.trim().toLowerCase())) {
                                return false;
                            }
                        }
                        
                        // Case-insensitive language filter
                        if (language != null && !language.trim().isEmpty()) {
                            String bookLanguage = (String) book.get("language");
                            if (bookLanguage == null || !bookLanguage.toLowerCase().contains(language.trim().toLowerCase())) {
                                return false;
                            }
                        }
                        
                        return true;
                    })
                    .collect(Collectors.toList());

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(Map.of(
                            "Content-Type", "application/json",
                            "Access-Control-Allow-Origin", "*",
                            "Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS",
                            "Access-Control-Allow-Headers", "Content-Type, Authorization"
                    ))
                    .withBody(objectMapper.writeValueAsString(books));

        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withHeaders(Map.of(
                            "Content-Type", "application/json",
                            "Access-Control-Allow-Origin", "*"
                    ))
                    .withBody("{\"message\":\"Internal server error: " + e.getMessage() + "\"}");
        }
    }

    private Map<String, Object> convertToBook(Map<String, AttributeValue> item) {
        Map<String, Object> book = new HashMap<>();
        
        if (item.get("id") != null) {
            book.put("id", Integer.parseInt(item.get("id").n()));
        }
        if (item.get("title") != null) {
            book.put("title", item.get("title").s());
        }
        if (item.get("author") != null) {
            book.put("author", item.get("author").s());
        }
        if (item.get("publicationYear") != null) {
            book.put("publicationYear", Integer.parseInt(item.get("publicationYear").n()));
        }
        if (item.get("language") != null) {
            book.put("language", item.get("language").s());
        }
        if (item.get("format") != null) {
            book.put("format", item.get("format").s());
        }
        if (item.get("readYear") != null) {
            book.put("readYear", Integer.parseInt(item.get("readYear").n()));
        }
        if (item.get("finishDate") != null) {
            book.put("finishDate", item.get("finishDate").s());
        }
        if (item.get("readingTimeInDays") != null) {
            book.put("readingTimeInDays", Integer.parseInt(item.get("readingTimeInDays").n()));
        }
        
        return book;
    }
}