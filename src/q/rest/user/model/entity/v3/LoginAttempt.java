package q.rest.user.model.entity.v3;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Table(name="usr_login_attempt")
@Entity
public class LoginAttempt implements Serializable {
    @Id
    @SequenceGenerator(name = "usr_login_attempt_id_seq_gen", sequenceName = "usr_login_attempt_id_seq", initialValue=1, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "usr_login_attempt_id_seq_gen")
    private int id;
    private String username;
    private boolean success;
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;
    private String ip;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
