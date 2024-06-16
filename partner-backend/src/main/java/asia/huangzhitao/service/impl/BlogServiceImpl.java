package asia.huangzhitao.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import asia.huangzhitao.common.ErrorCode;
import asia.huangzhitao.exception.BusinessException;
import asia.huangzhitao.mapper.BlogMapper;
import asia.huangzhitao.model.domain.Blog;
import asia.huangzhitao.model.domain.BlogLike;
import asia.huangzhitao.model.domain.Follow;
import asia.huangzhitao.model.domain.Message;
import asia.huangzhitao.model.domain.User;
import asia.huangzhitao.model.enums.MessageTypeEnum;
import asia.huangzhitao.model.request.BlogAddRequest;
import asia.huangzhitao.model.request.BlogUpdateRequest;
import asia.huangzhitao.model.vo.BlogVO;
import asia.huangzhitao.model.vo.UserVO;
import asia.huangzhitao.service.BlogLikeService;
import asia.huangzhitao.service.BlogService;
import asia.huangzhitao.service.FollowService;
import asia.huangzhitao.service.MessageService;
import asia.huangzhitao.service.UserService;
import asia.huangzhitao.utils.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static asia.huangzhitao.constants.RedisConstants.BLOG_FEED_KEY;
import static asia.huangzhitao.constants.RedisConstants.MESSAGE_BLOG_NUM_KEY;
import static asia.huangzhitao.constants.RedisConstants.MESSAGE_LIKE_NUM_KEY;
import static asia.huangzhitao.constants.RedissonConstant.BLOG_LIKE_LOCK;
import static asia.huangzhitao.constants.RedissonConstant.DEFAULT_LEASE_TIME;
import static asia.huangzhitao.constants.RedissonConstant.DEFAULT_WAIT_TIME;
import static asia.huangzhitao.constants.SystemConstants.PAGE_SIZE;
import static asia.huangzhitao.constants.SystemConstants.PROTOCOL_LENGTH;

