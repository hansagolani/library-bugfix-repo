package com.portfolio.library.dto;

import com.portfolio.library.entity.Loan;

import java.time.LocalDate;

public record LoanDTO (
        Long id,
        Long bookId,
        Long memberId,
        LocalDate loanDate,
        LocalDate dueDate,
        LocalDate returnDate
) {
    public static LoanDTO fromEntity(Loan loan) {
        return new LoanDTO(
                loan.getId(),
                loan.getBook().getId(),
                loan.getMember().getId(),
                loan.getLoanDate(),
                loan.getDueDate(),
                loan.getReturnDate()
        );
    }
}