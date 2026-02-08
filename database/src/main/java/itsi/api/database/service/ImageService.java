package itsi.api.database.service;

import itsi.api.database.entity.Image;
import itsi.api.database.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final ImageRepository imageRepository;

    public List<Image> findAll() {
        return imageRepository.findAll();
    }

    public Optional<Image> findById(Integer id) {
        return imageRepository.findById(id);
    }

    public Optional<Image> findByName(String name) {
        return imageRepository.findByName(name);
    }

    public Optional<Image> findByImageRef(String imageRef) {
        return imageRepository.findByImageRef(imageRef);
    }

    public Image save(Image image) {
        return imageRepository.save(image);
    }

    public void deleteById(Integer id) {
        imageRepository.deleteById(id);
    }
}

