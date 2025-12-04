package books;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class Book {
    private Integer id;
    private String title;
    private String author;
    private String finishDate;
    private Integer readingTimeInDays;
    private Integer readYear;
    private Integer publicationYear;
    private String format;
    private String language;

    @DynamoDbPartitionKey
    public Integer getId() {
        return id;
    }
}