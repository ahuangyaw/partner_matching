package asia.huangzhitao.service.impl;


import asia.huangzhitao.constants.RedisConstants;
import cn.hutool.core.lang.Pair;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import asia.huangzhitao.common.ErrorCode;
import asia.huangzhitao.exception.BusinessException;
import asia.huangzhitao.mapper.UserMapper;
import asia.huangzhitao.model.domain.Follow;
import asia.huangzhitao.model.domain.User;
import asia.huangzhitao.model.request.UserRegisterRequest;
import asia.huangzhitao.model.request.UserUpdateRequest;
import asia.huangzhitao.model.vo.UserVO;
import asia.huangzhitao.service.FollowService;
import asia.huangzhitao.service.UserService;
import asia.huangzhitao.utils.AlgorithmUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.BeanUtils;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static asia.huangzhitao.constants.RedisConstants.LOGIN_USER_KEY;
import static asia.huangzhitao.constants.RedisConstants.LOGIN_USER_TTL;
import static asia.huangzhitao.constants.RedisConstants.REGISTER_CODE_KEY;
import static asia.huangzhitao.constants.RedisConstants.USER_FORGET_PASSWORD_KEY;
import static asia.huangzhitao.constants.RedisConstants.USER_RECOMMEND_KEY;
import static asia.huangzhitao.constants.RedisConstants.USER_UPDATE_EMAIL_KEY;
import static asia.huangzhitao.constants.RedisConstants.USER_UPDATE_PHONE_KEY;
import static asia.huangzhitao.constants.SystemConstants.MAXIMUM_LOGIN_IDLE_TIME;
import static asia.huangzhitao.constants.SystemConstants.MINIMUM_ENABLE_RANDOM_USER_NUM;
import static asia.huangzhitao.constants.SystemConstants.PAGE_SIZE;
import static asia.huangzhitao.constants.UserConstants.ADMIN_ROLE;
import static asia.huangzhitao.constants.UserConstants.MINIMUM_ACCOUNT_LEN;
import static asia.huangzhitao.constants.UserConstants.MINIMUM_PASSWORD_LEN;
import static asia.huangzhitao.constants.UserConstants.USER_LOGIN_STATE;

