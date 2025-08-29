package com.dilaykal.repository;

import com.dilaykal.entities.FundInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IFundInfoRepository extends JpaRepository<FundInfo, Integer> {
    @Query(value = "SELECT * FROM fund_info WHERE fund_code = :fonKodu", nativeQuery = true)
    Optional<FundInfo> findByFundCode(@Param("fonKodu") String fonKodu);
}