/**
 * @author huang
 * @description 针对表【blog】的数据库操作Service实现
 * @createDate 2024/01-03 15:54:34
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog>
        implements BlogService {

    @Resource
    private BlogLikeService blogLikeService;

    @Resource
    private UserService userService;

    @Resource
    private FollowService followService;

    @Resource
    private MessageService messageService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Value("${hwang.qiniu.url:null}")
    private String qiniuUrl;

    @Override
    public Long addBlog(BlogAddRequest blogAddRequest, User loginUser) {
        Blog blog = new Blog();
        ArrayList<String> imageNameList = new ArrayList<>();
        try {
            MultipartFile[] images = blogAddRequest.getImages();
            if (images != null) {
                for (MultipartFile image : images) {
                    String filename = FileUtils.uploadFile(image);
                    imageNameList.add(filename);
                }
                String imageStr = StringUtils.join(imageNameList, ",");
                blog.setImages(imageStr);
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, e.getMessage());
        }
        blog.setUserId(loginUser.getId());
        blog.setTitle(blogAddRequest.getTitle());
        blog.setContent(blogAddRequest.getContent());
        boolean saved = this.save(blog);
        if (saved) {
            List<UserVO> userVOList = followService.listFans(loginUser.getId());
            if (!userVOList.isEmpty()) {
                for (UserVO userVO : userVOList) {
                    String key = BLOG_FEED_KEY + userVO.getId();
                    stringRedisTemplate.opsForZSet().add(key, blog.getId().toString(), System.currentTimeMillis());
                    String likeNumKey = MESSAGE_BLOG_NUM_KEY + userVO.getId();
                    Boolean hasKey = stringRedisTemplate.hasKey(likeNumKey);
                    if (Boolean.TRUE.equals(hasKey)) {
                        stringRedisTemplate.opsForValue().increment(likeNumKey);
                    } else {
                        stringRedisTemplate.opsForValue().set(likeNumKey, "1");
                    }
                }
            }
        }
        return blog.getId();
    }

    @Override
    public Page<BlogVO> listMyBlogs(long currentPage, Long id) {
        if (currentPage <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LambdaQueryWrapper<Blog> blogLambdaQueryWrapper = new LambdaQueryWrapper<>();
        blogLambdaQueryWrapper.eq(Blog::getUserId, id);
        Page<Blog> blogPage = this.page(new Page<>(currentPage, PAGE_SIZE), blogLambdaQueryWrapper);
        Page<BlogVO> blogVoPage = new Page<>();
        BeanUtils.copyProperties(blogPage, blogVoPage);
        List<BlogVO> blogVOList = blogPage.getRecords().stream().map((blog) -> {
            BlogVO blogVO = new BlogVO();
            BeanUtils.copyProperties(blog, blogVO);
            return blogVO;
        }).collect(Collectors.toList());
        for (BlogVO blogVO : blogVOList) {
            String images = blogVO.getImages();
            if (images == null) {
                continue;
            }
            String[] imgStr = images.split(",");
            blogVO.setCoverImage(qiniuUrl + imgStr[0]);
        }
        blogVoPage.setRecords(blogVOList);
        return blogVoPage;
    }

    /**
     * 点赞博文
     *
     * @param blogId 博文id
     * @param userId 用户id
     */
    @Override
    public void likeBlog(long blogId, Long userId) {
        RLock lock = redissonClient.getLock(BLOG_LIKE_LOCK + blogId + ":" + userId);
        try {
            if (lock.tryLock(DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, TimeUnit.MILLISECONDS)) {
                Blog blog = this.getById(blogId);
                if (blog == null) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "博文不存在");
                }
                LambdaQueryWrapper<BlogLike> blogLikeLambdaQueryWrapper = new LambdaQueryWrapper<>();
                blogLikeLambdaQueryWrapper.eq(BlogLike::getBlogId, blogId);
                blogLikeLambdaQueryWrapper.eq(BlogLike::getUserId, userId);
                long isLike = blogLikeService.count(blogLikeLambdaQueryWrapper);
                if (isLike > 0) {
                    blogLikeService.remove(blogLikeLambdaQueryWrapper);
                    doUnLikeBlog(blog, userId);
                } else {
                    doLikeBlog(blog, userId);
                }
            }
        } catch (Exception e) {
            log.error("LikeBlog error", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 取消点赞博文
     *
     * @param blog   博文
     * @param userId 用户id
     */
    public void doUnLikeBlog(Blog blog, Long userId) {
        int newNum = blog.getLikedNum() - 1;
        this.update().eq("id", blog.getId()).set("liked_num", newNum).update();
        LambdaQueryWrapper<Message> messageQueryWrapper = new LambdaQueryWrapper<>();
        messageQueryWrapper
                .eq(Message::getType, MessageTypeEnum.BLOG_LIKE.getValue())
                .eq(Message::getFromId, userId)
                .eq(Message::getToId, blog.getUserId())
                .eq(Message::getData, String.valueOf(blog.getId()));
        messageService.remove(messageQueryWrapper);
        String likeNumKey = MESSAGE_LIKE_NUM_KEY + blog.getUserId();
        String upNumStr = stringRedisTemplate.opsForValue().get(likeNumKey);
        if (!StrUtil.isNullOrUndefined(upNumStr) && Long.parseLong(upNumStr) != 0) {
            stringRedisTemplate.opsForValue().decrement(likeNumKey);
        }
    }

    /**
     * 点赞博文
     *
     * @param blog   博文
     * @param userId 用户id
     */
    public void doLikeBlog(Blog blog, Long userId) {
        BlogLike blogLike = new BlogLike();
        blogLike.setBlogId(blog.getId());
        blogLike.setUserId(userId);
        blogLikeService.save(blogLike);
        int newNum = blog.getLikedNum() + 1;
        this.update().eq("id", blog.getId()).set("liked_num", newNum).update();
        Message message = new Message();
        message.setType(MessageTypeEnum.BLOG_LIKE.getValue());
        message.setFromId(userId);
        message.setToId(blog.getUserId());
        message.setData(String.valueOf(blog.getId()));
        messageService.save(message);
        String likeNumKey = MESSAGE_LIKE_NUM_KEY + blog.getUserId();
        Boolean hasKey = stringRedisTemplate.hasKey(likeNumKey);
        if (Boolean.TRUE.equals(hasKey)) {
            stringRedisTemplate.opsForValue().increment(likeNumKey);
        } else {
            stringRedisTemplate.opsForValue().set(likeNumKey, "1");
        }
    }

    @Override
    public Page<BlogVO> pageBlog(long currentPage, String title, Long userId) {
        LambdaQueryWrapper<Blog> blogLambdaQueryWrapper = new LambdaQueryWrapper<>();
        blogLambdaQueryWrapper.like(StringUtils.isNotBlank(title), Blog::getTitle, title);
        blogLambdaQueryWrapper.orderBy(true, false, Blog::getCreateTime);
        Page<Blog> blogPage = this.page(new Page<>(currentPage, PAGE_SIZE), blogLambdaQueryWrapper);
        Page<BlogVO> blogVoPage = new Page<>();
        BeanUtils.copyProperties(blogPage, blogVoPage);
        List<BlogVO> blogVOList = blogPage.getRecords().stream().map((blog) -> {
            BlogVO blogVO = new BlogVO();
            BeanUtils.copyProperties(blog, blogVO);
            if (userId != null) {
                LambdaQueryWrapper<BlogLike> blogLikeLambdaQueryWrapper = new LambdaQueryWrapper<>();
                blogLikeLambdaQueryWrapper.eq(BlogLike::getBlogId, blog.getId()).eq(BlogLike::getUserId, userId);
                long count = blogLikeService.count(blogLikeLambdaQueryWrapper);
                blogVO.setIsLike(count > 0);
            }
            return blogVO;
        }).collect(Collectors.toList());
        for (BlogVO blogVO : blogVOList) {
            String images = blogVO.getImages();
            if (images == null) {
                continue;
            }
            String[] imgStrs = images.split(",");
            blogVO.setCoverImage(qiniuUrl + imgStrs[0]);
        }
        blogVoPage.setRecords(blogVOList);
        return blogVoPage;
    }

    @Override
    public BlogVO getBlogById(long blogId, Long userId) {
        Blog blog = this.getById(blogId);
        if (blog == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无法找到该博文");
        }
        BlogVO blogVO = new BlogVO();
        BeanUtils.copyProperties(blog, blogVO);
        LambdaQueryWrapper<BlogLike> blogLikeLambdaQueryWrapper = new LambdaQueryWrapper<>();
        blogLikeLambdaQueryWrapper.eq(BlogLike::getUserId, userId);
        blogLikeLambdaQueryWrapper.eq(BlogLike::getBlogId, blogId);
        long isLike = blogLikeService.count(blogLikeLambdaQueryWrapper);
        blogVO.setIsLike(isLike > 0);
        User author = userService.getById(blog.getUserId());
        if (author == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无法找到该博文作者");
        }
        UserVO authorVO = new UserVO();
        BeanUtils.copyProperties(author, authorVO);
        LambdaQueryWrapper<Follow> followLambdaQueryWrapper = new LambdaQueryWrapper<>();
        followLambdaQueryWrapper.eq(Follow::getFollowUserId, authorVO.getId()).eq(Follow::getUserId, userId);
        long count = followService.count(followLambdaQueryWrapper);
        authorVO.setIsFollow(count > 0);
        blogVO.setAuthor(authorVO);
        String images = blogVO.getImages();
        if (images == null) {
            return blogVO;
        }
        String[] imgStrs = images.split(",");
        ArrayList<String> imgStrList = new ArrayList<>();
        for (String imgStr : imgStrs) {
            imgStrList.add(qiniuUrl + imgStr);
        }
        String imgStr = StringUtils.join(imgStrList, ",");
        blogVO.setImages(imgStr);
        blogVO.setCoverImage(imgStrList.get(0));
        return blogVO;
    }

    @Override
    public void deleteBlog(Long blogId, Long userId, boolean isAdmin) {
        if (isAdmin) {
            this.removeById(blogId);
            return;
        }
        Blog blog = this.getById(blogId);
        if (!userId.equals(blog.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        this.removeById(blogId);
    }

    @Override
    public void updateBlog(BlogUpdateRequest blogUpdateRequest, Long userId, boolean isAdmin) {
        if (blogUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long createUserId = this.getById(blogUpdateRequest.getId()).getUserId();
        if (!createUserId.equals(userId) && !isAdmin) {
            throw new BusinessException(ErrorCode.NO_AUTH, "没有权限");
        }
        String title = blogUpdateRequest.getTitle();
        String content = blogUpdateRequest.getContent();
        if (StringUtils.isAnyBlank(title, content)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Blog blog = new Blog();
        blog.setId(blogUpdateRequest.getId());
        ArrayList<String> imageNameList = new ArrayList<>();
        if (StringUtils.isNotBlank(blogUpdateRequest.getImgStr())) {
            String imgStr = blogUpdateRequest.getImgStr();
            String[] imgs = imgStr.split(",");
            for (String img : imgs) {
                String fileName = img.substring(img.indexOf("/", PROTOCOL_LENGTH) + 1);
                imageNameList.add(fileName);
            }
        }
        if (blogUpdateRequest.getImages() != null) {
            MultipartFile[] images = blogUpdateRequest.getImages();
            for (MultipartFile image : images) {
                String filename = FileUtils.uploadFile(image);
                imageNameList.add(filename);
            }
        }
        if (!imageNameList.isEmpty()) {
            String imageStr = StringUtils.join(imageNameList, ",");
            blog.setImages(imageStr);
        }
        blog.setTitle(blogUpdateRequest.getTitle());
        blog.setContent(blogUpdateRequest.getContent());
        this.updateById(blog);
    }
}




