package itsi.api.database.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "live_environments")
public class LiveEnvironment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", unique = true, nullable = false)
    private Long userId;

    @Column(name = "vnc_port", nullable = false)
    private Integer vncPort;

    @Column(name = "vnc_host")
    private String vncHost;

    @Column(name = "vnc_password")
    private String vncPassword;

    @Column(name = "status")
    private String status;

    // Getter und Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getVncPort() { return vncPort; }
    public void setVncPort(Integer vncPort) { this.vncPort = vncPort; }
    public String getVncHost() { return vncHost; }
    public void setVncHost(String vncHost) { this.vncHost = vncHost; }
    public String getVncPassword() { return vncPassword; }
    public void setVncPassword(String vncPassword) { this.vncPassword = vncPassword; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
