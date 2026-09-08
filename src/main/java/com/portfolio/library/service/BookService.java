package com.portfolio.library.service;

import com.portfolio.library.dto.BookDTO;
import com.portfolio.library.entity.Book;
import com.portfolio.library.entity.Loan;
import com.portfolio.library.exception.DuplicateResourceException;
import com.portfolio.library.exception.InvalidOperationException;
import com.portfolio.library.exception.ResourceNotFoundException;
import com.portfolio.library.repository.BookRepository;
import com.portfolio.library.repository.LoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;

    @Autowired
    public BookService(BookRepository bookRepository, LoanRepository loanRepository) {
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
    }

    public List<BookDTO> getAllBooks() {
        return bookRepository.findAllWithLoans()
                .stream()
                .map(BookDTO::fromEntity)
                .toList();
    }

    public BookDTO getBookById(Long id) {
        return bookRepository.findById(id)
                .map(BookDTO::fromEntity)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "book not found with id : " + id
                        ));
    }

    public BookDTO createBook(Book book) {
        // 1. Check if the book already exists
        if (bookRepository.existsByIsbn(book.getIsbn())) {
            throw new DuplicateResourceException("Book already exists");
        }
        // 2. Save if it does not exist
        return BookDTO.fromEntity(bookRepository.save(book));
    }

    public BookDTO updateBook(Long id, Book updatedBook) {
        // 1. Check if the book exists, throw exception if it doesn't
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));

        // 2. Map the updated fields to the managed entity
        book.setTitle(updatedBook.getTitle());
        book.setAuthor(updatedBook.getAuthor());
        book.setIsbn(updatedBook.getIsbn());
        book.setTotalCopies(updatedBook.getTotalCopies());
        book.setAvailableCopies(updatedBook.getAvailableCopies());

        // 3. Save and return the updated entity
        return BookDTO.fromEntity(bookRepository.save(book));
    }

    public void deleteBook(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new ResourceNotFoundException("Book not found with id : " + id);
        }
        List<Loan> currentLoans = loanRepository.findByBookIdAndReturnDateIsNull(id);
        if (!currentLoans.isEmpty()) {
            throw new InvalidOperationException("Cannot delete a book with active loans.");
        }
        bookRepository.deleteById(id);
    }
}
