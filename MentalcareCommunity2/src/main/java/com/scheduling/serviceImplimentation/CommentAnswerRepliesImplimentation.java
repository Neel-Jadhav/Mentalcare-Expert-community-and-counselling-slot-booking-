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

import com.scheduling.dto.CommentAnswerRepliesRequestDto;
import com.scheduling.dto.CommentRepliesDto;
import com.scheduling.dto.UserResponseDto;
import com.scheduling.model.AnswerCommentReply;
import com.scheduling.model.CommentOnAnswer;
import com.scheduling.model.UserEntity;
import com.scheduling.repository.CommentAnswerRepliesRepository;
import com.scheduling.services.CommentAnswerRepliesService;
import com.scheduling.services.CommetOnAnswerService;
import com.scheduling.services.UserServices;

@Service
public class CommentAnswerRepliesImplimentation implements CommentAnswerRepliesService {
    
    @Autowired
    private UserServices userServices;

    @Autowired
    private CommentAnswerRepliesRepository answerRepliesRepository;
    
    @Autowired
    private CommetOnAnswerService commetOnAnswerService;

    @Override
    public boolean createComment(String username, Long commentId,
            CommentAnswerRepliesRequestDto answerRepliesRequestDto) {
        
        try {
            UserEntity userEntity = userServices.getUserByEmail(username);
            
            CommentOnAnswer commentOnAnswer = commetOnAnswerService.getCommentOnAnswerById(commentId);
            
            if (userEntity!=null && commentOnAnswer!=null) {
                AnswerCommentReply answerCommentReply = new AnswerCommentReply();
                answerCommentReply.setCommentOnAnswer(commentOnAnswer);
                answerCommentReply.setCommentText(answerRepliesRequestDto.getComment());
                answerCommentReply.setUserEntity(userEntity);
                answerCommentReply.setCommentTime(Time.valueOf(LocalTime.now()));
                answerCommentReply.setCommnentDate(Date.valueOf(LocalDate.now()));
                answerRepliesRepository.save(answerCommentReply);
                return true;
            }
            
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean deleteCommentReplies(Long commentReplyId, String username) {
        
        try {
            UserEntity userEntity = userServices.getUserByEmail(username);
            
            Optional<AnswerCommentReply> answerCommentReply = answerRepliesRepository.findById(commentReplyId);
            
            if (answerCommentReply.isPresent() && userEntity!=null) {
                if (answerCommentReply.get().getUserEntity().equals(userEntity)) {
                    answerRepliesRepository.delete(answerCommentReply.get());
                    return true;
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return false;
    }

    @Override
    public List<CommentRepliesDto> getcommentsreplies(Long commentsId, UserDetails userDetails) {
        
        CommentOnAnswer commentOnAnswer = commetOnAnswerService.getCommentOnAnswerById(commentsId);
        if (commentOnAnswer!=null) {
            if (userDetails!=null) {
                UserEntity userEntity = userServices.getUserByEmail(userDetails.getUsername());
                List<CommentRepliesDto> commentResponseDtos = convertToDto(answerRepliesRepository.getCommentsReplies(commentOnAnswer),userEntity);
                return commentResponseDtos;
            }else {
                List<CommentRepliesDto> commentResponseDtos = convertToDto(answerRepliesRepository.getCommentsReplies(commentOnAnswer),null);
                return commentResponseDtos;
            }
            
        }
        
        
        return null;
    }

    private List<CommentRepliesDto> convertToDto(List<AnswerCommentReply> commentsReplies,UserEntity userEntity) {
        List<CommentRepliesDto> commentResponseDtos = new ArrayList<>(); 
        commentsReplies.stream().forEach(comments ->{
                CommentRepliesDto commentResponseDto = new CommentRepliesDto();
                commentResponseDto.setCommentId(comments.getAnswerCommentReplyId());
                commentResponseDto.setComment(comments.getCommentText());
                commentResponseDto.setPostDate(comments.getCommnentDate());
                commentResponseDto.setPostTime(comments.getCommentTime());
                commentResponseDto.setUserResponseDto(userDtoConvertor(comments.getUserEntity()));
                commentResponseDtos.add(commentResponseDto);
            });
        return commentResponseDtos;
    }
    
    private UserResponseDto userDtoConvertor(UserEntity userEntity) {
        UserResponseDto userResponseDto = new UserResponseDto();
        userResponseDto.setUsername(userEntity.getUserName());
        userResponseDto.setProfilePath(userEntity.getProfilePath());
        userResponseDto.setUserId(userEntity.getUserId());
        return userResponseDto;
    }
   
}
