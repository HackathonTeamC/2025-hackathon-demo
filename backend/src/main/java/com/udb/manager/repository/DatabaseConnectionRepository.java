package com.udb.manager.repository;

import com.udb.manager.model.DatabaseConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DatabaseConnectionRepository extends JpaRepository<DatabaseConnection, String> {
}
