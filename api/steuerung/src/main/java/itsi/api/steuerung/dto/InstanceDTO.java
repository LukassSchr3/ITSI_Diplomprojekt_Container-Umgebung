package itsi.api.steuerung.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class InstanceDTO {
    private Integer id;
    private String containerId;
    private String name;
    private ImageDTO image;
    private UserDTO user;
    private String status;
    
    // Helper methods to get imageId and userId
    public Integer getImageId() {
        return image != null ? image.getId() : null;
    }
    
    public Integer getUserId() {
        return user != null ? user.getId() : null;
    }
    
    public void setImageId(Integer imageId) {
        if (this.image == null) {
            this.image = new ImageDTO();
        }
        this.image.setId(imageId);
    }
    
    public void setUserId(Integer userId) {
        if (this.user == null) {
            this.user = new UserDTO();
        }
        this.user.setId(userId);
    }
}
