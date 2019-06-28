package q.rest.user.operation;

import q.rest.user.dao.DAO;
import q.rest.user.filter.SecuredUser;
import q.rest.user.filter.ValidApp;
import q.rest.user.helper.Helper;
import q.rest.user.model.contract.UserHolder;
import q.rest.user.model.contract.UserWithPassword;
import q.rest.user.model.entity.*;

import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

@Path("/internal/api/v2/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserInternalApiV2 {

    @EJB
    private DAO dao;

    @ValidApp
    @POST
    @Path("login")
    public Response login(@HeaderParam("Authorization") String header,  Map<String,Object> map) {
        try {
            WebApp webApp = getWebAppFromAuthHeader(header);
            String password = Helper.cypher((String) map.get("password"));
            String username = (String) map.get("username");
            String sql = "select b from User b where b.status = :value0 and b.username = :value1 and b.password = :value2";
            User user = dao.findJPQLParams(User.class, sql, 'A', username, password);
            if (user != null) {
                String token = issueToken(user, webApp, 500);
                String sql2 = "select b.role from UserRole b where b.user = :value0";
                List<Role> roles = dao.getJPQLParams(Role.class, sql2, user);
                UserHolder holder = new UserHolder();
                holder.setUser(user);
                holder.setRoles(roles);
                holder.setActivities(getUserActivities(user));
                holder.setToken(token);
                return Response.status(200).entity(holder).build();
            } else {
                throw new Exception();
            }
        } catch (Exception ex) {
            return Response.status(401).build();
        }
    }

    @SecuredUser
    @GET
    @Path("all-roles")
    public Response getAllRoles() {
        try {
            List<Role> roles = dao.get(Role.class);
            for (Role role : roles) {
                role.setActivityList(this.getRoleActivities(role));
            }
            return Response.status(200).entity(roles).build();
        } catch (Exception ex) {
            return Response.status(500).build();
        }
    }



    @SecuredUser
    @GET
    @Path("/all-users")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllUsers() {
        try {
            List<User> users = dao.get(User.class);
            for (User user : users) {
                user.setRolesList(getUserRoles(user));
            }
            return Response.status(200).entity(users).build();
        } catch (Exception ex) {
            return Response.status(500).build();
        }
    }

    @SecuredUser
    @GET
    @Path("/all-activities")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllActivities() {
        try {
            List<Activity> act = dao.get(Activity.class);
            return Response.status(200).entity(act).build();
        } catch (Exception ex) {
            return Response.status(500).build();
        }
    }

    @SecuredUser
    @GET
    @Path("/user/{param}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUser(@PathParam(value = "param") int userId) {
        try {
            User user = dao.find(User.class, userId);
            return Response.status(200).entity(user).build();
        } catch (Exception ex) {
            return Response.status(500).build();
        }
    }

    // idempotent
    @SecuredUser
    @POST
    @Path("/user")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createNewUser(UserWithPassword uwp) {
        try {
            List<User> users = dao.getCondition(User.class, "username", uwp.getUser().getUsername());
            if (!users.isEmpty()) {
                return Response.status(409).build();
            }
            User u = new User();
            u.setFirstName(uwp.getUser().getFirstName());
            u.setLastName(uwp.getUser().getLastName());
            u.setPassword(Helper.cypher(uwp.getPassword()));
            u.setStatus(uwp.getUser().getStatus());
            u.setUsername(uwp.getUser().getUsername());
            u = dao.persistAndReturn(u);

            for (Role r : uwp.getUser().getRolesList()) {
                String sql = "insert into usr_user_role (user_id, role_id) values(" + u.getId() + "," + r.getId() + ")";
                dao.insertNative(sql);
            }
            return Response.status(200).build();
        } catch (Exception ex) {
            return Response.status(500).build();
        }
    }


    // idempotent
    @SecuredUser
    @POST
    @Path("/role")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createRole(Role role) {
        try {
            List<Role> roles = dao.getCondition(Role.class, "name", role.getName());
            if (!roles.isEmpty()) {
                return Response.status(409).build();
            }

            Role r = new Role();
            r.setName(role.getName());
            r.setNameAr(role.getNameAr());
            r.setStatus(role.getStatus());
            r = dao.persistAndReturn(r);
            for (Activity act : role.getActivityList()) {
                if (act.isAccess()) {
                    String sql = "insert into usr_role_activity (role_id, activity_id) values" + "(" + r.getId() + ","
                            + act.getId() + ") on conflict do nothing";
                    dao.insertNative(sql);
                }
            }
            return Response.status(200).build();
        } catch (Exception ex) {
            return Response.status(500).build();
        }
    }


    @SecuredUser
    @PUT
    @Path("/user")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateUser(User user) {
        try {
            String sql = "update usr_user set " + " first_name = '" + user.getFirstName() + "'" + " , last_name = '"
                    + user.getLastName() + "'" + " , username = '" + user.getUsername() + "'" + " , status = '"
                    + user.getStatus() + "'" + " WHERE id = " + user.getId();
            dao.updateNative(sql);

            String sql2 = "delete from usr_user_role where user_id = " + user.getId();
            dao.updateNative(sql2);
            for (Role role : user.getRolesList()) {
                String sql3 = "insert into usr_user_role (role_id, user_id) values (" + role.getId() + ", "
                        + user.getId() + ")";
                dao.updateNative(sql3);
            }
            return Response.status(200).build();
        } catch (Exception ex) {
            return Response.status(500).build();
        }

    }

    @SecuredUser
    @PUT
    @Path("/role")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateRole(Role role) {
        try {
            List<Activity> all = role.getActivityList();
            for (Activity activity : all) {
                if (activity.isAccess()) {
                    String sql = "insert into usr_role_activity(role_id, activity_id) values " + "(" + role.getId()
                            + "," + activity.getId() + ") on conflict do nothing";
                    dao.insertNative(sql);
                } else {
                    String sql = "delete from usr_role_activity where role_id = " + role.getId() + " and activity_id = "
                            + activity.getId();
                    dao.updateNative(sql);
                }
            }

            dao.update(role);
            return Response.status(200).build();
        } catch (Exception ex) {
            return Response.status(500).build();
        }
    }


    @SecuredUser
    @GET
    @Path("current-quoting-score/user/{user-id}")
    public Response getFinderScore(@PathParam(value="user-id") Integer userId) {
        try {
            Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            String jpql = "select sum(b.score) from UserQuotingScore b where b.createdBy = :value0 and b.created between :value1 and :value2 and b.score > :value3";
            Long pos = dao.findJPQLParams(Long.class, jpql, userId, Helper.getDateStartOfMonth(year, month), Helper.getDateEndOfMonth(year, month), 0);
            jpql = "select sum(b.score) from UserQuotingScore b where b.createdBy = :value0 and b.created between :value1 and :value2 and b.score < : value3";
            Long neg = dao.findJPQLParams(Long.class, jpql, userId, Helper.getDateStartOfMonth(year, month), Helper.getDateEndOfMonth(year, month), 0);
            Map<String,Long> map = new HashMap<>();
            map.put("positive", pos == null ? 0 : pos);
            map.put("negative", neg == null ? 0 : neg);
            return Response.status(200).entity(map).build();
        }catch(Exception ex) {
            ex.printStackTrace();
            return Response.status(500).build();
        }
    }

    @SecuredUser
    @GET
    @Path("quoting-make-ids/user/{param}")
    public Response getFinderMakes(@PathParam(value = "param") int userId) {
        try {
            String jpql = "select b.makeId from UserQuotingMake b where b.user.id = :value0 order by b.created asc";
            List<Integer> makeIds = dao.getJPQLParams(Integer.class, jpql, userId);
            return Response.status(200).entity(makeIds).build();
        } catch (Exception ex) {
            return Response.status(500).build();
        }
    }

    @SecuredUser
    @POST
    @Path("quoting-score")
    public Response createFinderScore(Map<String,Object> map) {
        try {
            Long quotationId = ((Number) map.get("quotationId")).longValue();
            Long billResponseId = ((Number) map.get("billResponseId")).longValue();
            Integer score = ((Number) map.get("score")).intValue();
            Integer createdBy = ((Number) map.get("createdBy")).intValue();
            String stage = (String) map.get("stage");
            String desc = (String) map.get("desc");

            UserQuotingScore fc = new UserQuotingScore();
            fc.setQuotationId(quotationId);
            fc.setCreated(new Date());
            fc.setDesc(desc);
            fc.setBillResponseId(billResponseId == 0 ? null : billResponseId);
            fc.setScore(score);
            fc.setStage(stage);
            fc.setCreatedBy(createdBy);
            dao.persist(fc);
            return Response.status(201).build();
        }catch(Exception ex) {
            return Response.status(500).build();
        }
    }

    @POST
    @Path("/match-token/ws")
    public Response matchTokenWs(Map<String, Object> map) {
        try {
            String token = ((String) map.get("token"));
            Integer userId = ((Number) map.get("userId")).intValue();
            String jpql = "select b from AccessToken b where b.userId = :value0 and b.status = :value1 and b.token = :value2 and b.expire > :value3";
            List<AccessToken> l = dao.getJPQLParams(AccessToken.class, jpql, userId, 'A', token, new Date());
            if (!l.isEmpty()) {
                return Response.status(200).build();
            } else {
                throw new Exception();
            }
        }catch(Exception ex) {
            return Response.status(403).build();// unauthorized
        }
    }

    @SecuredUser
    @GET
    @Path("web-apps")
    public Response getWebApps(){
        try{
            List<WebApp> webApps = dao.get(WebApp.class);
            return Response.status(200).entity(webApps).build();
        }catch (Exception ex){
            return Response.status(500).build();
        }
    }

    @SecuredUser
    @POST
    @Path("/match-token")
    public Response matchToken(Map<String,String> map) {
        try {
            String appSecret = map.get("appSecret");
            String token = map.get("token");
            Integer userId = Integer.parseInt(map.get("username"));
            WebApp webApp = getWebAppFromSecret(appSecret);

            String sql = "select b from AccessToken b where b.userId = :value0 and b.webApp = :value1 " +
                    "and b.status =:value2 and b.token =:value3 and b.expire > :value4";
            List<AccessToken> accessTokenList = dao.getJPQLParams(AccessToken.class, sql, userId, webApp, 'A', token, new Date());
            if(accessTokenList.isEmpty()){
                throw new NotAuthorizedException("Request authorization failed");
            }
            return Response.status(200).build();
        } catch (Exception e) {
            return Response.status(403).build();// unauthorized
        }
    }



    private List<Activity> getUserActivities(User user) {
        String sql = "select * from usr_activity a where a.id in ("
                + "select ra.activity_id from usr_role_activity ra where ra.role_id in ("
                + "select ur.role_id from usr_user_role ur where ur.user_id = " + user.getId() + ") ) order by a.id";
        return dao.getNative(Activity.class, sql);
    }


    private List<Activity> getRoleActivities(Role role) {
        List<Activity> allActs = dao.getOrderBy(Activity.class, "name");
        for (Activity a : allActs) {
            RoleActivity roleAct = dao.findTwoConditions(RoleActivity.class, "role", "activity", role, a);
            if (roleAct != null) {
                a.setAccess(true);
            }
        }
        return allActs;
    }

    private List<Role> getUserRoles(User user) {
        String jpql = "select b.role from UserRole b where b.user = :value0";
        List<Role> roles = dao.getJPQLParams(Role.class, jpql, user);
        for(Role role : roles) {
            role.setActivityList(getRoleActivities(role));
        }
        return roles;
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


    private WebApp getWebAppFromAuthHeader(String authHeader) throws Exception {
            String[] values = authHeader.split("&&");
            String appSecret = values[2].trim();
            return getWebAppFromSecret(appSecret);
    }

    private String issueToken(User user, WebApp webapp, int expireMinutes) {
        deactivateOldTokens(user);
        Date tokenTime = new Date();
        AccessToken accessToken = new AccessToken(user.getId(), tokenTime);
        accessToken.setWebApp(webapp);
        accessToken.setExpire(Helper.addMinutes(tokenTime, expireMinutes));
        accessToken.setStatus('A');
        accessToken.setToken(Helper.getSecuredRandom());
        dao.persist(accessToken);
        return accessToken.getToken();
    }

    private void deactivateOldTokens(User user) {
        List<AccessToken> tokens = dao.getTwoConditions(AccessToken.class, "userId", "status", user.getId(), 'A');
        for (AccessToken t : tokens) {
            t.setStatus('K');// kill old token
            dao.update(t);
        }
    }
}
