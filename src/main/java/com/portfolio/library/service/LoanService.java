package com.portfolio.library.service;

import com.portfolio.library.dto.LoanDTO;
import com.portfolio.library.entity.Book;
import com.portfolio.library.entity.Loan;
import com.portfolio.library.entity.Member;
import com.portfolio.library.exception.InvalidOperationException;
import com.portfolio.library.exception.ResourceNotFoundException;
import com.portfolio.library.repository.BookRepository;
import com.portfolio.library.repository.LoanRepository;
import com.portfolio.library.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * This is the heart of the app's business rules - most of the planted bugs
 * in this project will likely live here (checkout/return logic, copy counts,
 * due dates). Design it carefully; write it yourself.
 */
@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;

    private static final int DEFAULT_LOAN_PERIOD = 14;

    @Autowired
    public LoanService(LoanRepository loanRepository, BookRepository bookRepository, MemberRepository memberRepository) {
        this.loanRepository = loanRepository;
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;
    }

    /**
     * Checks out a book to a member.
     * TODO: implement. Think through:
     * - What happens if there are 0 available copies?
     * - What's the default loan period (due date)?
     * - What update needs to happen to the book's availableCopies?
     */

    public LoanDTO checkoutBook(Long bookId, Long memberId) {
        // 1. Check if the book already exists
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id : " + bookId));

        // 2. Check if the member already exists
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id : " + memberId));

        // 3. Check if there are any copies available. If yes, decrement the availableCopies.
        if(book.getAvailableCopies() <= 0)
            throw new InvalidOperationException("No more copies available.");
        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);

        //4. Create the loan record
        Loan loan = new Loan(book, member, LocalDate.now(), LocalDate.now().plusDays(DEFAULT_LOAN_PERIOD));
        return LoanDTO.fromEntity(loanRepository.save(loan));
    }

    /**
     * Marks a loan as returned.
     * TODO: implement. Think through:
     * - What happens if the loan was already returned?
     * - What update needs to happen to the book's availableCopies?
     */
    @Transactional
    public LoanDTO returnBook(Long loanId) {
        // 1. Check if the loan exists
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("loan not found with id : " + loanId));

        // 2. Check if the loan was already returned
        if (loan.getReturnDate() != null)
            throw new InvalidOperationException("The loan was already returned.");

        Book book = loan.getBook();
//         Considered a redundant safety check here for book.getTotalCopies() == book.getAvailableCopies()
//         in case of DB drift from manual edits in shared dev/QA/UAT environments.
//         Removed for this project since the returnDate null check above is sufficient
//         - see README design decisions for the tradeoff.

//        if (book.getTotalCopies() == book.getAvailableCopies())
//            throw new InvalidOperationException("The book was already returned.");

        // 3. increment availableCopies of the book
        book.setAvailableCopies(book.getAvailableCopies() + 1);
        bookRepository.save(book);

        // 4. Update the returnDate of the loan
        loan.setReturnDate(LocalDate.now());
        return LoanDTO.fromEntity(loanRepository.save(loan));
    }

    public List<LoanDTO> getLoansForMember(Long memberId) {
        List<Loan> loans = loanRepository.findByMemberId(memberId);
        if (loans.isEmpty() && !memberRepository.existsById(memberId)) {
            throw new ResourceNotFoundException("Member not found with id : " + memberId);
        }
        return loans.stream()
                .map(LoanDTO::fromEntity)
                .toList();
    }
}
