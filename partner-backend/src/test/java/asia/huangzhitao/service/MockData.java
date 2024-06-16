package asia.huangzhitao.service;

import asia.huangzhitao.constants.RedisConstants;
import asia.huangzhitao.utils.AlgorithmUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import asia.huangzhitao.model.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.*;

import static org.apache.commons.lang3.math.NumberUtils.min;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MockData {
    @Resource
    private UserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final String[] avatarUrls = {
            "https://upload-bbs.miyoushe.com/upload/2023/12/25/345635027/93b31f5443defbb521a20e35956d330d_3966508656458729950.jpg",
            "http://niu.huang.xyz/12d4949b4009d089eaf071aef0f1f40.jpg",
            "http://niu.huang.xyz/1bff61de34bdc7bf40c6278b2848fbcf.jpg",
            "http://niu.huang.xyz/22fe8428428c93a565e181782e97654.jpg",
            "http://niu.huang.xyz/75e31415779979ae40c4c0238aa4c34.jpg",
            "http://niu.huang.xyz/905731909dfdafd0b53b3c4117438d3.jpg",
            "http://niu.huang.xyz/a84b1306e46061c0d664e6067417e5b.jpg",
            "http://niu.huang.xyz/b93d640cc856cb7035a851029aec190.jpg",
            "http://niu.huang.xyz/c11ae3862b3ca45b0a6cdff1e1bf841.jpg",
            "http://niu.huang.xyz/cccfb0995f5d103414bd8a8bd742c34.jpg",
            "http://niu.huang.xyz/f870176b1a628623fa7fe9918b862d7.jpg"};
    private static final String[] OTHER_TAGS = {"\"java\"", "\"python\"", "\"c\"", "\"c++\"", "\"c#\"", "\"html/css\"", "\"vue\"", "\"react\""};
    private static final String[] SCHOOL_YEARS = {"\"高一\"", "\"高二\"", "\"高三\"", "\"大一\"", "\"大二\"", "\"大三\"", "\"大四\"", "\"研究生\"", "\"已工作\""};
    private static final String[] FIRST_NAMES = {"Alice", "Bob", "Charlie", "David", "Emily", "Frank", "Grace", "Henry", "Isabella", "Jack", "Kate", "Liam", "Mia", "Nathan", "Olivia", "Peter", "Quinn", "Rachel", "Sarah", "Tyler", "Ursula", "Victoria", "William", "Xander", "Yvonne", "Zachary"};
    private static final String[] LAST_NAMES = {"Anderson", "Brown", "Clark", "Davis", "Evans", "Ford", "Garcia", "Harris", "Isaacs", "Johnson", "Klein", "Lee", "Miller", "Nguyen", "O'Brien", "Parker", "Quinn", "Roberts", "Smith", "Taylor", "Ueda", "Valdez", "Williams", "Xu", "Yamamoto", "Zhang"};
    private static final String[] EMAIL_DOMAINS = {"gmail.com", "yahoo.com", "hotmail.com", "outlook.com", "aol.com", "icloud.com", "protonmail.com", "yandex.com", "mail.com", "inbox.com"};
    private static final String[] GENDER = {"\"男\"", "\"女\"", "\"保密\""};

    @Test
    void insert() {
        Random random = new Random();
        for (int i = 0; i < 2; i++) {
            String randomUsername = getRandomString(10);
            ArrayList<String> randomTags = getRandomTags(random);
            String randomEmail = randomUsername + "@" + EMAIL_DOMAINS[random.nextInt(EMAIL_DOMAINS.length)];
            String randomProfile = getRandomProfile();
            String randomPhone = getRandomPhone();
            User user = new User();
            user.setId(null);
            user.setUsername(randomUsername);
            user.setPassword("12345678");
            user.setUserAccount(randomUsername.toLowerCase());
            user.setAvatarUrl(avatarUrls[random.nextInt(avatarUrls.length)]);
            user.setEmail(randomEmail);
            user.setProfile(randomProfile);
            user.setPhone(randomPhone);
            user.setTags(randomTags.toString());
            user.setRole(0);
            user.setGender(random.nextInt(2));
            user.setStatus(0);
            user.setIsDelete(0);
            userService.save(user);
        }
    }


    private static String getRandomPhone() {
        Random random = new Random();
        String phoneNumber = "1";
        for (int j = 0; j < 10; j++) {
            phoneNumber += random.nextInt(10);
        }
        return phoneNumber;
    }

    private static String getRandomProfile() {
        String[] adjectives = {"开心的", "难过的", "刺激的", "无聊的", "有趣的", "搞笑的", "严肃的", "有创意的", "懒惰的", "充满活力的", "美丽的", "聪明的", "勇敢的", "诚实的", "慷慨的", "有趣的", "有创意的", "有条理的", "有耐心的", "有决心的", "有毅力的", "有同情心的", "有幽默感的", "有冒险精神的", "有野心的", "有魅力的", "有自信的", "有智慧的", "有礼貌的", "有耳聪目明的", "有远见的", "有责任心的", "有领导才能的"};
        String[] nouns = {"学生", "教师", "程序员", "艺术家", "作家", "音乐家", "运动员", "厨师", "科学家", "企业家", "医生", "教师", "律师", "工程师", "会计师", "程序员", "销售员", "市场营销人员", "记者", "作家", "演员", "音乐家", "画家", "建筑师", "设计师", "厨师", "服务员", "警察", "消防员", "军人", "运动员", "教练", "经理", "企业家", "政治家", "科学家", "研究员", "救援人员"};
        Random random = new Random();
        String adjective = adjectives[random.nextInt(adjectives.length)];
        String noun = nouns[random.nextInt(nouns.length)];
        return "我是一个" + adjective + noun + "。";
    }

    private static String getRandomUsername(Random random) {
        String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
        String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
        return (firstName + lastName);
    }

    private static ArrayList<String> getRandomTags(Random random) {
        ArrayList<String> randomTags = getRandomValue(OTHER_TAGS, random);
        randomTags.add(getRandomUniqueValue(SCHOOL_YEARS, random));
        String randomGender = getRandomGender(random);
        if (randomGender != null) {
            randomTags.add(randomGender);
        }
        return randomTags;
    }

    private static String getRandomGender(Random random) {
        int randomIndex = random.nextInt(4);
        if (randomIndex == 3) {
            return null;
        } else {
            return GENDER[randomIndex];
        }
    }

    private static ArrayList<String> getRandomValue(String[] values, Random random) {
        ArrayList<String> tagList = new ArrayList<>();
        int randomTagNum = random.nextInt(3) + 1;
        for (int i = 0; i < randomTagNum; i++) {
            tagList.add(values[random.nextInt(values.length)]);
        }
        return tagList;
    }

    private static String getRandomUniqueValue(String[] values, Random random) {
        return values[random.nextInt(values.length)];
    }

    private static String getRandomString(int length) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(62);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }

    @Test
    public void importUserGEOByRedis() {
        List<User> userList = userService.list(); // 查询所有用户
        String key = RedisConstants.USER_GEO_KEY; // Redis的key
        List<RedisGeoCommands.GeoLocation<String>> locationList = new ArrayList<>(userList.size()); // 初始化地址（经纬度）List
        for (User user : userList) {
            locationList.add(new RedisGeoCommands.GeoLocation<>(String.valueOf(user.getId()), new Point(user.getLongitude(),
                    user.getDimension()))); // 往locationList添加每个用户的经纬度数据
        }
        stringRedisTemplate.opsForGeo().add(key, locationList); // 将每个用户的经纬度信息写入Redis中
    }
    @Test
    public void getUserGeo() {
        String key = RedisConstants.USER_GEO_KEY;
        List<User> userList = userService.list();
        // 计算每个用户与登录用户的距离
        for (User user : userList) {
            Distance distance = stringRedisTemplate.opsForGeo().distance(key,
                    "1", String.valueOf(user.getId()), RedisGeoCommands.DistanceUnit.KILOMETERS);
            assert distance != null;
            System.out.println("User: " + user.getId() + ", Distance: " +
                    distance.getValue() + " " + distance.getUnit());
        }
    }
    @Test
    public void searchUserByGeo() {
        User loginUser = userService.getById(1);
        Distance geoRadius = new Distance(1500, RedisGeoCommands.DistanceUnit.KILOMETERS);
        Circle circle = new Circle(new Point(loginUser.getLongitude(), loginUser.getDimension()), geoRadius);
        RedisGeoCommands.GeoRadiusCommandArgs geoRadiusCommandArgs = RedisGeoCommands.GeoRadiusCommandArgs
                .newGeoRadiusArgs().includeCoordinates();
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo().radius(RedisConstants.USER_GEO_KEY, circle, geoRadiusCommandArgs);
        assert results != null;
        for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : results) {
            if (!result.getContent().getName().equals("1")) {
                System.out.println(result.getContent().getName()); // // 打印1500km内的用户id
            }
        }
    }

    @Test
    void testCompareTags() {
        List<String> tagList1 = Arrays.asList("大一", "C#", "C++", "react");
        List<String> tagList2 = Arrays.asList("react", "大一", "保密");
        List<String> tagList3 = Arrays.asList("C#", "Vue", "大一");
        List<String> tagList4 = Arrays.asList("Python", "大二", "女", "C++", "java", "react", "C#");
        List<String> tagList5 = Arrays.asList("Python", "大二", "女");
        List<String> tagList6 = Arrays.asList("Python", "大二", "女");
        List<String> tagList7 = Arrays.asList("Python", "大二", "女");
        List<String> tagList8 = Arrays.asList("Python", "大二", "女");
        List<String> tagList9 = Arrays.asList("Python", "大二", "女");
        List<String> tagList10 = Arrays.asList("Python", "大二", "女");
//        ["大二","C#","C++"]
//        [""python"", ""高一"", ""女""]
//        [""c#"", ""vue"", ""大一"", ""保密""]
//        [""python"", ""c++"", ""大三"", ""男""]
//        [""c#"", ""c++"", ""大三""]
//        [""java"", ""java"", ""html/css"", ""研究生""]
//        "[""python"", ""react"", ""java"", ""大二"", ""保密""]
//        "[""react"", ""大四"", ""男""]
//        "[""java"", ""高一"", ""女""]
//        "[""react"", ""大四"", ""男""]
//        "[""Vue"", ""初一"", ""男""]"
//        "[""c++"", ""java"", ""已工作""]
//        "[""vue"", ""c"", ""python"", ""高二"", ""保密""]
//        "[""c++"", ""python"", ""高二"", ""保密""]
//        "[""java"", ""java"", ""html/css"", ""大三"", ""男""]
//        "[""java"", ""高一"", ""男""]
//        "[""react"", ""大一"", ""保密""]
//        "[""html/css"", ""大一"", ""女""]
//        "[""html/css"", ""大二""]


        // 1
        int score1 = AlgorithmUtil.minDistance(tagList1, tagList2);
        // 3
        int score2 = AlgorithmUtil.minDistance(tagList1, tagList3);
        int score3 = AlgorithmUtil.minDistance(tagList1, tagList4);
        System.out.println(score1);
        System.out.println(score2);
        System.out.println(score3);
    }
}
