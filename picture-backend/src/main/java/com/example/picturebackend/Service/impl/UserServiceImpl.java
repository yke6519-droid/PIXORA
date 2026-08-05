package com.example.picturebackend.Service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.picturebackend.Exception.ErrorCode;
import com.example.picturebackend.Exception.ThrowExceptionUtils;
import com.example.picturebackend.constant.UserConstant;
import com.example.picturebackend.Service.UserService;
import com.example.picturebackend.Mapper.UserMapper;
import com.example.picturebackend.domain.MyEnums.UserLevel;
import com.example.picturebackend.domain.MyEnums.UserStatus;
import com.example.picturebackend.domain.po.User;
import com.example.picturebackend.domain.request.user.*;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;


/**
* @author chen
* @description 针对表【user】的数据库操作Service实现
* @createDate 2026-04-21 15:38:23
*/
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService{
    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 用户注册
     * @param registerRequest
     * @return
     */
    @Override
    public Boolean userRegister(RegisterRequest registerRequest) {
        //2. 判断当前账号是否已经存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<User>()
                .eq("userAccount", registerRequest.getUseraccount());
        ThrowExceptionUtils.throwIF(
                this.getOne(queryWrapper)!= null,
                ErrorCode.PARAMS_ERROR,
                "当前账号已存在!"
        );
        String Account= registerRequest.getUseraccount();
        String Password = registerRequest.getUserpassword();
        String RePassword = registerRequest.getReUserPassword();
        String Username = registerRequest.getUsername();
        //3. 判断合法性
        ThrowExceptionUtils.throwIF(
                Account.isBlank()||Password.isBlank()||RePassword.isBlank()||Username.isBlank(),
                ErrorCode.PARAMS_ERROR
        );
        ThrowExceptionUtils.throwIF(
                Account.length()<6 || Account.length()>20||
                        Password.length()<6 || Password.length()>20||
                        RePassword.length()<6 || RePassword.length()>20||
                        Username.length()>10 || registerRequest.getPhone().length()!=11,
                ErrorCode.PARAMS_ERROR,
                "用户名长度限制在在1-10位，账号密码限制在6-20位 手机号为11位"
        );
        ThrowExceptionUtils.throwIF(
                registerRequest.getGender()<0|| registerRequest.getGender()>2,
                ErrorCode.PARAMS_ERROR,
                "性别超出限定范围"
        );
        //4. 给密码加密
        String encryptPassword = this.passwordEncrypt(Password);
        registerRequest.setUserpassword(encryptPassword);
        User user = new User();
        BeanUtils.copyProperties(registerRequest,user);
        // 注册用户统一从后端写入默认等级和正常状态，避免依赖不同数据库环境的默认值。
        user.setUserLevel(UserConstant.DEFAULT_ROLE);
        user.setUserStatus(UserStatus.NORMAL.getValue());
        return this.save(user);
    }

    @Override
    public String userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<User>()
                .eq("userAccount",userLoginRequest.getUseraccount())
                .select("userPassword");
        // userPasswordOnly : 从数据库中查出来的密码
        User userPasswordOnly = this.getOne(queryWrapper);
        ThrowExceptionUtils.throwIF(
                userPasswordOnly==null,
                ErrorCode.PARAMS_ERROR,
                "当前用户不存在"
        );
        String userPassword = userPasswordOnly.getUserpassword();
        //2. 登录校验
        ThrowExceptionUtils.throwIF(
                !userPassword.equals(
                        // 对传入的密码进行加密处理
                        this.passwordEncrypt(userLoginRequest.getUserpassword())
                ),
                ErrorCode.PARAMS_ERROR,
                "登录密码错误");
        //3. 将脱敏后的用户信息存入Session
        QueryWrapper<User> queryUserWrapper = new QueryWrapper<User>()
                .eq("userAccount",userLoginRequest.getUseraccount());
        User user = this.getOne(queryUserWrapper);
        ThrowExceptionUtils.throwIF(
                user != null && UserStatus.BANNED.getValue().equals(user.getUserStatus()),
                ErrorCode.FORBIDDEN_ERROR,
                "账号已被封禁"
        );
        User saftyUser = this.getSaftyUser(user);
        request.getSession().setAttribute(UserConstant.CURRENT_USER_SESSION_KEY,saftyUser);
        return "登录成功";
    }

    /**
     * 获取到当前用户所有信息
     * @param request
     * @return
     */
    @Override
    public User getCurrentUser(HttpServletRequest request) {
        //1. 从Session中获取到当前用户信息
        User currentUser = (User)request.getSession().getAttribute(UserConstant.CURRENT_USER_SESSION_KEY);
        System.out.println(currentUser);
        ThrowExceptionUtils.throwIF(
                currentUser == null,
                ErrorCode.NOT_LOGIN_ERROR
        );
        User latestUser = this.getById(currentUser.getId());
        ThrowExceptionUtils.throwIF(
                latestUser != null && UserStatus.BANNED.getValue().equals(latestUser.getUserStatus()),
                ErrorCode.FORBIDDEN_ERROR,
                "账号已被封禁"
        );
        return latestUser;
    }

    /**
     * 密码加密工具类
     * @param password
     * @return
     */
    @Override
    public String passwordEncrypt(String password) {
        String salt = "盐";
        return DigestUtils.md5DigestAsHex((salt + password).getBytes());
    }

    @Override
    public boolean updateSelf(Long id, HttpServletRequest request, UpdateSelfRequest updateSelfRequest) {
        // copy到user实体类中并设置id
        User user = new User();
        BeanUtils.copyProperties(updateSelfRequest,user);
        user.setId(id);
        // 更新数据库
        boolean b = this.updateById(user);
        // 更新后 将最新的当前用户数据(脱敏后)存入Session中
        request.getSession().setAttribute(UserConstant.CURRENT_USER_SESSION_KEY,this.getSaftyUser(this.getById(id)));
        return b;
    }

    @Override
    public boolean addUser(AddUserRequest addUserRequest) {
        //1. 检验当前账户是否已存在
        String Account = addUserRequest.getUseraccount();
        QueryWrapper<User> queryWrapper = new QueryWrapper<User>()
                .eq("userAccount",Account);
        ThrowExceptionUtils.throwIF(
                this.getOne(queryWrapper)!=null,
                ErrorCode.SYSTEM_ERROR,
                "当前用户已存在！"
        );
        String initPassword = "123456";
        User user = new User();
        BeanUtils.copyProperties(addUserRequest,user);
        String userLevel = StrUtil.blankToDefault(addUserRequest.getUserLevel(), UserConstant.DEFAULT_ROLE);
        ThrowExceptionUtils.throwIF(
                UserLevel.getEnumByValue(userLevel) == null,
                ErrorCode.PARAMS_ERROR,
                "用户等级只能是 user、admin 或 vip"
        );
        user.setUserLevel(userLevel);
        user.setUserStatus(UserStatus.NORMAL.getValue());
        user.setCreatetime(LocalDateTime.now());
        user.setUpdatetime(LocalDateTime.now());
        user.setUserpassword(this.passwordEncrypt(initPassword));
        return this.save(user);
    }

    @Override
    public IPage<User> queryPageByCondition(QueryPageRequest queryPageRequest) {
        QueryWrapper<User> queryWrapper = this.getQueryWrapper(queryPageRequest);
        return this.page(new Page<User>(queryPageRequest.getCurrent(), queryPageRequest.getSize()), queryWrapper);
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(QueryPageRequest queryPageRequest) {
        ThrowExceptionUtils.throwIF(
                queryPageRequest == null,
                ErrorCode.NOT_LOGIN_ERROR,
                "请求体为空"
        );
        // 对请求体使用插件allget()获取到所有的对应字段
        String sortField = queryPageRequest.getSortField();
        String sortOrder = queryPageRequest.getSortOrder();
        Long id = queryPageRequest.getId();
        String queryUsername = queryPageRequest.getQueryUsername();
        String queryUserAccount = queryPageRequest.getQueryUserAccount();
        String profile = queryPageRequest.getProfile();
        String userLevel = queryPageRequest.getUserLevel();
        Integer accountStatus = queryPageRequest.getAccountStatus();
        Integer gender = queryPageRequest.getGender();
        // 根据查询逻辑，是否合法为依据，创建queryWrapper
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjectUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userLevel), "userLevel", userLevel);
        queryWrapper.eq(ObjectUtil.isNotNull(accountStatus), "userStatus", accountStatus);
        queryWrapper.like(StrUtil.isNotBlank(queryUsername), "username", queryUsername);
        queryWrapper.like(StrUtil.isNotBlank(queryUserAccount), "userAccount", queryUserAccount);
        queryWrapper.like(StrUtil.isNotBlank(profile), "profile", profile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        queryWrapper.eq(ObjectUtil.isNotNull(gender),"gender", gender);
        return queryWrapper;
    }

    @Override
    public User getSaftyUser(User user) {
        ThrowExceptionUtils.throwIF(ObjectUtil.isNull(user),ErrorCode.PARAMS_ERROR,"传入用户为空");
        User saftyUser = new User();
        saftyUser.setId(user.getId());
        saftyUser.setUsername(user.getUsername());
        saftyUser.setUseraccount(user.getUseraccount());
        saftyUser.setAvatarurl(user.getAvatarurl());
        saftyUser.setGender(user.getGender());
        saftyUser.setEmail(user.getEmail());
        saftyUser.setPhone(user.getPhone());
        saftyUser.setProfile(user.getProfile());
        saftyUser.setCreatetime(user.getCreatetime());
        saftyUser.setUserLevel(user.getUserLevel());
        saftyUser.setUserStatus(user.getUserStatus());
        // todo 前端上传图片时，可以根据用户登录态选择是否展示单选择器
        saftyUser.setSpaceId(user.getSpaceId());
        return saftyUser;
    }

}




