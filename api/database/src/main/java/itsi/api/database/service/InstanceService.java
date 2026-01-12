package itsi.api.database.service;

import itsi.api.database.entity.Instance;
import itsi.api.database.repository.InstanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InstanceService {

    private final InstanceRepository instanceRepository;

    public List<Instance> findAll() {
        return instanceRepository.findAll();
    }

    public Optional<Instance> findById(Integer id) {
        return instanceRepository.findById(id);
    }

    public Optional<Instance> findByContainerId(String containerId) {
        return instanceRepository.findByContainerId(containerId);
    }

    public Optional<Instance> findByName(String name) {
        return instanceRepository.findByName(name);
    }

    public List<Instance> findByUserId(Integer userId) {
        return instanceRepository.findByUserId(userId);
    }

    public List<Instance> findByImageId(Integer imageId) {
        return instanceRepository.findByImageId(imageId);
    }

    public List<Instance> findByStatus(String status) {
        return instanceRepository.findByStatus(status);
    }

    public Instance save(Instance instance) {
        return instanceRepository.save(instance);
    }

    public void deleteById(Integer id) {
        instanceRepository.deleteById(id);
    }

    // Größte Container-ID ermitteln
    public Optional<String> findMaxContainerId() {
        return instanceRepository.findTopByOrderByContainerIdDesc()
                .map(Instance::getContainerId);
    }
}
