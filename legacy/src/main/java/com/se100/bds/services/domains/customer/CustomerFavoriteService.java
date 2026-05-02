package com.se100.bds.services.domains.customer;

import com.se100.bds.dtos.responses.property.SimplePropertyCard;
import com.se100.bds.utils.Constants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CustomerFavoriteService {
    boolean like(UUID refId, Constants.LikeTypeEnum likeType);
    boolean isLike(UUID refId, UUID customerId, Constants.LikeTypeEnum likeType);
    boolean isLikeByMe(UUID refId, Constants.LikeTypeEnum likeType);
    Page<SimplePropertyCard> getFavoritePropertyCards(Pageable pageable);
}
