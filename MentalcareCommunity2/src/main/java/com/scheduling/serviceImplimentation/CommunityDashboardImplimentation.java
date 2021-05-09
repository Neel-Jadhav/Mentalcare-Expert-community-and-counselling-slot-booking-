package com.scheduling.serviceImplimentation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.scheduling.dto.AnswerDto;
import com.scheduling.dto.AnswerResponseDto;
import com.scheduling.dto.FeedsDto;
import com.scheduling.dto.PostResponseDto;
import com.scheduling.dto.QuestionDto;
import com.scheduling.dto.QuestionResponseDto;
import com.scheduling.dto.UserProfileDto;
import com.scheduling.dto.UserResponseDto;
import com.scheduling.model.Answer;
import com.scheduling.model.FileList;
import com.scheduling.model.Follow;
import com.scheduling.model.LikeAnswer;
import com.scheduling.model.Post;
import com.scheduling.model.PostLike;
import com.scheduling.model.Question;
import com.scheduling.model.UserEntity;
import com.scheduling.services.AnswerService;
import com.scheduling.services.CommunityDashboardService;
import com.scheduling.services.PostService;
import com.scheduling.services.QuestionService;
import com.scheduling.services.UserServices;

@Service
public class CommunityDashboardImplimentation implements CommunityDashboardService{

    
    @Autowired
    private PostService postService;
    
    @Autowired
    private UserServices userServices;

    @Autowired
    private QuestionService questionService;
    
    @Autowired
    private AnswerService answerService;
    
