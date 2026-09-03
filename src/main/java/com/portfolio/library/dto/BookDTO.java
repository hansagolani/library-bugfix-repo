package com.portfolio.library.dto;

import com.portfolio.library.entity.Book;
import java.util.List;
import java.util.Optional;

public record BookDTO (
    Long id,
    String title,
    String author,
    String isbn,
    int totalCopies,
    int availableCopies,
    List<LoanDTO> loans
) {

    public static BookDTO fromEntity(Book book) {
        return new BookDTO(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                book.getTotalCopies(),
                book.getAvailableCopies(),
                Optional.ofNullable(book.getLoans())
                        .orElse(List.of())
                        .stream()
                        .map(LoanDTO::fromEntity)
                        .toList()
        );
    }

}
