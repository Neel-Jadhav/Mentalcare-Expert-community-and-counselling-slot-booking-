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

import com.scheduling.dto.CommentPostRepliesRequestDto;
import com.scheduling.dto.CommentRepliesDto;
import com.scheduling.dto.UserResponseDto;
import com.scheduling.model.CommentOnPost;
import com.scheduling.model.PostCommentReply;
import com.scheduling.model.UserEntity;
import com.scheduling.repository.CommentOnPostReplieRepository;
import com.scheduling.services.CommentOnPostReplieService;
import com.scheduling.services.CommentOnPostService;
import com.scheduling.services.UserServices;

@Service
public class CommentOnPostReplieImplimentation implements CommentOnPostReplieService {

    @Autowired
    private UserServices userServices;
    
    @Autowired
    private CommentOnPostService commentOnPostService;

    @Autowired
    private CommentOnPostReplieRepository commentOnPostReplieRepository;
    
    @Override
    public boolean createComment(Long commentId, CommentPostRepliesRequestDto commentPostRepliesRequestDto,
            String username) {
        
        try {
            UserEntity userEntity = userServices.getUserByEmail(username);
            
            CommentOnPost commentOnPost = commentOnPostService.getCommentByCommentIdOnPost(commentId);
            
            if (userEntity!=null && commentOnPost!=null) {
                PostCommentReply postCommentReply = new PostCommentReply();
                postCommentReply.setCommentText(commentPostRepliesRequestDto.getComment());
                postCommentReply.setUserEntity(userEntity);
                postCommentReply.setCommentOnPost(commentOnPost);
                postCommentReply.setCommentTime(Time.valueOf(LocalTime.now()));
                postCommentReply.setCommnentDate(Date.valueOf(LocalDate.now()));
                commentOnPostReplieRepository.save(postCommentReply);
                return true;
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean deleteCommentReply(Long replyId, String username) {
        try {
            UserEntity userEntity = userServices.getUserByEmail(username);
            Optional<PostCommentReply> commentReply = commentOnPostReplieRepository.findById(replyId);
            if (commentReply.isPresent() && userEntity!=null) {
                if (commentReply.get().getUserEntity().equals(userEntity)) {
                    commentOnPostReplieRepository.delete(commentReply.get());
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
        
        CommentOnPost commentOnPost = commentOnPostService.getCommentByCommentIdOnPost(commentsId);
        
        if (commentOnPost!=null) {
            if (userDetails!=null) {
                UserEntity userEntity = userServices.getUserByEmail(userDetails.getUsername());
                List<CommentRepliesDto> commentResponseDtos = convertToDto(commentOnPostReplieRepository.getCommentsReplies(commentOnPost),userEntity);
                return commentResponseDtos;
            }else {
                List<CommentRepliesDto> commentResponseDtos = convertToDto(commentOnPostReplieRepository.getCommentsReplies(commentOnPost),null);
                return commentResponseDtos;
            }
            
        }
        
        return null;
    }
    private List<CommentRepliesDto> convertToDto(List<PostCommentReply> commentsReplies,UserEntity userEntity) {
        List<CommentRepliesDto> commentResponseDtos = new ArrayList<>(); 
        commentsReplies.stream().forEach(comments ->{
                CommentRepliesDto commentResponseDto = new CommentRepliesDto();
                commentResponseDto.setCommentId(comments.getPostCommentReplyId());
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
