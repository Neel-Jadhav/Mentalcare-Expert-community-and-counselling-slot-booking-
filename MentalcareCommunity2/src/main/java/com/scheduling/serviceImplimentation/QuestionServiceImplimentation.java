package com.scheduling.serviceImplimentation;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.scheduling.dto.AnswerDto;
import com.scheduling.dto.AnswerResponseDto;
import com.scheduling.dto.QuestionDto;
import com.scheduling.dto.QuestionResponseDto;
import com.scheduling.dto.UserResponseDto;
import com.scheduling.model.Answer;
import com.scheduling.model.Follow;
import com.scheduling.model.LikeAnswer;
import com.scheduling.model.Question;
import com.scheduling.model.UserEntity;
import com.scheduling.repository.QuestionRepository;
import com.scheduling.services.QuestionService;
import com.scheduling.services.UserServices;

@Service
public class QuestionServiceImplimentation implements QuestionService {

    @Autowired
    private UserServices userservice;
    
    @Autowired
    private QuestionRepository questionRepository;
    
    @Override
    public QuestionResponseDto createQuestion(QuestionDto questionDto, String username) {
        
        try {
            Question question = new Question();
            question.setUserQuestion(questionDto.getQuestion());
            question.setUserEntity(userservice.getUserByEmail(username));
            question.setQuestionTime(Time.valueOf(LocalTime.now()));
            question.setQuestionDate(Date.valueOf(LocalDate.now()));
            Question saved = questionRepository.save(question);
            return convertor(saved);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private QuestionResponseDto convertor(Question saved) {
        QuestionResponseDto questionResponseDto = new QuestionResponseDto();
        questionResponseDto.setQuestionId(saved.getQuestionId());
        questionResponseDto.setAnswerDtos(new ArrayList<>());
        questionResponseDto.setQuestionDate(saved.getQuestionDate());
        questionResponseDto.setQuestionTime(saved.getQuestionTime());
        questionResponseDto.setQuestionDto(new QuestionDto(saved.getQuestionId(), saved.getUserQuestion()));
        questionResponseDto.setUserResponseDto(new UserResponseDto(saved.getUserEntity().getUserId(), saved.getUserEntity().getUserName(), saved.getUserEntity().getProfilePath()));
        return questionResponseDto;
    }

    @Override
    public boolean checkQuestionUser(String username, long questionId) {
        
        UserEntity userEntity  = userservice.getUserByEmail(username);
        
        Optional<Question> question = questionRepository.findById(questionId);
        
        if (userEntity!=null && question.isPresent()) {
            if (question.get().getUserEntity().equals(userEntity)) {
                questionRepository.delete(question.get());
                return true;
            }
        }
        
        return false;
    }

    @Override
    public QuestionResponseDto getQuestionByIdDto(long questionId,UserDetails userDetails) {
        
        try {
            Optional<Question> userQuestion = questionRepository.findById(questionId);
            
            if (userQuestion.isPresent()) {
                Question question = userQuestion.get();
                QuestionResponseDto questionResponseDto = new QuestionResponseDto();
                questionResponseDto.setQuestionId(question.getQuestionId());
                questionResponseDto.setQuestionDto(new QuestionDto(question.getQuestionId(),question.getUserQuestion()));
                questionResponseDto.setQuestionDate(question.getQuestionDate());
                questionResponseDto.setQuestionTime(question.getQuestionTime());
                questionResponseDto.setAnswerDtos(getListAnswerDto(question.getAnswers(),userDetails));
                questionResponseDto.setUserResponseDto(convertUserToUserDto(question.getUserEntity(),userDetails));
                questionResponseDto.setNoAnswer(Long.valueOf(question.getAnswers().size()));
                return questionResponseDto;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
            return null;
    }

    private List<AnswerResponseDto> getListAnswerDto(List<Answer> answers,UserDetails userDetails) {
        
        List<AnswerResponseDto> answerResponseDtos = new ArrayList<>();
        
        answers.stream().forEach(answer -> {
            AnswerResponseDto answerResponseDto = new AnswerResponseDto();
            answerResponseDto.setAnswerDto(new AnswerDto(answer.getAnswerId(),answer.getUserAnswer()));
            answerResponseDto.setAnswerId(answer.getAnswerId());
            answerResponseDto.setAnswerDate(answer.getAnswerDate());
            answerResponseDto.setAnswerTime(answer.getAnswerTime());
            answerResponseDto.setQuestionDto(new QuestionDto(answer.getQuestion().getQuestionId(),answer.getQuestion().getUserQuestion()));
            answerResponseDto.setNoLikes(Long.valueOf(answer.getLikeAnswers().size()));
            answerResponseDto.setNoComments(Long.valueOf(answer.getCommentOnAnswers().size()));
            answerResponseDto.setUserDto(new UserResponseDto(answer.getUserEntity().getUserId(), answer.getUserEntity().getUserName(), answer.getUserEntity().getProfilePath()));
            if(userDetails!=null) {
                UserEntity userByEmail = userservice.getUserByEmail(userDetails.getUsername());
                answerResponseDto.setLiked(userLikedOrNot(answer,userByEmail));
            }else {
                answerResponseDto.setLiked(false);
            }
            answerResponseDtos.add(answerResponseDto);
        });
        
        return answerResponseDtos;
    }

    private boolean userLikedOrNot(Answer answer, UserEntity userByEmail) {
        for (LikeAnswer likeanswer : answer.getLikeAnswers()) {
             if (likeanswer.getUserEntity().equals(userByEmail)) {
                return true;
            }
        }
        return false;
    }

    private UserResponseDto convertUserToUserDto(UserEntity userEntity,UserDetails userDetails) {
        UserResponseDto userResponseDto = new UserResponseDto();
        userResponseDto.setUsername(userEntity.getUserName());
        userResponseDto.setProfilePath(userEntity.getProfilePath());
        userResponseDto.setUserId(userEntity.getUserId());
        if (userDetails!=null) {
            userResponseDto.setFollowOrNot(followOrNot(userEntity,userDetails));
        }else {
            userResponseDto.setFollowOrNot(false);
        }
        return userResponseDto;
    }

    private boolean followOrNot(UserEntity userEntity, UserDetails userDetails) {
        for (Follow follow : userservice.getUserByEmail(userDetails.getUsername()).getFollowing()) {
             if (follow.getToUser().equals(userEntity)) {
                return true;
            }
        }    
        return false;
    }

    @Override
    public List<QuestionResponseDto> getQuestionList() {
        
        try {
            List<QuestionResponseDto> questionResponseDtos = new ArrayList<>();
            
            List<Question> questions = questionRepository.findAll(Sort.by("questionTime"));
            
            questions.stream().forEach(question -> {
                QuestionResponseDto questionResponseDto = new QuestionResponseDto();
                questionResponseDto.setQuestionId(question.getQuestionId());
                questionResponseDto.setQuestionDto(new QuestionDto(question.getQuestionId(),question.getUserQuestion()));
                questionResponseDto.setQuestionDate(question.getQuestionDate());
                questionResponseDto.setQuestionTime(question.getQuestionTime());
                questionResponseDto.setAnswerDtos(getFirstAnswerDto(question.getAnswers()));
                questionResponseDto.setUserResponseDto(convertUserToUserDto(question.getUserEntity(),null));
                questionResponseDtos.add(questionResponseDto);
            });
            return questionResponseDtos;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<AnswerResponseDto> getFirstAnswerDto(List<Answer> answers) {
        
        List<AnswerResponseDto> answerResponseDtos = new ArrayList<>();
        
        if (answers==null) {
            return null;
        }
        Answer answer = answers.get(0);
        answerResponseDtos.add(new AnswerResponseDto(new QuestionDto(answer.getQuestion().getQuestionId(), answer.getQuestion().getUserQuestion()),new AnswerDto(answer.getUserAnswer()),convertUserToUserDto(answer.getUserEntity(),null), answer.getAnswerTime(),answer.getAnswerDate()));
     
        return answerResponseDtos;
    }

    @Override
    public Question getQuestionById(long qid) {
        Optional<Question> question = questionRepository.findById(qid);
        if (question.isPresent()) {
            return question.get();
        }
        return null;
    }

    @Override
    public Set<QuestionResponseDto> getQuestionFeed(UserEntity userEntity, Integer pageNo) {
        try {
            Set<QuestionResponseDto> questionResponseDtos = new HashSet<>();
            Pageable pageable = PageRequest.of(pageNo-1, 2,Sort.by("questionTime"));
                List<Question> content = questionRepository.getFeedQuestions(pageable).getContent();
                content.stream().forEach(question -> {
                    QuestionResponseDto questionResponseDto = new QuestionResponseDto();
                    questionResponseDto.setQuestionId(question.getQuestionId());
                    questionResponseDto.setQuestionDto(new QuestionDto(question.getQuestionId(), question.getUserQuestion()));
                    questionResponseDto.setQuestionDate(question.getQuestionDate());
                    questionResponseDto.setQuestionTime(question.getQuestionTime());
                    questionResponseDto.setUserResponseDto(new UserResponseDto(question.getUserEntity().getUserId(), question.getUserEntity().getUserName(), question.getUserEntity().getProfilePath(),followOrNot(userEntity, question.getUserEntity())));
                    questionResponseDto.setNoAnswer(Long.valueOf(question.getAnswers().size()));
                    questionResponseDtos.add(questionResponseDto);
                });
            return questionResponseDtos;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return new HashSet<>();
    }

    private boolean followOrNot(UserEntity loginUser, UserEntity postUser) {
        if(loginUser==null) return false;
        for (Follow follow : loginUser.getFollowing()) {
            if (follow.getToUser().equals(postUser)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public List<Question> getUserQuestions(UserEntity profileUser,int pageNo) {
        try {
            Pageable pageable = PageRequest.of(pageNo-1, 2,Sort.by("questionDate").and(Sort.by("questionTime")));
            List<Question> questions = questionRepository.getUserQuestion(profileUser,pageable).getContent();
            return questions;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }
  

}
