package itsi.api.database.service;

import itsi.api.database.entity.QuestionResult;
import itsi.api.database.repository.QuestionResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class QuestionResultService {

    private final QuestionResultRepository questionResultRepository;

    public List<QuestionResult> findAll() {
        return questionResultRepository.findAll();
    }

    public Optional<QuestionResult> findById(Integer id) {
        return questionResultRepository.findById(id);
    }

    public List<QuestionResult> findByUserId(Integer userId) {
        return questionResultRepository.findByUserId(userId);
    }

    public List<QuestionResult> findByQuestionId(Integer questionId) {
        return questionResultRepository.findByQuestionId(questionId);
    }

    public Optional<QuestionResult> findByUserIdAndQuestionId(Integer userId, Integer questionId) {
        return questionResultRepository.findByUserIdAndQuestionId(userId, questionId);
    }

    public List<QuestionResult> findByUserIdAndBestanden(Integer userId, Boolean bestanden) {
        return questionResultRepository.findByUserIdAndBestanden(userId, bestanden);
    }

    public QuestionResult save(QuestionResult questionResult) {
        return questionResultRepository.save(questionResult);
    }

    public void deleteById(Integer id) {
        questionResultRepository.deleteById(id);
    }
}
