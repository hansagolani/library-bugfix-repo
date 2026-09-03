package com.portfolio.library.dto;

import com.portfolio.library.entity.Member;
import java.util.List;
import java.util.Optional;

public record MemberDTO (
        Long id,
        String name,
        String email,
        List<LoanDTO> loans
){

    public static MemberDTO fromEntity(Member member) {
        return new MemberDTO(
                member.getId(),
                member.getName(),
                member.getEmail(),
                Optional.ofNullable(member.getLoans())
                        .orElse(List.of())
                        .stream()
                        .map(LoanDTO::fromEntity)
                        .toList()
        );
    }
}
