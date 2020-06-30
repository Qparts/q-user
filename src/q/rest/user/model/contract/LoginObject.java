package q.rest.user.model.contract;

import q.rest.user.model.entity.v3.User;

public class LoginObject {
    private User user;
    private String jwt;

    public LoginObject(User user, String jwt) {
        this.user = user;
        this.jwt = jwt;
    }

    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
