package com.portfolio.library.repository;

import com.portfolio.library.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByMemberId(Long memberId);
    List<Loan> findByBookIdAndReturnDateIsNull(Long bookId);
    List<Loan> findByMemberIdAndReturnDateIsNull(Long memberId);
}
