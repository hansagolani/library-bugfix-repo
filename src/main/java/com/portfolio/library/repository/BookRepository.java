package com.portfolio.library.repository;

import com.portfolio.library.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {
    Optional<Book> findByIsbn(String isbn);
    boolean existsByIsbn(String isbn);

    @Query("SELECT DISTINCT b FROM Book b LEFT JOIN FETCH b.loans")
    List<Book> findAllWithLoans();
}
