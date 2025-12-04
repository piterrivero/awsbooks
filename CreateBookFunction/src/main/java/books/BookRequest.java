package books;

import lombok.Data;

@Data
public class BookRequest {
    private String title;
    private int publicationYear;
    private String author;
    private String language;
    private String format;
}