package itsi.api.database.repository;

import itsi.api.database.entity.QuestionResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionResultRepository extends JpaRepository<QuestionResult, Integer> {
    List<QuestionResult> findByUserId(Integer userId);
    List<QuestionResult> findByQuestionId(Integer questionId);
    Optional<QuestionResult> findByUserIdAndQuestionId(Integer userId, Integer questionId);
    List<QuestionResult> findByUserIdAndBestanden(Integer userId, Boolean bestanden);
}
