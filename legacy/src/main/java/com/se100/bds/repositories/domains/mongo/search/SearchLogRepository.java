package com.se100.bds.repositories.domains.mongo.search;

import com.se100.bds.models.schemas.search.SearchLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SearchLogRepository extends MongoRepository<SearchLog, String> {
}
