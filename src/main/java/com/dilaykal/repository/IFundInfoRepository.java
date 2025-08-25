package com.dilaykal.repository;

import com.dilaykal.entities.FundInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface IFundInfoRepository extends JpaRepository<FundInfo, String> {

    @Query("SELECT DISTINCT fi FROM FundInfo fi " +
            "JOIN FETCH fi.fundReturns fr " +
            "JOIN FETCH fr.returnTypes rt")
    List<FundInfo> findAllWithAllDetails();


}
