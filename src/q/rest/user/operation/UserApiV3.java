package q.rest.user.operation;

import q.rest.user.dao.DAO;
import q.rest.user.filter.annotation.UserJwt;
import q.rest.user.filter.annotation.V3ValidApp;
import q.rest.user.helper.Helper;
import q.rest.user.helper.KeyConstant;
import q.rest.user.model.contract.LoginObject;
import q.rest.user.model.contract.UserCreateHolder;
import q.rest.user.model.entity.*;
import q.rest.user.model.entity.v3.Activity;
import q.rest.user.model.entity.v3.Role;
import q.rest.user.model.entity.v3.User;

import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

@Path("/internal/api/v3/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserApiV3 {

    @EJB
    private DAO dao;


    @GET
    @UserJwt
    @Path("users")
    public Response getUsers(){
        List<User> users = dao.get(User.class);
        return Response.status(200).entity(users).build();
    }

    @GET
    @UserJwt
    @Path("activities")
    public Response getActivities(){
        List<Activity> activities = dao.get(Activity.class);
        return Response.ok().entity(activities).build();
    }


    @GET
    @UserJwt
    @Path("roles")
    public Response getRoles(){
        List<Role> roles = dao.get(Role.class);
        return Response.ok().entity(roles).build();
    }



    @POST
    @Path("role")
    @UserJwt
    public Response createGeneralRole(Role role) {
        dao.persist(role);
        return Response.status(200).entity(role).build();
    }

    @PUT
    @Path("role")
    @UserJwt
    public Response updateGeneralRole(Role role) {
        dao.update(role);
        return Response.status(200).entity(role).build();
    }

    @POST
    @Path("user")
    @UserJwt
    public Response createUser(UserCreateHolder holder){
        holder.getUser().setPassword(Helper.cypher(holder.getPassword()));
        dao.persist(holder.getUser());
        return Response.ok().build();
    }

    @PUT
    @Path("user")
    @UserJwt
    public Response updateGeneralRole(User user) {
        User orig = dao.find(User.class, user.getId());
        user.setPassword(orig.getPassword());
        dao.update(user);
        return Response.status(200).entity(user).build();
    }

    @POST
    @Path("login")
    @V3ValidApp
    public Response login(@HeaderParam(HttpHeaders.AUTHORIZATION) String header, Map<String, String> map) {
        WebApp webApp = getWebAppFromAuthHeader(header);
        String password = Helper.cypher(map.get("password"));
        String email = map.get("username").trim().toLowerCase();
        String sql = "select b from User b where b.status = :value0 and b.username = :value1 and b.password = :value2";
        User user = dao.findJPQLParams(User.class, sql, 'A', email, password);
        verifyNotNull(user);
        LoginObject loginObject = getLoginObject(user, webApp.getAppCode());
        return Response.status(200).entity(loginObject).build();
    }



    private WebApp getWebAppFromAuthHeader(String authHeader) {
        try {
            String appSecret = authHeader.substring("Bearer".length()).trim();
            return getWebAppFromSecret(appSecret);
        } catch (Exception ex) {
            throwError(401, "invalid secret");
            return null;
        }
    }


    // retrieves app object from app secret
    private WebApp getWebAppFromSecret(String secret) throws Exception {
        // verify web app secret
        WebApp webApp = dao.findTwoConditions(WebApp.class, "appSecret", "active", secret, true);
        if (webApp == null) {
            throw new Exception();
        }
        return webApp;
    }


    public void throwError(int code) {
        throwError(code, null);
    }

    public void throwError(int code, String msg) {
        throw new WebApplicationException(
                Response.status(code).entity(msg).build()
        );
    }
    private void verifyNotNull(Object subscriber) {
        if (subscriber == null) {
            throwError(404, "Invalid");
        }
    }

    private void verifyNotNull(Object subscriber, String message) {
        if (subscriber == null) {
            throwError(404, message);
        }
    }

    private void verifyNotNull(Object subscriber, int code, String message) {
        if (subscriber == null) {
            throwError(code, message);
        }
    }

    private LoginObject getLoginObject(User user, int appCode) {
        String jwt = issueToken(user.getId(), appCode);
        return new LoginObject(user, jwt);
    }


    private String issueToken(int userId, int appCode) {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("typ", 'U');
            map.put("appCode", appCode);
            return KeyConstant.issueToken(userId, map);
        } catch (Exception ex) {
            throwError(500, "Token issuing error");
            return null;
        }
    }
}