    @Override
    public Set<FeedsDto> getFeeds(UserDetails userDetails,Integer pageNo) {
        try {
            Set<FeedsDto> feedsDtos = new HashSet<>();
                 if (userDetails!=null) {
                     UserEntity userEntity = userServices.getUserByEmail(userDetails.getUsername());
                     Set<PostResponseDto> postResponseDtos = postService.getPostForFeeds(userEntity,pageNo);
                     Set<QuestionResponseDto> questionResponseDtos = questionService.getQuestionFeed(userEntity,pageNo);
                     Set<AnswerResponseDto> answerResponseDtos = answerService.getAnswerFeeds(userEntity,pageNo);
                     
                     postResponseDtos.stream().forEach(post ->{
                         FeedsDto feedsDto = new FeedsDto();
                         feedsDto.setPostResponseDto(post);
                         feedsDto.setFlag(3);
                         feedsDtos.add(feedsDto);
                     });
                     
                     questionResponseDtos.stream().forEach(question -> {
                         FeedsDto feedsDto = new FeedsDto();
                         feedsDto.setQuestionResponseDto(question);
                         feedsDto.setFlag(1);
                         feedsDtos.add(feedsDto);
                     });
                     
                     answerResponseDtos.stream().forEach(answer -> {
                         FeedsDto feedsDto = new FeedsDto();
                         feedsDto.setAnswerResponseDto(answer);
                         feedsDto.setFlag(2);
                         feedsDtos.add(feedsDto);
                     });
                     
                 return feedsDtos;
                }else {
                    Set<PostResponseDto> postResponseDtos = postService.getPostForFeeds(null,pageNo);
                    Set<QuestionResponseDto> questionResponseDtos = questionService.getQuestionFeed(null,pageNo);
                    Set<AnswerResponseDto> answerResponseDtos = answerService.getAnswerFeeds(null,pageNo);
                    
                    postResponseDtos.stream().forEach(post ->{
                        FeedsDto feedsDto = new FeedsDto();
                        feedsDto.setPostResponseDto(post);
                        feedsDto.setFlag(3);
                        feedsDtos.add(feedsDto);
                    });
                    
                    questionResponseDtos.stream().forEach(question -> {
                        FeedsDto feedsDto = new FeedsDto();
                        feedsDto.setQuestionResponseDto(question);
                        feedsDto.setFlag(1);
                        feedsDtos.add(feedsDto);
                    });
                    
                    answerResponseDtos.stream().forEach(answer -> {
                        FeedsDto feedsDto = new FeedsDto();
                        feedsDto.setAnswerResponseDto(answer);
                        feedsDto.setFlag(2);
                        feedsDtos.add(feedsDto);
                    });
                    
                return feedsDtos;
                }
                
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new HashSet<>();
    }

    @Override
    public UserProfileDto getuserProfile(Long userId,UserDetails userDetails,int pageNo) {
       try {
        UserProfileDto profileDto = new UserProfileDto();
        UserEntity userEntity = null;
           if(userDetails!=null) {
                userEntity = userServices.getUserByEmail(userDetails.getUsername());
           }
           UserEntity profileUser = userServices.getUserByUserId(userId);
           Set<FeedsDto> feedsDtos = getUserProfileFeedsPagiation(profileUser,userEntity,pageNo); 
           profileDto.setFollowingOrNot(followingOrNot(userEntity,profileUser));
           profileDto.setUserId(profileUser.getUserId());
           profileDto.setProfilePath(profileUser.getProfilePath());
           profileDto.setUserName(profileUser.getUserName());
           profileDto.setNoPost(Long.valueOf(profileUser.getPosts().size()+profileUser.getAnswers().size()+profileUser.getQuestions().size()));
           profileDto.setNoFollower(Long.valueOf(profileUser.getFollowers().size()));
           profileDto.setNoFollowing(Long.valueOf(profileUser.getFollowing().size()));
           profileDto.setFeedsDtos(feedsDtos);
           return profileDto;
       } catch (Exception e) {
        e.printStackTrace();
    }
      
       return new UserProfileDto();
    }

//    private Set<FeedsDto> getUserProfileFeeds(UserEntity profileUser,UserEntity userEntity) {
//        Set<FeedsDto> feedsDtos = new TreeSet<>();
//        profileUser.getQuestions().forEach(question -> {
//            feedsDtos.add(getQuestionFeedDtos(question)); 
//        });
//        profileUser.getPosts().forEach(post ->{
//            feedsDtos.add(getPostFeedDtos(post,userEntity));
//        });
//        profileUser.getAnswers().forEach(answer -> {
//            feedsDtos.add(getAnswerFeeds(answer,userEntity));
//        });
//        
//        return feedsDtos;
//    }
    
    private Set<FeedsDto> getUserProfileFeedsPagiation(UserEntity profileUser,UserEntity userEntity,int pageNo) {
        Set<FeedsDto> feedsDtos = new TreeSet<>();
        List<Question> questions =  questionService.getUserQuestions(profileUser,pageNo);
        List<Answer> answers = answerService.getUserAnswers(profileUser,pageNo);
        List<Post> posts = postService.getUserPosts(profileUser,pageNo);
        questions.forEach(question -> {
            feedsDtos.add(getQuestionFeedDtos(question)); 
        });
        posts.forEach(post ->{
            feedsDtos.add(getPostFeedDtos(post,userEntity));
        });
        answers.forEach(answer -> {
            feedsDtos.add(getAnswerFeeds(answer,userEntity));
        });
        return feedsDtos;
    }
    
    private FeedsDto getAnswerFeeds(Answer answer,UserEntity userEntity) {
        FeedsDto feedsDto = new FeedsDto();
        AnswerResponseDto answerResponseDto = new AnswerResponseDto();
        answerResponseDto.setAnswerId(answer.getAnswerId());
        answerResponseDto.setAnswerDto(new AnswerDto(answer.getUserAnswer()));
        answerResponseDto.setAnswerDate(answer.getAnswerDate());
        answerResponseDto.setAnswerTime(answer.getAnswerTime());
        answerResponseDto.setQuestionDto(new QuestionDto(answer.getQuestion().getQuestionId(), answer.getQuestion().getUserQuestion()));
        answerResponseDto.setNoLikes(Long.valueOf(answer.getLikeAnswers().size()));
        answerResponseDto.setNoComments(Long.valueOf(answer.getCommentOnAnswers().size()));
        answerResponseDto.setUserDto(new UserResponseDto(answer.getUserEntity().getUserId(), answer.getUserEntity().getUserName(), answer.getUserEntity().getProfilePath()));
        if (userEntity!=null) {
            answerResponseDto.setLiked(likedOrNot(answer.getLikeAnswers(),userEntity));
        }else {
            answerResponseDto.setLiked(false);
        }
        feedsDto.setAnswerResponseDto(answerResponseDto);
        feedsDto.setFlag(2);
        feedsDto.setDate(answer.getAnswerDate());
        feedsDto.setTime(answer.getAnswerTime());
        return feedsDto;
    }

    private boolean likedOrNot(List<LikeAnswer> likeAnswers, UserEntity userEntity) {
        for (LikeAnswer likeAnswer : likeAnswers) {
            if (likeAnswer.getUserEntity().equals(userEntity)) {
                return true;
            }
        }
        return false;
    }
    private FeedsDto getPostFeedDtos(Post post, UserEntity userEntity) {
        FeedsDto feedsDto = new FeedsDto();
        PostResponseDto postResponseDto = new PostResponseDto();
        if (!post.getPostCaption().equals("")) postResponseDto.setCaption(post.getPostCaption()); 
        if (post.getFileLists()!=null) postResponseDto.setFiles(getFilePaths(post.getFileLists())); 
         postResponseDto.setNoComments(Long.valueOf(post.getCommentOnPosts().size()));
         postResponseDto.setNoLikes(Long.valueOf(post.getPostLikes().size()));
         postResponseDto.setPostDate(post.getPostDate());
         postResponseDto.setPostTime(post.getPostTime());
         postResponseDto.setPostId(post.getPostId());
         postResponseDto.setUserResponseDto(new UserResponseDto(post.getUserEntity().getUserId(), post.getUserEntity().getUserName(), post.getUserEntity().getProfilePath()));
         if (userEntity!=null) {
             postResponseDto.setLiked(convertor(post.getPostLikes(),userEntity));
        }else {
            postResponseDto.setLiked(false);
        }
         feedsDto.setPostResponseDto(postResponseDto);
         feedsDto.setFlag(3);
         feedsDto.setDate(post.getPostDate());
         feedsDto.setTime(post.getPostTime());
         return feedsDto;
    }

     private boolean convertor(List<PostLike> postLikes, UserEntity userEntity) {
        for (PostLike postLike : postLikes) {
            if (postLike.getUserEntity().equals(userEntity)) {
                return true;
            }
        }
        return false;
    }

    private List<String> getFilePaths(List<FileList> fileLists) {
        List<String> list = new ArrayList<>();
        fileLists.stream().forEach(f1 -> {
            list.add(f1.getPath());
        });
        return list;
    }


    private FeedsDto getQuestionFeedDtos(Question question) {
        FeedsDto feedsDto = new FeedsDto();
        QuestionResponseDto questionResponseDto = new QuestionResponseDto();
        questionResponseDto.setQuestionId(question.getQuestionId());
        questionResponseDto.setQuestionDto(new QuestionDto(question.getQuestionId(), question.getUserQuestion()));
        questionResponseDto.setQuestionDate(question.getQuestionDate());
        questionResponseDto.setQuestionTime(question.getQuestionTime());
        questionResponseDto.setUserResponseDto(new UserResponseDto(question.getUserEntity().getUserId(), question.getUserEntity().getUserName(), question.getUserEntity().getProfilePath()));
        questionResponseDto.setNoAnswer(Long.valueOf(question.getAnswers().size()));
        feedsDto.setQuestionResponseDto(questionResponseDto);
        feedsDto.setFlag(1);
        feedsDto.setDate(question.getQuestionDate());
        feedsDto.setTime(question.getQuestionTime());
        return feedsDto;
    }

    private boolean followingOrNot(UserEntity userEntity,UserEntity profileUser) {
//        for(Follow follow : userEntity.getFollowers()) {
//            if (follow.getFromUser().equals(profileUser)) {
//                return true;
//            }
//        }
        if(userEntity==null) return false;
        
        for(Follow follow : userEntity.getFollowing()) {
            if (follow.getToUser().equals(profileUser)) {
                return true;
            }
        }
        return false;
    }

}
