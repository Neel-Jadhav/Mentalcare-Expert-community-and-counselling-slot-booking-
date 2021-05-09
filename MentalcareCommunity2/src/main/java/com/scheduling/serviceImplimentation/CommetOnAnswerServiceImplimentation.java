package com.scheduling.serviceImplimentation;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.scheduling.dto.AnswerCommentDto;
import com.scheduling.dto.AnswerDto;
import com.scheduling.dto.CommentOnAnswerResponseDto;
import com.scheduling.dto.CommentResponseDto;
import com.scheduling.dto.UserResponseDto;
import com.scheduling.model.Answer;
import com.scheduling.model.CommentOnAnswer;
import com.scheduling.model.LikeAnswerComment;
import com.scheduling.model.UserEntity;
import com.scheduling.notification.PushNotificationRequest;
import com.scheduling.notification.PushNotificationService;
import com.scheduling.repository.CommetOnAnswerRepository;
import com.scheduling.services.AnswerService;
import com.scheduling.services.CommetOnAnswerService;
import com.scheduling.services.NotificationService;
import com.scheduling.services.UserServices;

@Service
public class CommetOnAnswerServiceImplimentation  implements  CommetOnAnswerService{

    @Autowired
    private CommetOnAnswerRepository commentRepo;

    @Autowired
    private UserServices userServices;
    
    @Autowired
    private AnswerService answerService;
    
