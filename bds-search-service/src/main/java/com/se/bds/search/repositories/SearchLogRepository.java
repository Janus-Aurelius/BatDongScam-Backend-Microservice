package com.se.bds.search.repositories;

import com.se.bds.search.models.schemas.search.SearchLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SearchLogRepository extends MongoRepository<SearchLog, String> {
}
