package com.scheduling.serviceImplimentation;

import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.scheduling.dto.NotificationResponseDto;
import com.scheduling.dto.UserResponseDto;
import com.scheduling.model.Notification;
import com.scheduling.model.UserEntity;
import com.scheduling.repository.NotificationRepository;
import com.scheduling.services.NotificationService;
import com.scheduling.services.UserServices;

@Service
public class NotificationServiceImplimentation implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepo;
    
    @Autowired
    private UserServices userServices;
     
    @Override
    public void createNotification(UserEntity receiver, UserEntity sender, String title, String message, Date date,
            Time time) {
        try {
            Notification notification = new Notification();
            notification.setReceiver(receiver);
            notification.setSender(sender);
            notification.setMessage(message);
            notification.setTitle(title);
            notification.setNotificationDate(date);
            notification.setNotificationTime(time);
            notificationRepo.save(notification);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    @Override
    public List<NotificationResponseDto> getUserNotification(String username) {
        
        try {
            List<NotificationResponseDto> dtos = new ArrayList<>();
            
            UserEntity entity = userServices.getUserByEmail(username);
            
            List<Notification> notifications = notificationRepo.findAllByUser(entity);
            
            return convertor(dtos,notifications);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    private List<NotificationResponseDto> convertor(List<NotificationResponseDto> dtos,
            List<Notification> notifications) {
        notifications.stream().forEach(notification -> {
            NotificationResponseDto nr = new NotificationResponseDto();
            nr.setReceiver(userConvertor(notification.getReceiver()));
            nr.setSender(userConvertor(notification.getSender()));
            nr.setDate(notification.getNotificationDate().toString());
            nr.setTime(notification.getNotificationTime().toString());
            nr.setMessage(notification.getMessage());
            dtos.add(nr);
        });
        return dtos;
    }

    private UserResponseDto userConvertor(UserEntity receiver) {
        UserResponseDto dto = new UserResponseDto();
        dto.setUsername(receiver.getUserName());
        dto.setProfilePath(receiver.getProfilePath());
        dto.setUserId(receiver.getUserId());
        return dto;
    }

}
