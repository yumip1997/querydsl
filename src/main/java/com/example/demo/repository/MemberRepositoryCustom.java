package com.example.demo.repository;

import com.example.demo.dto.MemberSearchCondition;
import com.example.demo.dto.MemberTeamDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MemberRepositoryCustom {
    List<MemberTeamDto> search(MemberSearchCondition condition);

    Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable);
}
