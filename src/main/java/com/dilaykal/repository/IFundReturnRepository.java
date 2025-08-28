package com.dilaykal.repository;

import com.dilaykal.entities.FundInfo;
import com.dilaykal.entities.FundReturns;
import com.dilaykal.entities.ReturnTypes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface IFundReturnRepository extends JpaRepository<FundReturns, Integer> {

    @Query(
            value = "SELECT fr.* FROM fund_returns fr " +
                    "INNER JOIN fund_info fi ON fr.fund_id = fi.id  " +
                    "INNER JOIN return_types rt ON fr.return_type_id  = rt.id  " +
                    "WHERE fi.fund_code = :fundCode AND fr.date = :date",
            nativeQuery = true
    )
    List<FundReturns> findByFundCode(@Param("fundCode") String fundCode, @Param("date") LocalDate date);

    @Query(value = "SELECT fr.* FROM fund_returns fr " +
            "INNER JOIN fund_info fi ON fr.fund_id = fi.id  " +
            "INNER JOIN return_types rt ON fr.return_type_id = rt.id " +
            "WHERE fi.fund_code = :fundCode AND fr.date BETWEEN :startDate AND :endDate",
            nativeQuery = true)
    List<FundReturns> findByFundCodeAndDate(@Param("fundCode") String fundCode, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    Optional<FundReturns> findByFundInfoAndReturnTypesAndDate(
            FundInfo fundInfo,
            ReturnTypes returnTypes,
            LocalDate date
    );

    @Query(value = "SELECT fr.* FROM fund_returns fr " +
            "INNER JOIN fund_info fi ON fr.fund_id = fi.id  " +
            "INNER JOIN return_types rt ON fr.return_type_id = rt.id " ,
            nativeQuery = true)
    List<FundReturns> findAllWithAllDetails();

}
