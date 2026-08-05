package com.example.picturebackend.Controller;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.picturebackend.Exception.ErrorCode;
import com.example.picturebackend.Exception.ThrowExceptionUtils;
import com.example.picturebackend.Service.UserService;
import com.example.picturebackend.Utils.ResponseUtils;
import com.example.picturebackend.annotation.AuthCheck;
import com.example.picturebackend.constant.UserConstant;
import com.example.picturebackend.domain.MyEnums.UserLevel;
import com.example.picturebackend.domain.MyEnums.UserStatus;
import com.example.picturebackend.domain.po.User;
import com.example.picturebackend.domain.request.BaseResponse;
import com.example.picturebackend.domain.request.user.*;
import com.example.picturebackend.domain.vo.user.UserPagesVO;
import com.example.picturebackend.domain.vo.user.UserVO;

import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;

    /**
     * 用户注册
     * @param registerRequest
     * @return
     */
   @PostMapping("/register")
   public BaseResponse<Boolean> userRegister(@RequestBody RegisterRequest registerRequest){
       //1. 判空处理
       ThrowExceptionUtils.throwIF(
               registerRequest ==null,
               ErrorCode.PARAMS_ERROR
       );
       Boolean save = userService.userRegister(registerRequest);
       return ResponseUtils.success(save);
   }

    /**
     * 用户登录
     * @param userLoginRequest
     * @return
     */
   @PostMapping("/userLogin")
    public BaseResponse<String> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request){
       String Account = userLoginRequest.getUseraccount();
       String Password =userLoginRequest.getUserpassword();
       //1. 校验判空 和 长度

       ThrowExceptionUtils.throwIF(
               StrUtil.isAllBlank(Account,Password),
               ErrorCode.PARAMS_ERROR
       );
       ThrowExceptionUtils.throwIF(
               Password.length()<6 || Password.length()>20
                       || Account.length()<6 || Account.length()>20,
               ErrorCode.PARAMS_ERROR,
               "密码和账号长度限制在6-20内"
       );
       //2. 用户登录逻辑
       String loginStatus = userService.userLogin(userLoginRequest, request);
       return ResponseUtils.success(loginStatus);
   }

    /**
     * 退出登录
     * @param request
     * @return
     */
   @GetMapping("/userLogout")
    public BaseResponse<String> userLogout(HttpServletRequest request){
       request.getSession().removeAttribute(UserConstant.CURRENT_USER_SESSION_KEY);
       return ResponseUtils.success("成功退出登录");
   }

    /**
     * 获取当前登录用户信息
     * @param request
     * @return
     */
    @GetMapping("/getCurrentUser")
    public BaseResponse<UserVO> getCurrentUser(HttpServletRequest request){

        // 内部已做脱敏
        User currentUser = userService.getCurrentUser(request);
        UserVO latestUser = userService.getSaftyUser(currentUser);

        // 每次get完都要把最新的user存进去
        request.setAttribute(UserConstant.CURRENT_USER_SESSION_KEY,latestUser);
        return ResponseUtils.success(latestUser);
    }

    /**
     * 查询所有用户信息
     * 仅管理员可操作
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/queryUsers")
    public BaseResponse<List<UserVO>> getAllUsers(){
        // 查询所有用户信息，并进行脱敏处理
        List<UserVO> UserList = userService.list().stream().map(user->{
                return userService.getSaftyUser(user);
        }).collect(java.util.stream.Collectors.toList());

        return ResponseUtils.success(UserList);
    }

    /**
     * 用户分页查询
     * 仅管理员可操作
     * @param queryPageRequest
     * @return UserPagesVO
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/queryPages")
    public BaseResponse<UserPagesVO> queryPages(@RequestBody QueryPageRequest queryPageRequest){
        ThrowExceptionUtils.throwIF(
                queryPageRequest == null,
                ErrorCode.PARAMS_ERROR
        );
        IPage<User> iPage = userService.queryPageByCondition(queryPageRequest);
        
        // 将查询到的User对象列表转换为UserVO对象列表 
        List<UserVO> userVOList = iPage.getRecords().stream().map(user->{
                return userService.getSaftyUser(user);
        }).collect(java.util.stream.Collectors.toList());

        UserPagesVO userPagesVO = new UserPagesVO();
        userPagesVO.setUserList(userVOList);
        userPagesVO.setTotalSize(iPage.getTotal());

        return ResponseUtils.success(userPagesVO);
    }

    /**
     * 用户根据id查询其他用户脱敏后的信息
     * @param queryUserRequest
     * @return User
     */
    @GetMapping("/queryUserById")
    public BaseResponse<UserVO> getUserById(QueryUserRequest queryUserRequest){
        // 判空处理
        ThrowExceptionUtils.throwIF(
                queryUserRequest==null || queryUserRequest.getId() == null,
                ErrorCode.PARAMS_ERROR,
                "用户ID不能为空"
        );
        UserVO userVO = userService.getSaftyUser(userService.getById(queryUserRequest.getId()));

        // 如果用户不存在，抛出异常
        ThrowExceptionUtils.throwIF(
                userVO == null,
                ErrorCode.NOT_FOUND_ERROR,
                "用户不存在"
        );

        return ResponseUtils.success(userVO);
    }

    /**
     * 根据id更新用户信息
     * 仅管理员
     * @param updateUserRequest
     * @return Boolean
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UpdateUserRequest updateUserRequest){
         ThrowExceptionUtils.throwIF(
                 updateUserRequest==null,
                 ErrorCode.PARAMS_ERROR,
                 "更新请求体为空"
         );
         final String Username = updateUserRequest.getUsername();
         final String AvatarURL = updateUserRequest.getAvatarurl();
         final Integer gender = updateUserRequest.getGender();
         final String Phone = updateUserRequest.getPhone();
         final String Email = updateUserRequest.getEmail();
         final String Profile = updateUserRequest.getProfile();
         final String userLevel = updateUserRequest.getUserLevel();
         final Integer accountStatus = updateUserRequest.getAccountStatus();
         ThrowExceptionUtils.throwIF(
                 StrUtil.isAllBlank(Username,AvatarURL,Phone,Email,Profile)
                         && gender == null
                         && userLevel == null
                         && accountStatus == null,
                 ErrorCode.PARAMS_ERROR,
                 "更新信息全为空！"
         );
        ThrowExceptionUtils.throwIF(
                userLevel != null && (StrUtil.isBlank(userLevel) || UserLevel.getEnumByValue(userLevel) == null),
                ErrorCode.PARAMS_ERROR,
                "用户等级只能是 user、admin 或 vip"
        );
        ThrowExceptionUtils.throwIF(
                accountStatus != null && UserStatus.getEnumByValue(accountStatus) == null,
                ErrorCode.PARAMS_ERROR,
                "账户状态只能是 0（正常）或 1（封禁）"
        );
        User user = new User();
        BeanUtils.copyProperties(updateUserRequest,user);
        // 请求对象用 accountStatus 表达账户状态，实体字段仍对应数据库 userStatus。
        if (accountStatus != null) {
            user.setUserStatus(accountStatus);
        }
        boolean b = userService.updateById(user);
        return ResponseUtils.success(b);
    }

    /**
     * 用户自己更新自己的信息
     * @param updateSelfRequest
     * @param request
     * @return Boolean
     */
    @PostMapping("/updateSelf")
    public BaseResponse<Boolean> updateSelf(@RequestBody UpdateSelfRequest updateSelfRequest,HttpServletRequest request){
        // 不从前端接收用户id，从当前Session中获取更安全
        User currentUser = userService.getCurrentUser(request);

        ThrowExceptionUtils.throwIF(
                currentUser ==null,
                ErrorCode.NOT_LOGIN_ERROR
        );

        // 请求体判空
        ThrowExceptionUtils.throwIF(
                updateSelfRequest == null,
                ErrorCode.PARAMS_ERROR,
                "更新请求体为空"
        );

        final String Username = updateSelfRequest.getUsername();
        final String AvatarURL = updateSelfRequest.getAvatarurl();
        final Integer gender = updateSelfRequest.getGender();
        final String Phone = updateSelfRequest.getPhone();
        final String Email = updateSelfRequest.getEmail();
        final String Profile = updateSelfRequest.getProfile();
        
        ThrowExceptionUtils.throwIF(
                StrUtil.isAllBlank(Username,AvatarURL,Phone,Email,Profile) && gender==null,
                ErrorCode.PARAMS_ERROR,
                "更新信息全为空！"
        );

        boolean b = userService.updateSelf(currentUser.getId(),request,updateSelfRequest);
        return ResponseUtils.success(b);
    }

    /**
     * 根据id删除
     * 仅管理员可操作
     * @param deleteRequest
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @DeleteMapping("/deleteById")
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest){
        boolean b = userService.removeById(deleteRequest.getId());
        return ResponseUtils.success(b);
    }

    /**
     * 批量id列表删除
     * 仅管理员可操作
     * @param deleteRequest
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @DeleteMapping("/deleteByIds")
    public BaseResponse<Boolean> deleteUsers(@RequestBody DeleteRequest deleteRequest){
        boolean b = userService.removeByIds(deleteRequest.getIds());
        return ResponseUtils.success(b);
    }

    /**
     * 管理员新增用户
     * @param addUserRequest
     * @return
     */
    @PutMapping("/addUser")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> addUser(@RequestBody AddUserRequest addUserRequest){
        ThrowExceptionUtils.throwIF(
                addUserRequest==null,
                ErrorCode.PARAMS_ERROR
        );
        boolean b= userService.addUser(addUserRequest);
        return ResponseUtils.success(b);
    }

}
