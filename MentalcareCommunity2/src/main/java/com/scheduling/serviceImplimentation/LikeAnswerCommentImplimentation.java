package com.scheduling.serviceImplimentation;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.scheduling.model.CommentOnAnswer;
import com.scheduling.model.LikeAnswerComment;
import com.scheduling.model.UserEntity;
import com.scheduling.notification.PushNotificationRequest;
import com.scheduling.notification.PushNotificationService;
import com.scheduling.repository.LikeAnswerCommentRepository;
import com.scheduling.services.CommetOnAnswerService;
import com.scheduling.services.LikeAnswerCommentService;
import com.scheduling.services.NotificationService;
import com.scheduling.services.UserServices;

@Service
public class LikeAnswerCommentImplimentation implements LikeAnswerCommentService {

    
    @Autowired
    private LikeAnswerCommentRepository answerCommentRepository;
    
    @Autowired 
    private CommetOnAnswerService commentOnAnswerService;
    
    @Autowired
    private UserServices userServices;

    @Autowired
    private PushNotificationService pushNotificationService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Override
    public String createLike(Long answerCommentId, String username) {
        
        try {
            UserEntity userEntity = userServices.getUserByEmail(username);
            
            CommentOnAnswer commentOnAnswer = commentOnAnswerService.getCommentOnAnswerById(answerCommentId);
            
            if (userEntity!=null && commentOnAnswer!=null) {
                
                LikeAnswerComment answerComment1 = answerCommentRepository.getlikeAnswerComment(commentOnAnswer,userEntity); 
                if (answerComment1!=null) { 
                    answerCommentRepository.delete(answerComment1);
                    return "disliked";    
                }else {
                    LikeAnswerComment answerComment = new LikeAnswerComment();
                    answerComment.setCommentOnAnswer(commentOnAnswer);
                    answerComment.setUserEntity(userEntity);
                    answerComment.setCommentTime(Time.valueOf(LocalTime.now()));
                    answerComment.setCommnentDate(Date.valueOf(LocalDate.now()));
                    answerCommentRepository.save(answerComment);
                    if (sendNotification(userEntity.getUserName(),commentOnAnswer.getUserEntity().getMobileToken())) {
                        notificationService.createNotification(commentOnAnswer.getUserEntity(),userEntity,"E-dost ",userEntity.getUserName() +" likes your comment",Date.valueOf(LocalDate.now()),Time.valueOf(LocalTime.now()));
                    };
                    return "liked";
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return "";
    }
    
    private boolean sendNotification(String username,String token) {
        if (token!=null) {
            try {
                PushNotificationRequest notificationRequest = new PushNotificationRequest("E-dost ",username +" likes your comment",token);
                pushNotificationService.sendPushNotificationToToken(notificationRequest);
            } catch (Exception e) {
                e.printStackTrace();
            }
       }
       return true; 
   }
    
    
}