    @Autowired
    private PushNotificationService pushNotificationService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Override
    public CommentOnAnswerResponseDto createAnswercomment(AnswerCommentDto answerCommentDto, Long answerId,
            String username) {
        try {
            UserEntity userEntity = userServices.getUserByEmail(username);
            
            Answer answer = answerService.getanswerByAnswerId(answerId);
            
            if (answer!=null && userEntity!=null) {
                CommentOnAnswer commentOnAnswer = new CommentOnAnswer();
                commentOnAnswer.setComment(answerCommentDto.getComment());
                commentOnAnswer.setAnswer(answer);
                commentOnAnswer.setUserEntity(userEntity);
                commentOnAnswer.setCommnentDate(Date.valueOf(LocalDate.now()));
                commentOnAnswer.setCommentTime(Time.valueOf(LocalTime.now()));
                CommentOnAnswer saved = commentRepo.save(commentOnAnswer);
                CommentOnAnswerResponseDto answerResponseDto = convertor(saved);
                if (sendNotification(userEntity.getUserName(),answer.getUserEntity().getMobileToken())) {
                    notificationService.createNotification(answer.getUserEntity(),userEntity,"E-dost",userEntity.getUserName() +" comment on your answer",Date.valueOf(LocalDate.now()),Time.valueOf(LocalTime.now()));
                };
                return answerResponseDto;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }

    private boolean sendNotification(String username,String token) {
        if (token!=null) {
            try {
                PushNotificationRequest notificationRequest = new PushNotificationRequest("E-dost",username +" comment on your answer",token);
                pushNotificationService.sendPushNotificationToToken(notificationRequest);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
       }
        return true;
   }
    
    private CommentOnAnswerResponseDto convertor(CommentOnAnswer commentOnAnswer) {
        CommentOnAnswerResponseDto commentOnAnswerResponseDto = new CommentOnAnswerResponseDto();
        commentOnAnswerResponseDto.setAnswerCommentDto(new AnswerCommentDto(commentOnAnswer.getComment()));
        commentOnAnswerResponseDto.setUserResponseDto(new UserResponseDto(commentOnAnswer.getUserEntity().getUserId(), commentOnAnswer.getUserEntity().getUserName(), commentOnAnswer.getUserEntity().getProfilePath()));
        commentOnAnswerResponseDto.setCommentDate(commentOnAnswer.getCommnentDate());
        commentOnAnswerResponseDto.setCommentTime(commentOnAnswer.getCommentTime());
        commentOnAnswerResponseDto.setAnswerDto(new AnswerDto(commentOnAnswer.getAnswer().getAnswerId(),commentOnAnswer.getAnswer().getUserAnswer()));
        commentOnAnswerResponseDto.setCommentId(commentOnAnswer.getCommentOnAnswerId());
        return commentOnAnswerResponseDto;
    }

    @Override
    public boolean deleteCommment(Long commentId, String username) {
        try {
            UserEntity userEntity = userServices.getUserByEmail(username);
            
            Optional<CommentOnAnswer> commentOnAnswer = commentRepo.findById(commentId);
            
            if (commentOnAnswer.isPresent()) {
                if (userEntity!=null) {
                    if (commentOnAnswer.get().getUserEntity().equals(userEntity)) {
                        commentRepo.delete(commentOnAnswer.get());
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return false;
    }

    @Override
    public CommentOnAnswer getCommentOnAnswerById(Long answerCommentId) {
        Optional<CommentOnAnswer> commentOnAnswer = commentRepo.findById(answerCommentId);    
        if (commentOnAnswer.isPresent()) {
            return commentOnAnswer.get();
        }
        return null;
    }

    @Override
    public List<CommentResponseDto> getAllCommentsOfAnswer(Long answerId, UserDetails details) {
        try {
            Answer answer = answerService.getanswerByAnswerId(answerId);

            if (answer!=null) {
                if (details!=null) {
                    UserEntity userEntity = userServices.getUserByEmail(details.getUsername());
                    List<CommentResponseDto> commentResponseDtos = converToDto(commentRepo.getAllCommentsOfAnswer(answer),userEntity);
                    return commentResponseDtos;
                }else {
                    List<CommentResponseDto> commentResponseDtos = converToDto(commentRepo.getAllCommentsOfAnswer(answer),null);
                    return commentResponseDtos;
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    private List<CommentResponseDto> converToDto(List<CommentOnAnswer> allCommentsOfAnswer,UserEntity userEntity) {
        
        List<CommentResponseDto> commentResponseDtos = new ArrayList<>();
        
        if (userEntity!=null) {
            allCommentsOfAnswer.stream().forEach(comments -> {
                CommentResponseDto commentResponseDto = new CommentResponseDto();
                commentResponseDto.setCommentId(comments.getCommentOnAnswerId());
                commentResponseDto.setComment(comments.getComment());
                commentResponseDto.setNoComments(Long.valueOf(comments.getPostCommentReplies().size()));
                commentResponseDto.setNoLikes(Long.valueOf(comments.getAnswerComments().size()));
                commentResponseDto.setPostDate(comments.getCommnentDate());
                commentResponseDto.setPostTime(comments.getCommentTime());
                commentResponseDto.setLiked(userLikedOrNot(comments.getAnswerComments(),userEntity));
                commentResponseDto.setUserResponseDto(userDtoConvertor(comments.getUserEntity()));
                commentResponseDtos.add(commentResponseDto);
            });
        }else {
            allCommentsOfAnswer.stream().forEach(comments -> {
                CommentResponseDto commentResponseDto = new CommentResponseDto();
                commentResponseDto.setCommentId(comments.getCommentOnAnswerId());
                commentResponseDto.setComment(comments.getComment());
                commentResponseDto.setNoComments(Long.valueOf(comments.getPostCommentReplies().size()));
                commentResponseDto.setNoLikes(Long.valueOf(comments.getAnswerComments().size()));
                commentResponseDto.setPostDate(comments.getCommnentDate());
                commentResponseDto.setPostTime(comments.getCommentTime());
                commentResponseDto.setLiked(false);
                commentResponseDto.setUserResponseDto(userDtoConvertor(comments.getUserEntity()));
                commentResponseDtos.add(commentResponseDto);
        });
        }  
        return commentResponseDtos;
    }
    private UserResponseDto userDtoConvertor(UserEntity userEntity) {
        UserResponseDto userResponseDto = new UserResponseDto();
        userResponseDto.setUsername(userEntity.getUserName());
        userResponseDto.setProfilePath(userEntity.getProfilePath());
        userResponseDto.setUserId(userEntity.getUserId());
        return userResponseDto;
    }

    private boolean userLikedOrNot(List<LikeAnswerComment> likePostComments, UserEntity userEntity) {
        for (LikeAnswerComment likePostComment : likePostComments) {
            if (likePostComment.getUserEntity().equals(userEntity)) {
                return true;
            }
        }
        return false;
    }

    
}
