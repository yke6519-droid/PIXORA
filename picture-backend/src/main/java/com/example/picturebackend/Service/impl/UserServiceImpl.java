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
import com.example.picturebackend.Service.AvatarCheckService;
import com.example.picturebackend.Service.UserService;
import com.example.picturebackend.Mapper.UserMapper;
import com.example.picturebackend.domain.MyEnums.UserLevel;
import com.example.picturebackend.domain.MyEnums.UserStatus;
import com.example.picturebackend.domain.po.AvatarCheck;
import com.example.picturebackend.domain.po.User;
import com.example.picturebackend.domain.request.user.*;
import com.example.picturebackend.domain.vo.user.UserVO;
import com.example.picturebackend.manager.CosManager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
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
    @Resource
    private AvatarCheckService avatarCheckService;
    @Resource
    private CosManager cosManager;

    /**
     * 用户注册
     * @param registerRequest
     * @return
     */
    @Override
    public Boolean userRegister(RegisterRequest registerRequest) {
        // 1. 请求体判空
        ThrowExceptionUtils.throwIF(ObjectUtil.isNull(registerRequest),
                ErrorCode.PARAMS_ERROR
        );

        // 2. 先读取字段，再统一校验必填项，避免缺字段时调用方法导致 NPE
        String account = registerRequest.getUseraccount();
        String password = registerRequest.getUserpassword();
        String rePassword = registerRequest.getReUserPassword();
        String username = registerRequest.getUsername();
        String phone = registerRequest.getPhone();
        Integer gender = registerRequest.getGender();

        ThrowExceptionUtils.throwIF(
                StrUtil.isBlank(account)
                        || StrUtil.isBlank(password)
                        || StrUtil.isBlank(rePassword)
                        || StrUtil.isBlank(username)
                        || StrUtil.isBlank(phone)
                        || ObjectUtil.isNull(gender),
                ErrorCode.PARAMS_ERROR,
                "账号、用户名、密码、确认密码、手机号和性别不能为空"
        );

        // 3. 校验字段长度和手机号格式
        ThrowExceptionUtils.throwIF(
                account.length() < 6 || account.length() > 20
                        || password.length() < 6 || password.length() > 20
                        || rePassword.length() < 6 || rePassword.length() > 20
                        || username.length() > 10
                        || !phone.matches("^1[3-9]\\d{9}$"),
                ErrorCode.PARAMS_ERROR,
                "用户名长度限制在1-10位，账号密码限制在6-20位，手机号格式不正确"
        );

        // 4. 性别沿用当前接口约定：0、1、2 为合法值
        ThrowExceptionUtils.throwIF(
                gender < 0 || gender > 2,
                ErrorCode.PARAMS_ERROR,
                "性别超出限定范围"
        );

        // 5. 校验两次密码一致性
        ThrowExceptionUtils.throwIF(!password.equals(rePassword),
                ErrorCode.PARAMS_ERROR,"两次密码输入不一致"
        );

        // 6. 参数通过后，再查询账号是否已经存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<User>()
                .eq("userAccount", account);
        ThrowExceptionUtils.throwIF(
                this.getOne(queryWrapper) != null,
                ErrorCode.PARAMS_ERROR,
                "当前账号已存在!"
        );

        // 7. 给密码加密
        String encryptPassword = this.passwordEncrypt(password);
        registerRequest.setUserpassword(encryptPassword);
        User user = new User();
        BeanUtils.copyProperties(registerRequest,user);

        // 8. 注册用户统一从后端写入默认等级和正常状态，避免依赖不同数据库环境的默认值。
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
        
        
        QueryWrapper<User> queryUserWrapper = new QueryWrapper<User>()
                .eq("userAccount",userLoginRequest.getUseraccount());

        User user = this.getOne(queryUserWrapper);
        System.out.println("----------------------------user = " + user);

        // 判断当前用户是否被封禁
        ThrowExceptionUtils.throwIF(
                user != null && UserStatus.BANNED.getValue().equals(user.getUserStatus()),
                ErrorCode.FORBIDDEN_ERROR,
                "账号已被封禁"
        );

        //3. 将脱敏后的用户信息存入Session
        UserVO saftyUser = this.getSaftyUser(user);
        System.out.println("----------------------------saftyUser = " + saftyUser);
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
        UserVO currentUser = (UserVO)request.getSession().getAttribute(UserConstant.CURRENT_USER_SESSION_KEY);

        ThrowExceptionUtils.throwIF(
                currentUser == null || currentUser.getId() == null,
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
        // 资料接口只允许更新资料字段，头像由独立的头像上传/审核流程维护。
        User user = new User();
        user.setId(id);
        user.setUsername(updateSelfRequest.getUsername());
        user.setGender(updateSelfRequest.getGender());
        user.setPhone(updateSelfRequest.getPhone());
        user.setEmail(updateSelfRequest.getEmail());
        user.setProfile(updateSelfRequest.getProfile());
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
    public UserVO getSaftyUser(User user) {
        ThrowExceptionUtils.throwIF(ObjectUtil.isNull(user),ErrorCode.PARAMS_ERROR,"传入用户为空");

        UserVO saftyUser = new UserVO();

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
        
        saftyUser.setSpaceId(user.getSpaceId());
        return saftyUser;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean adminCheckAvatar(AdminCheckAvatarRequest adminCheckAvatarRequest, User currentUser){
        
        String checkMessage = adminCheckAvatarRequest.getCheckMessage();
        Integer checkResult = adminCheckAvatarRequest.getCheckResult();
        Long userId = adminCheckAvatarRequest.getUserId();

        // 拿到该用户对应的审核记录 - 最近一次的
        AvatarCheck avatarCheck = avatarCheckService.getOne(new QueryWrapper<AvatarCheck>()
                        .eq("userId", userId)
                        .orderByDesc("updateTime")
                        .last("LIMIT 1"));
        ThrowExceptionUtils.throwIF(avatarCheck == null,
                ErrorCode.NOT_FOUND_ERROR, "当前用户没有待审核头像");
        ThrowExceptionUtils.throwIF(!Integer.valueOf(0).equals(avatarCheck.getStatus()),
                ErrorCode.PARAMS_ERROR,"仅能修改待审核头像"
            );

        if (checkResult == 1) {
            
            // 审核通过，修改状态并更新
            avatarCheck.setStatus(checkResult);
            avatarCheck.setCheckMessage("审核通过~");
            boolean reviewUpdated = avatarCheckService.updateById(avatarCheck);
            ThrowExceptionUtils.throwIF(!reviewUpdated,
                    ErrorCode.OPERATION_ERROR, "头像审核状态更新失败");
            // 修改用户头像
            User user = this.getById(userId);
            ThrowExceptionUtils.throwIF(user == null,
                    ErrorCode.NOT_FOUND_ERROR, "用户不存在");
            user.setAvatarurl(avatarCheck.getUrl());
            boolean userUpdated = this.updateById(user);
            ThrowExceptionUtils.throwIF(!userUpdated,
                    ErrorCode.OPERATION_ERROR, "用户头像更新失败");

        }else if (checkResult == 2) {
            // 审核不通过
            avatarCheck.setStatus(checkResult);
            avatarCheck.setCheckMessage(checkMessage);
            boolean reviewUpdated = avatarCheckService.updateById(avatarCheck);
            ThrowExceptionUtils.throwIF(!reviewUpdated,
                    ErrorCode.OPERATION_ERROR, "头像审核状态更新失败");
            // 用户头像保持原样
            deleteRejectedAvatar(avatarCheck.getUrl());
        } else {
            ThrowExceptionUtils.throwIF(true,
                    ErrorCode.PARAMS_ERROR, "审核结果只能是通过或拒绝");
        }

        return true;
    }

    /**
     * 拒绝审核后删除候选头像，当前 COS URL 由上传接口按“域名 + objectKey”生成。
     */
    private void deleteRejectedAvatar(String avatarUrl) {
        if (StrUtil.isBlank(avatarUrl)) {
            log.warn("拒绝头像缺少 COS URL，跳过对象清理");
            return;
        }

        try {
            String path = URI.create(avatarUrl).getPath();
            String objectKey = path != null && path.startsWith("/")
                    ? path.substring(1)
                    : path;
            if (StrUtil.isBlank(objectKey)) {
                log.warn("无法从头像 URL 提取 COS Key，url = {}", avatarUrl);
                return;
            }
            cosManager.deleteObject(objectKey);
        } catch (IllegalArgumentException exception) {
            log.warn("头像 URL 格式异常，跳过 COS 对象清理，url = {}", avatarUrl, exception);
        }
    }
}




