package com.scheduling.serviceImplimentation;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.scheduling.model.CommentOnPost;
import com.scheduling.model.LikePostComment;
import com.scheduling.model.UserEntity;
import com.scheduling.notification.PushNotificationRequest;
import com.scheduling.notification.PushNotificationService;
import com.scheduling.repository.LikePostCommentRepository;
import com.scheduling.services.CommentOnPostService;
import com.scheduling.services.LikePostCommentService;
import com.scheduling.services.NotificationService;
import com.scheduling.services.UserServices;

@Service
public class LikePostCommentImplimentation implements LikePostCommentService {

    @Autowired
    private CommentOnPostService commentOnPostService;
    
    @Autowired
    private UserServices userServices;
    
    @Autowired
    private LikePostCommentRepository likePostCommentRepo;

    @Autowired
    private PushNotificationService pushNotificationService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Override
    public String createLike(Long commentId, String username) {
        try {
            UserEntity entity = userServices.getUserByEmail(username);
            CommentOnPost commentPost = commentOnPostService.getCommentByCommentIdOnPost(commentId);
            if (entity!=null && commentPost!=null) {
                
                LikePostComment likePostComment = likePostCommentRepo.getLikePostComment(entity,commentPost);
                
                if (likePostComment!=null) {
                    likePostCommentRepo.delete(likePostComment);
                    return "disliked";
                }else {
                    LikePostComment comment = new LikePostComment();
                    comment.setCommentOnPost(commentPost);
                    comment.setCommentTime(Time.valueOf(LocalTime.now()));
                    comment.setCommnentDate(Date.valueOf(LocalDate.now()));
                    comment.setUserEntity(entity);
                    likePostCommentRepo.save(comment);
                    if (sendNotification(entity.getUserName(),commentPost.getUserEntity().getMobileToken())) {
                        notificationService.createNotification(commentPost.getUserEntity(),entity,"E-dost ",entity.getUserName() +" likes your comment",Date.valueOf(LocalDate.now()),Time.valueOf(LocalTime.now()));
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
                PushNotificationRequest notificationRequest = new PushNotificationRequest("E-dost ",username+" likes your comment",token);
                pushNotificationService.sendPushNotificationToToken(notificationRequest);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
       } 
       return true; 
   }
    
}
