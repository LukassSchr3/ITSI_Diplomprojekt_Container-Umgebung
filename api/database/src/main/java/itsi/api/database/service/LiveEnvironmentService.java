package itsi.api.database.service;

import itsi.api.database.entity.LiveEnvironment;
import itsi.api.database.repository.LiveEnvironmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LiveEnvironmentService {
    @Autowired
    private LiveEnvironmentRepository repository;

    public List<LiveEnvironment> findAll() {
        return repository.findAll();
    }

    public Optional<LiveEnvironment> findById(Long id) {
        return repository.findById(id);
    }

    public Optional<LiveEnvironment> findByUserId(Long userId) {
        return repository.findByUserId(userId);
    }

    public LiveEnvironment save(LiveEnvironment env) {
        return repository.save(env);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    public void deleteByUserId(Long userId) {
        repository.deleteByUserId(userId);
    }

    public Integer getMaxVncPort() {
        return repository.findMaxVncPort();
    }
}
