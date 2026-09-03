package com.portfolio.library.controller;

import com.portfolio.library.dto.LoanDTO;
import com.portfolio.library.entity.Loan;
import com.portfolio.library.service.LoanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    @Autowired
    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public LoanDTO checkoutBook(@RequestParam Long bookId, @RequestParam Long memberId) {
        return loanService.checkoutBook(bookId, memberId);
    }

    @PostMapping("/{loanId}/return")
    public LoanDTO returnBook(@PathVariable Long loanId) {
        return loanService.returnBook(loanId);
    }

    @GetMapping("/member/{memberId}")
    public List<LoanDTO> getLoansForMember(@PathVariable Long memberId) {
        return loanService.getLoansForMember(memberId);
    }
}
