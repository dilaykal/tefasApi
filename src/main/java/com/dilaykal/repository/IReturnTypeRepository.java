package com.dilaykal.repository;

import com.dilaykal.entities.ReturnTypes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IReturnTypeRepository extends JpaRepository<ReturnTypes, Integer> {
}
