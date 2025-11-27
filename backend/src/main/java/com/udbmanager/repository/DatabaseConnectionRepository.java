package com.udbmanager.repository;

import com.udbmanager.model.DatabaseConnection;
import com.udbmanager.model.DatabaseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DatabaseConnectionRepository extends JpaRepository<DatabaseConnection, String> {
    
    Optional<DatabaseConnection> findByConnectionName(String connectionName);
    
    List<DatabaseConnection> findByDatabaseType(DatabaseType databaseType);
    
    boolean existsByConnectionName(String connectionName);
}
