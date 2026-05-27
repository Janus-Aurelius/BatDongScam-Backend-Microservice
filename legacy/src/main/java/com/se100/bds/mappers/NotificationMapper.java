package com.se100.bds.mappers;

import com.se100.bds.dtos.responses.notification.NotificationDetails;
import com.se100.bds.dtos.responses.notification.NotificationItem;
import com.se100.bds.models.entities.notification.Notification;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper extends BaseMapper {

    protected NotificationMapper(ModelMapper modelMapper) {
        super(modelMapper);
    }

    @Override
    protected void configureCustomMappings() {
        // Mapping for NotificationItem
        modelMapper.createTypeMap(Notification.class, NotificationItem.class)
                .addMapping(Notification::getId, NotificationItem::setId)
                .addMapping(Notification::getCreatedAt, NotificationItem::setCreatedAt)
                .addMapping(Notification::getUpdatedAt, NotificationItem::setUpdatedAt)
                .addMapping(Notification::getType, NotificationItem::setType)
                .addMapping(Notification::getTitle, NotificationItem::setTitle)
                .addMapping(Notification::getIsRead, NotificationItem::setRead);

        // Mapping for NotificationDetails
        modelMapper.createTypeMap(Notification.class, NotificationDetails.class)
                .addMapping(Notification::getId, NotificationDetails::setId)
                .addMapping(Notification::getCreatedAt, NotificationDetails::setCreatedAt)
                .addMapping(Notification::getUpdatedAt, NotificationDetails::setUpdatedAt)
                .addMapping(Notification::getType, NotificationDetails::setType)
                .addMapping(Notification::getTitle, NotificationDetails::setTitle)
                .addMapping(Notification::getIsRead, NotificationDetails::setRead)
                .addMapping(Notification::getMessage, NotificationDetails::setMessage)
                .addMapping(Notification::getRelatedEntityType, NotificationDetails::setRelatedEntityType)
                .addMapping(Notification::getRelatedEntityId, NotificationDetails::setRelatedEntityId)
                .addMapping(Notification::getImgUrl, NotificationDetails::setImgUrl)
                .addMapping(Notification::getReadAt, NotificationDetails::setReadAt);
    }

    public NotificationItem toNotificationItem(Notification notification) {
        return mapTo(notification, NotificationItem.class);
    }

    public NotificationDetails toNotificationDetails(Notification notification) {
        return mapTo(notification, NotificationDetails.class);
    }
}

