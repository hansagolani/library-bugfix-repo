package com.portfolio.library.service;

import com.portfolio.library.dto.MemberDTO;
import com.portfolio.library.entity.Loan;
import com.portfolio.library.entity.Member;
import com.portfolio.library.exception.DuplicateResourceException;
import com.portfolio.library.exception.InvalidOperationException;
import com.portfolio.library.exception.ResourceNotFoundException;
import com.portfolio.library.repository.LoanRepository;
import com.portfolio.library.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final LoanRepository loanRepository;

    @Autowired
    public MemberService(MemberRepository memberRepository, LoanRepository loanRepository) {
        this.memberRepository = memberRepository;
        this.loanRepository = loanRepository;
    }

    public List<MemberDTO> getAllMembers() {
        return memberRepository.findAll()
                .stream()
                .map(MemberDTO::fromEntity)
                .toList();
    }

    public MemberDTO getMemberById(Long id) {
        return memberRepository.findById(id)
                .map(MemberDTO::fromEntity)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Member not found with id : " + id
                        ));
    }

    public MemberDTO createMember(Member member) {
        // 1. Check if the member already exists
        if (memberRepository.existsByEmail(member.getEmail())) {
            throw new DuplicateResourceException("Member already exists");
        }
        // 2. Save if it does not exist
        return MemberDTO.fromEntity(memberRepository.save(member));
    }

    public void deleteMember(Long id) {
        List<Loan> currentLoans = loanRepository.findByMemberIdAndReturnDateIsNull(id);
        if (!currentLoans.isEmpty()) {
            throw new InvalidOperationException("Cannot delete a member with active loans.");
        }
        memberRepository.deleteById(id);
    }
}