/**
 * 用户服务实现
 *
 * @author huang
 * @description 针对表【user】的数据库操作Service实现
 * @createDate 2024/01-07 19:56:01
 * @date 2024/01/25
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final String[] AVATAR_URLS = {
            "https://upload-bbs.miyoushe.com/upload/2024/01/02/285802042/51a31eeef957834fc1b797aa5ea2c36b_6649473296511790775.png",
            "https://upload-bbs.miyoushe.com/upload/2023/12/25/345635027/2d614722a189de74f2887a485d6e86f3_4786857272219630503.jpg",
            "https://upload-bbs.miyoushe.com/upload/2023/12/25/345635027/3a1a03047b098b2ea46c7662d5132b23_4426906400139150137.jpg",
            "https://upload-bbs.miyoushe.com/upload/2023/12/25/345635027/93b31f5443defbb521a20e35956d330d_3966508656458729950.jpg",
            "https://upload-bbs.miyoushe.com/upload/2023/12/25/345635027/eb85bcb7caf76a018f391b57949cec31_7864381713428247687.jpg",
            "https://upload-bbs.miyoushe.com/upload/2023/11/19/337874093/8a3f457b94b83406084591462b601bb6_6474943391553395936.png",
            "https://upload-bbs.miyoushe.com/upload/2023/11/24/337874093/ef5beacdb2bb0a630e88c5628b08c972_5560493480586475489.png",
            "https://upload-bbs.miyoushe.com/upload/2023/11/24/337874093/dea770d571b87be91c72c0258994986f_1750332781333377660.webp",
            "https://upload-bbs.miyoushe.com/upload/2024/01/02/285802042/51a31eeef957834fc1b797aa5ea2c36b_6649473296511790775.png",

};
    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "huang";
    @Resource
    private UserMapper userMapper;
    @Resource
    private FollowService followService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;


    /**
     * 用户登记
     *
     * @param userRegisterRequest 用户登记要求
     * @param request             要求
     * @return {@link String}
     */
    @Override
    public String userRegister(UserRegisterRequest userRegisterRequest, HttpServletRequest request) {
        String phone = userRegisterRequest.getPhone();
        String code = userRegisterRequest.getCode();
        String account = userRegisterRequest.getUserAccount();
        String password = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        checkRegisterRequest(phone, code, account, password, checkPassword);
        checkAccountValid(account);
        checkAccountRepetition(account);
        checkHasRegistered(phone);
        String key = REGISTER_CODE_KEY + phone;
        checkCode(code, key);
        checkPassword(password, checkPassword);
        long userId = insetUser(phone, account, password);
        return afterInsertUser(key, userId, request);
    }

    /**
     * 管理员寄存器
     *
     * @param userRegisterRequest 用户登记要求
     * @param request             要求
     * @return {@link Long}
     */
    @Override
    public Long adminRegister(UserRegisterRequest userRegisterRequest, HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "未登录");
        }
        Integer role = loginUser.getRole();
        if (!role.equals(ADMIN_ROLE)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限");
        }
        String phone = userRegisterRequest.getPhone();
        String account = userRegisterRequest.getUserAccount();
        String password = userRegisterRequest.getUserPassword();
        checkAccountValid(account);
        checkAccountRepetition(account);
        return insetUser(phone, account, password);
    }

    /**
     * 改变用户地位
     *
     * @param id id
     */
    @Override
    public void changeUserStatus(Long id) {
        User user = this.getById(id);
        LambdaUpdateWrapper<User> userLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        if (user.getStatus().equals(0)) {
            userLambdaUpdateWrapper.eq(User::getId, id).set(User::getStatus, 1);
        } else {
            userLambdaUpdateWrapper.eq(User::getId, id).set(User::getStatus, 0);
        }
        try {
            this.update(userLambdaUpdateWrapper);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统错误");
        }
    }

    @Override
    public List<UserVO> searchNearby(int radius, User loginUser) {
        String geoKey = RedisConstants.USER_GEO_KEY;
        String userId = String.valueOf(loginUser.getId());
        int longitude = loginUser.getLongitude();
        int dimension = loginUser.getDimension();
        if (longitude == 0 || dimension == 0) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "登录用户经纬度参数为空");
        }
        Distance geoRadius = new Distance(radius, RedisGeoCommands.DistanceUnit.KILOMETERS);
        Circle circle = new Circle(new Point(longitude, dimension), geoRadius);
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo()
                .radius(geoKey, circle);
        List<Long> userIdList = new ArrayList<>();
        assert results != null;
        for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : results) {
            String id = result.getContent().getName();
            if (!userId.equals(id)) {
                userIdList.add(Long.parseLong(id));
            }
        }
        List<UserVO> userVOList = userIdList.stream().map(
                id -> {
                    UserVO userVO = new UserVO();
                    User user = this.getById(id);
                    BeanUtils.copyProperties(user, userVO);
                    Distance distance = stringRedisTemplate.opsForGeo().distance(geoKey, userId, String.valueOf(id),
                            RedisGeoCommands.DistanceUnit.KILOMETERS);
                    assert distance != null;
                    userVO.setDistance(distance.getValue());
                    return userVO;
                }
        ).collect(Collectors.toList());
        return userVOList;
    }

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户暗语
     * @param request      要求
     * @return {@link String}
     */
    @Override
    public String userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        validateUserRequest(userAccount, userPassword);
        User userInDatabase = getUserInDatabase(userAccount, userPassword);
        // 3. 用户脱敏
        User safetyUser = getSafetyUser(userInDatabase);
        // 4. 记录用户的登录态
        return setUserLoginState(request, safetyUser);
    }

    /**
     * 设置用户登录状态
     *
     * @param request    要求
     * @param safetyUser 安全用户
     * @return {@link String}
     */
    public String setUserLoginState(HttpServletRequest request, User safetyUser) {
        request.getSession().setAttribute(USER_LOGIN_STATE, safetyUser);
        request.getSession().setMaxInactiveInterval(MAXIMUM_LOGIN_IDLE_TIME);
        String token = UUID.randomUUID().toString(true);
        Gson gson = new Gson();
        String userStr = gson.toJson(safetyUser);
        stringRedisTemplate.opsForValue().set(LOGIN_USER_KEY + token, userStr);
        stringRedisTemplate.expire(LOGIN_USER_KEY + token, Duration.ofSeconds(MAXIMUM_LOGIN_IDLE_TIME));
        return token;
    }

    /**
     * 验证用户请求
     *
     * @param userAccount  用户账户
     * @param userPassword 用户暗语
     */
    public void validateUserRequest(String userAccount, String userPassword) {
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }
        if (userAccount.length() < MINIMUM_ACCOUNT_LEN) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号非法");
        }
        if (userPassword.length() < MINIMUM_PASSWORD_LEN) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码非法");
        }
        // 账户不能包含特殊字符
        String validPattern = "[^[a-zA-Z][a-zA-Z0-9_]{4,15}$]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (!matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号非法");
        }
    }

    /**
     * 管理员登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户暗语
     * @param request      要求
     * @return {@link String}
     */
    @Override
    public String adminLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        validateUserRequest(userAccount, userPassword);
        User userInDatabase = getUserInDatabase(userAccount, userPassword);
        if (!userInDatabase.getRole().equals(1)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "非管理员禁止登录");
        }
        // 3. 用户脱敏
        User safetyUser = getSafetyUser(userInDatabase);
        // 4. 记录用户的登录态
        return setUserLoginState(request, safetyUser);
    }

    /**
     * 取得数据库数据
     *
     * @param userAccount  用户账户
     * @param userPassword 用户暗语
     * @return {@link User}
     */
    public User getUserInDatabase(String userAccount, String userPassword) {
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userLambdaQueryWrapper.eq(User::getUserAccount, userAccount);
        User userInDatabase = this.getOne(userLambdaQueryWrapper);
        if (userInDatabase == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        if (!userInDatabase.getPassword().equals(encryptPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        if (!userInDatabase.getStatus().equals(0)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "该用户已被封禁");
        }
        return userInDatabase;
    }

    /**
     * 用户脱敏
     *
     * @param originUser 起源用户
     * @return {@link User}
     */
    @Override
    public User getSafetyUser(User originUser) {
        if (originUser == null) {
            return null;
        }
        User safetyUser = new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUsername(originUser.getUsername());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setAvatarUrl(originUser.getAvatarUrl());
        safetyUser.setGender(originUser.getGender());
        safetyUser.setPhone(originUser.getPhone());
        safetyUser.setEmail(originUser.getEmail());
        safetyUser.setRole(originUser.getRole());
        safetyUser.setStatus(originUser.getStatus());
        safetyUser.setCreateTime(originUser.getCreateTime());
        safetyUser.setTags(originUser.getTags());
        safetyUser.setProfile(originUser.getProfile());
        return safetyUser;
    }

    /**
     * 用户注销
     *
     * @param request 要求
     * @return int
     */
    @Override
    public int userLogout(HttpServletRequest request) {
        // 移除登录态
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        stringRedisTemplate.delete(LOGIN_USER_KEY + token);
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }

    /**
     * 按标签搜索用户
     * 根据标签搜索用户（内存过滤）
     *
     * @param tagNameList 用户要拥有的标签
     * @param currentPage 当前页码
     * @return {@link Page}<{@link User}>
     */
    @Override
    public Page<User> searchUsersByTags(List<String> tagNameList, long currentPage) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
        for (String tagName : tagNameList) {
            userLambdaQueryWrapper = userLambdaQueryWrapper
                    .or().like(Strings.isNotEmpty(tagName), User::getTags, tagName);
        }
        return page(new Page<>(currentPage, PAGE_SIZE), userLambdaQueryWrapper);
    }

    /**
     * 是否为管理员
     *
     * @param loginUser 登录用户
     * @return boolean
     */
    @Override
    public boolean isAdmin(User loginUser) {
        return loginUser != null && loginUser.getRole() == ADMIN_ROLE;
    }


    /**
     * 使现代化用户
     *
     * @param user    用户
     * @param request 要求
     * @return boolean
     */
    @Override
    public boolean updateUser(User user, HttpServletRequest request) {
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        user.setId(loginUser.getId());
        if (!(isAdmin(loginUser) || loginUser.getId().equals(user.getId()))) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        return updateById(user);
    }

    /**
     * 用户分页
     *
     * @param currentPage 当前页码
     * @return {@link Page}<{@link UserVO}>
     */
    @Override
    public Page<UserVO> userPage(long currentPage) {
        Page<User> page = this.page(new Page<>(currentPage, PAGE_SIZE));
        Page<UserVO> userVoPage = new Page<>();
        BeanUtils.copyProperties(page, userVoPage);
        return userVoPage;
    }

    /**
     * 收到登录用户
     *
     * @param request 要求
     * @return {@link User}
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
            return null;
        }
        String userStr = stringRedisTemplate.opsForValue().get(LOGIN_USER_KEY + token);
        if (StrUtil.isBlank(userStr)) {
            return null;
        }
        Gson gson = new Gson();
        User user = gson.fromJson(userStr, User.class);
        stringRedisTemplate.expire(LOGIN_USER_KEY + token, LOGIN_USER_TTL, TimeUnit.MINUTES);
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        request.getSession().setMaxInactiveInterval(MAXIMUM_LOGIN_IDLE_TIME);
        return user;
    }

    /**
     * 是登录名
     *
     * @param request 要求
     * @return {@link Boolean}
     */
    @Override
    public Boolean isLogin(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        return userObj != null;
    }

    /**
     * 获取用户
     *
     * @param currentPage 当前页码
     * @param loginUser   登录用户
     * @return {@link Page}<{@link UserVO}>
     */
    @Override
    public Page<UserVO> matchUser(long currentPage, User loginUser) {
        String tags = loginUser.getTags();
        if (tags == null || tags.isEmpty() || tags.equals("[]")){
            return this.getRandomUser();
        }
        // 获取根据算法排列后的用户列表
        List<Pair<User, Long>> arrangedUser = getArrangedUser(tags, loginUser.getId());
        // 截取currentPage所需的List
        ArrayList<Pair<User, Long>> finalUserPairList = new ArrayList<>();
        int begin = (int) ((currentPage - 1) * PAGE_SIZE);
        int end = (int) (((currentPage - 1) * PAGE_SIZE) + PAGE_SIZE) - 1;
        // 手动整理最后一页
        if (arrangedUser.size() < end) {
            //剩余数量
            int temp = arrangedUser.size() - begin;
            if (temp <= 0) {
                return new Page<>();
            }
            for (int i = begin; i <= begin + temp - 1; i++) {
                finalUserPairList.add(arrangedUser.get(i));
            }
        } else {
            for (int i = begin; i < end; i++) {
                finalUserPairList.add(arrangedUser.get(i));
            }
        }
        // 获取排列后的UserId
        List<Long> userIdList = finalUserPairList.stream().map(pair -> pair.getKey().getId())
                .collect(Collectors.toList());
        List<UserVO> userVOList = getUserListByIdList(userIdList, loginUser.getId());
        Page<UserVO> userVoPage = new Page<>();
        userVoPage.setRecords(userVOList);
        userVoPage.setCurrent(currentPage);
        userVoPage.setSize(userVOList.size());
        userVoPage.setTotal(userVOList.size());
        return userVoPage;
    }

    /**
     * 根据算法排列用户
     *
     * @param tags 标签
     * @param id   id
     * @return {@link List}<{@link Pair}<{@link User}, {@link Long}>>
     */
    public List<Pair<User, Long>> getArrangedUser(String tags, Long id) {
        LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userLambdaQueryWrapper.select(User::getId, User::getTags);
        List<User> userList = this.list(userLambdaQueryWrapper);
        Gson gson = new Gson();
        List<String> tagList = gson.fromJson(tags, new TypeToken<List<String>>() {
        }.getType());
        // 用户列表的下标 => 相似度
        List<Pair<User, Long>> list = new ArrayList<>();
        // 依次计算所有用户和当前用户的相似度
        for (User user : userList) {
            String userTags = user.getTags();
            // 无标签或者为当前用户自己
            if (StringUtils.isBlank(userTags) || Objects.equals(user.getId(), id)) {
                continue;
            }
            List<String> userTagList = gson.fromJson(userTags, new TypeToken<List<String>>() {
            }.getType());
            // 计算分数
            long distance = AlgorithmUtil.minDistance(tagList, userTagList);
            list.add(new Pair<>(user, distance));
        }
        // 按编辑距离由小到大排序
        return list.stream()
                .sorted((a, b) -> (int) (a.getValue() - b.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * 通过Id列表获取用户
     *
     * @param userIdList 用户id列表
     * @param userId     用户id
     * @return {@link List}<{@link UserVO}>
     */
    public List<UserVO> getUserListByIdList(List<Long> userIdList, long userId) {
        String idStr = StringUtils.join(userIdList, ",");
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.in("id", userIdList).last("ORDER BY FIELD(id," + idStr + ")");
        return this.list(userQueryWrapper)
                .stream()
                .map((user) -> followService.getUserFollowInfo(user, userId))
                .collect(Collectors.toList());
    }

    /**
     * 收到用户通过id
     *
     * @param userId      用户id
     * @param loginUserId 登录用户id
     * @return {@link UserVO}
     */
    @Override
    public UserVO getUserById(Long userId, Long loginUserId) {
        User user = this.getById(userId);
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        LambdaQueryWrapper<Follow> followLambdaQueryWrapper = new LambdaQueryWrapper<>();
        followLambdaQueryWrapper.eq(Follow::getUserId, loginUserId).eq(Follow::getFollowUserId, userId);
        long count = followService.count(followLambdaQueryWrapper);
        userVO.setIsFollow(count > 0);
        return userVO;
    }

    /**
     * 收到用户标签
     *
     * @param id id
     * @return {@link List}<{@link String}>
     */
    @Override
    public List<String> getUserTags(Long id) {
        User user = this.getById(id);
        String userTags = user.getTags();
        Gson gson = new Gson();
        return gson.fromJson(userTags, new TypeToken<List<String>>() {
        }.getType());
    }

    /**
     * 更新标记
     *
     * @param tags   标签
     * @param userId 用户id
     */
    @Override
    public void updateTags(List<String> tags, Long userId) {
        User user = new User();
        Gson gson = new Gson();
        String tagsJson = gson.toJson(tags);
        user.setId(userId);
        user.setTags(tagsJson);
        this.updateById(user);
    }

    /**
     * 使现代化用户具有密码
     *
     * @param updateRequest 更新请求
     * @param userId        用户id
     */
    @Override
    public void updateUserWithCode(UserUpdateRequest updateRequest, Long userId) {
        String key;
        boolean isPhone = false;
        if (StringUtils.isNotBlank(updateRequest.getPhone())) {
            key = USER_UPDATE_PHONE_KEY + updateRequest.getPhone();
            isPhone = true;
        } else {
            key = USER_UPDATE_EMAIL_KEY + updateRequest.getEmail();
        }
        String correctCode = stringRedisTemplate.opsForValue().get(key);
        if (correctCode == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请先发送验证码");
        }
        if (!correctCode.equals(updateRequest.getCode())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误");
        }
        LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
        if (isPhone) {
            userLambdaQueryWrapper.eq(User::getPhone, updateRequest.getPhone());
            User user = this.getOne(userLambdaQueryWrapper);
            if (user != null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "该手机号已被绑定");
            }
        } else {
            userLambdaQueryWrapper.eq(User::getEmail, updateRequest.getEmail());
            User user = this.getOne(userLambdaQueryWrapper);
            if (user != null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "该邮箱已被绑定");
            }
        }
        User user = new User();
        BeanUtils.copyProperties(updateRequest, user);
        user.setId(userId);
        this.updateById(user);
    }

    /**
     * 收到随机用户
     *
     * @return {@link Page}<{@link UserVO}>
     */
    @Override
    public Page<UserVO> getRandomUser() {
        List<User> randomUser = userMapper.getRandomUser();
        List<UserVO> userVOList = randomUser.stream().map((item) -> {
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(item, userVO);
            return userVO;
        }).collect(Collectors.toList());
        Page<UserVO> userVOPage = new Page<>();
        userVOPage.setRecords(userVOList);
        return userVOPage;
    }

    /**
     * 更新密码
     *
     * @param phone           电话
     * @param code            密码
     * @param password        暗语
     * @param confirmPassword 确认密码
     */
    @Override
    public void updatePassword(String phone, String code, String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        String key = USER_FORGET_PASSWORD_KEY + phone;
        String correctCode = stringRedisTemplate.opsForValue().get(key);
        if (correctCode == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请先获取验证码");
        }
        if (!correctCode.equals(code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误");
        }
        LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userLambdaQueryWrapper.eq(User::getPhone, phone);
        User user = this.getOne(userLambdaQueryWrapper);
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + password).getBytes());
        user.setPassword(encryptPassword);
        this.updateById(user);
        stringRedisTemplate.delete(key);
    }

    /**
     * 通过用户名收到用户分页
     *
     * @param currentPage 当前页码
     * @param username    用户名
     * @param loginUser   登录用户
     * @return {@link Page}<{@link UserVO}>
     */
    public Page<UserVO> getUserPageByUsername(long currentPage, String username, User loginUser) {
        LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userLambdaQueryWrapper.like(User::getUsername, username);

        Page<User> userPage = this.page(new Page<>(currentPage, PAGE_SIZE), userLambdaQueryWrapper);
        Page<UserVO> userVOPage = new Page<>();
        BeanUtils.copyProperties(userPage, userVOPage);

        List<UserVO> userVOList = userPage.getRecords()
                .stream().map((user) -> this.getUserById(user.getId(), loginUser.getId()))
                .collect(Collectors.toList());
        userVOPage.setRecords(userVOList);
        return userVOPage;
    }

    /**
     * 之前获取用户
     *
     * @param currentPage 当前页码
     * @param username    用户名
     * @param loginUser   登录用户
     * @return {@link Page}<{@link UserVO}>
     */
    @Override
    public Page<UserVO> preMatchUser(long currentPage, String username, User loginUser) {
        Gson gson = new Gson();
        // 用户已登录
        if (loginUser != null) {
            String key = USER_RECOMMEND_KEY + loginUser.getId() + ":" + currentPage;
            Page<UserVO> userVOPage;
            if (StringUtils.isNotBlank(username)) { // 填写了用户名,模糊查询
                userVOPage = getUserPageByUsername(currentPage, username, loginUser);
            } else {
                // 没有填写用户名,正常匹配
                Boolean hasKey = stringRedisTemplate.hasKey(key);
                if (Boolean.TRUE.equals(hasKey)) { // 存在缓存
                    String userVOPageStr = stringRedisTemplate.opsForValue().get(key);
                    userVOPage = gson.fromJson(userVOPageStr, new TypeToken<Page<UserVO>>() {
                    }.getType());
                } else
                { // 不存在缓存,匹配后加入缓存
                    userVOPage = this.matchUser(currentPage, loginUser);
                    String userVOPageStr = gson.toJson(userVOPage);
                    stringRedisTemplate.opsForValue().set(key, userVOPageStr);
                }
            }
            return userVOPage;
        } else { // 用户未登录
            if (StringUtils.isNotBlank(username)) { // 禁止未登录用户模糊查询
                throw new BusinessException(ErrorCode.NOT_LOGIN);
            }
            long userNum = this.count();
            // 用户量过少,直接列出用户
            if (userNum <= MINIMUM_ENABLE_RANDOM_USER_NUM) {
                Page<User> userPage = this.page(new Page<>(currentPage, PAGE_SIZE));
                List<UserVO> userVOList = userPage.getRecords().stream().map((user) -> {
                    UserVO userVO = new UserVO();
                    BeanUtils.copyProperties(user, userVO);
                    return userVO;
                }).collect(Collectors.toList());
                Page<UserVO> userVOPage = new Page<>();
                userVOPage.setRecords(userVOList);
                return userVOPage;
            }
            // 用户量足够,随机展示用户
            return this.getRandomUser();
        }
    }

    /**
     * 检查寄存器请求
     *
     * @param phone         电话
     * @param code          密码
     * @param account       账户
     * @param password      暗语
     * @param checkPassword 检查密码
     */
    private void checkRegisterRequest(String phone,
                                      String code,
                                      String account,
                                      String password,
                                      String checkPassword) {
        if (StringUtils.isAnyBlank(phone, code, account, password, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "信息不全");
        }
        if (StringUtils.isAnyBlank(phone, code, account, password, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (account.length() < MINIMUM_ACCOUNT_LEN) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (password.length() < MINIMUM_PASSWORD_LEN || checkPassword.length() < MINIMUM_PASSWORD_LEN) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
    }

    /**
     * 支票已注册
     *
     * @param phone 电话
     */
    private void checkHasRegistered(String phone) {
        LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userLambdaQueryWrapper.eq(User::getPhone, phone);
        long phoneNum = this.count(userLambdaQueryWrapper);
        if (phoneNum >= 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "该手机号已注册");
        }
    }

    /**
     * 校验码
     *
     * @param code 密码
     * @param key  钥匙
     */
    private void checkCode(String code, String key) {
        Boolean hasKey = stringRedisTemplate.hasKey(key);
        if (Boolean.FALSE.equals(hasKey)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "请先获取验证码");
        }
        String correctCode = stringRedisTemplate.opsForValue().get(key);
        if (correctCode == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        if (!correctCode.equals(code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误");
        }
    }

    /**
     * 支票帐户有效
     *
     * @param account 账户
     */
    private void checkAccountValid(String account) {
        String validPattern = "[^[a-zA-Z][a-zA-Z0-9_]{4,15}$]";
        Matcher matcher = Pattern.compile(validPattern).matcher(account);
        if (!matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号非法");
        }
    }

    /**
     * 检查密码
     *
     * @param password      暗语
     * @param checkPassword 检查密码
     */
    private void checkPassword(String password, String checkPassword) {
        if (!password.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
    }

    /**
     * 支票账户重复
     *
     * @param account 账户
     */
    private void checkAccountRepetition(String account) {
        LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userLambdaQueryWrapper.eq(User::getUserAccount, account);
        long count = this.count(userLambdaQueryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
    }

    /**
     * 插图用户
     *
     * @param phone    电话
     * @param account  账户
     * @param password 暗语
     * @return long
     */
    private long insetUser(String phone, String account, String password) {
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + password).getBytes());
        // 3. 插入数据
        User user = new User();
        Random random = new Random();
        user.setAvatarUrl(AVATAR_URLS[random.nextInt(AVATAR_URLS.length)]);
        user.setPhone(phone);
        user.setUsername(account);
        user.setUserAccount(account);
        user.setPassword(encryptPassword);
        ArrayList<String> tag = new ArrayList<>();
        Gson gson = new Gson();
        String jsonTag = gson.toJson(tag);
        user.setTags(jsonTag);
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return user.getId();
    }

    /**
     * 之后插入用户
     *
     * @param key     钥匙
     * @param userId  用户id
     * @param request 要求
     * @return {@link String}
     */
    @Override
    public String afterInsertUser(String key, long userId, HttpServletRequest request) {
        stringRedisTemplate.delete(key);
        User userInDatabase = this.getById(userId);
        User safetyUser = this.getSafetyUser(userInDatabase);
        String token = UUID.randomUUID().toString(true);
        Gson gson = new Gson();
        String userStr = gson.toJson(safetyUser);
        request.getSession().setAttribute(USER_LOGIN_STATE, safetyUser);
        request.getSession().setMaxInactiveInterval(MAXIMUM_LOGIN_IDLE_TIME);
        stringRedisTemplate.opsForValue().set(LOGIN_USER_KEY + token, userStr);
        stringRedisTemplate.expire(LOGIN_USER_KEY + token, Duration.ofSeconds(MAXIMUM_LOGIN_IDLE_TIME));
        return token;
    }
}




