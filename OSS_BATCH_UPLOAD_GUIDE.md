# 批量上传 OSS 并同步数据库 — 操作手册

> 适用场景：大量本地图片一次性上传至阿里云 OSS，并将 URL 回写到数据库对应字段。

---

## 一、整体流程

```
扫描本地目录 → 匹配数据库记录 → 逐个上传 OSS → 获取 URL → 更新数据库
```

## 二、涉及的类和方法

| 类/方法 | 位置 | 作用 |
|---------|------|------|
| `OssServiceImpl.uploadLocalFile()` | `service/impl/OssServiceImpl.java:121` | 本地文件直传 OSS |
| `peizhi.java` | `common/config/peizhi.java` | OSS 客户端 Bean 配置 |
| `SchoolMapper` | `mapper/SchoolMapper.java` | 学校表 CRUD |
| `SchoolImageMapper` | `mapper/SchoolImageMapper.java` | 学校图片表 CRUD |

## 三、uploadLocalFile 方法签名

```java
/**
 * @param localPath  本地文件完整路径，如 C:\Users\Admin\Desktop\logos\上海中学.png
 * @param objectName OSS 存储路径，如 logo/171.png
 * @return 完整 HTTPS URL，如 https://lianpeng.oss-cn-shanghai.aliyuncs.com/logo/171.png
 */
public String uploadLocalFile(String localPath, String objectName)
```

OSS URL 拼接规则（`buildUrl` 方法）：

```java
"https://" + bucketName + "." + endpoint + "/" + objectName
// 即: https://lianpeng.oss-cn-shanghai.aliyuncs.com/logo/171.png
```

## 四、场景一：Logo 批量上传 → school.logo_url

### 目录结构

```
C:\Users\Admin\Desktop\logos\
├── 上海中学.png
├── 复旦附中.png
├── 交大附中.png
└── ...
```

### 上传规则

- **匹配方式**：文件名 = 学校名称（精确匹配 `school.name`）
- **OSS 路径**：`logo/{schoolId}.png`
- **更新字段**：`school.logo_url`

### 工具类模板

```java
import com.example.lpsx.LpsxApplication;
import com.example.lpsx.entity.School;
import com.example.lpsx.mapper.SchoolMapper;
import com.example.lpsx.service.impl.OssServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.File;

public class LogoBatchUploader {

    public static void main(String[] args) {
        // 1. 启动 Spring 容器
        ConfigurableApplicationContext ctx =
            SpringApplication.run(LpsxApplication.class, args);

        OssServiceImpl ossService = ctx.getBean(OssServiceImpl.class);
        SchoolMapper schoolMapper = ctx.getBean(SchoolMapper.class);

        // 2. 扫描本地文件夹
        File rootDir = new File("C:\\Users\\Admin\\Desktop\\logos");
        File[] files = rootDir.listFiles();
        if (files == null) {
            System.out.println("目录为空或不存在");
            ctx.close();
            return;
        }

        int success = 0, skip = 0;
        for (File file : files) {
            if (!file.isFile()) continue;

            // 3. 文件名去掉扩展名 = 学校名称
            String schoolName = file.getName();
            int dotIdx = schoolName.lastIndexOf('.');
            if (dotIdx > 0) schoolName = schoolName.substring(0, dotIdx);

            // 4. 查数据库匹配学校
            School school = schoolMapper.selectOne(
                new LambdaQueryWrapper<School>().eq(School::getName, schoolName));

            if (school == null) {
                System.out.println("[跳过] 未匹配到学校: " + schoolName);
                skip++;
                continue;
            }

            try {
                // 5. 上传 OSS
                String ossPath = "logo/" + school.getId() + ".png";
                String url = ossService.uploadLocalFile(
                    file.getAbsolutePath(), ossPath);

                // 6. 更新数据库
                school.setLogoUrl(url);
                schoolMapper.updateById(school);

                System.out.println("[完成] " + schoolName + " → " + url);
                success++;
            } catch (Exception e) {
                System.err.println("[失败] " + schoolName + ": " + e.getMessage());
            }
        }

        System.out.println("===== 上传结束 =====");
        System.out.println("成功: " + success + ", 跳过: " + skip);
        ctx.close();
    }
}
```

## 五、场景二：学校卡片图上传 → school_image

### 目录结构

```
C:\Users\Admin\Desktop\schoolcard\
├── 上海中学\
│   ├── 01.png
│   ├── 02.png
│   └── 03.png
├── 复旦附中\
│   ├── 01.png
│   └── 02.png
└── ...
```

### 上传规则

- **匹配方式**：文件夹名 = 学校名称
- **OSS 路径**：`school-gallery/{schoolId}/{序号}.png`
- **写入表**：`school_image`（每张图片一条记录）

### 工具类模板

```java
import com.example.lpsx.LpsxApplication;
import com.example.lpsx.entity.School;
import com.example.lpsx.entity.SchoolImage;
import com.example.lpsx.mapper.SchoolMapper;
import com.example.lpsx.mapper.SchoolImageMapper;
import com.example.lpsx.service.impl.OssServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class SchoolCardBatchUploader {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx =
            SpringApplication.run(LpsxApplication.class, args);

        OssServiceImpl ossService = ctx.getBean(OssServiceImpl.class);
        SchoolMapper schoolMapper = ctx.getBean(SchoolMapper.class);
        SchoolImageMapper schoolImageMapper = ctx.getBean(SchoolImageMapper.class);

        File rootDir = new File("C:\\Users\\Admin\\Desktop\\schoolcard");
        File[] schoolDirs = rootDir.listFiles(File::isDirectory);
        if (schoolDirs == null) {
            System.out.println("目录为空或不存在");
            ctx.close();
            return;
        }

        int totalSchools = 0, totalImages = 0;
        for (File schoolDir : schoolDirs) {
            String schoolName = schoolDir.getName();

            // 1. 匹配学校
            School school = schoolMapper.selectOne(
                new LambdaQueryWrapper<School>().eq(School::getName, schoolName));
            if (school == null) {
                System.out.println("[跳过] 未匹配到学校: " + schoolName);
                continue;
            }

            // 2. 获取文件夹内图片，按文件名排序
            File[] images = schoolDir.listFiles(
                (dir, name) -> name.toLowerCase().matches(".*\\.(png|jpg|jpeg|webp)$"));
            if (images == null || images.length == 0) {
                System.out.println("[跳过] 无图片: " + schoolName);
                continue;
            }
            Arrays.sort(images, Comparator.comparing(File::getName));

            // 3. 先清理旧记录（可选，避免重复）
            schoolImageMapper.delete(
                new LambdaQueryWrapper<SchoolImage>()
                    .eq(SchoolImage::getSchoolId, school.getId()));

            // 4. 逐张上传 + 入库
            for (int i = 0; i < images.length; i++) {
                try {
                    String ext = getExtension(images[i].getName());
                    String ossPath = "school-gallery/" + school.getId()
                        + "/" + (i + 1) + "." + ext;
                    String url = ossService.uploadLocalFile(
                        images[i].getAbsolutePath(), ossPath);

                    SchoolImage img = new SchoolImage();
                    img.setSchoolId(school.getId());
                    img.setImageUrl(url);
                    img.setImageType(1);      // 1=卡片图，按需定义
                    img.setSortOrder(i + 1);
                    schoolImageMapper.insert(img);

                    System.out.println("[完成] " + schoolName + " #" + (i+1) + " → " + url);
                    totalImages++;
                } catch (Exception e) {
                    System.err.println("[失败] " + schoolName + " #" + (i+1)
                        + ": " + e.getMessage());
                }
            }
            totalSchools++;
        }

        System.out.println("===== 上传结束 =====");
        System.out.println("学校数: " + totalSchools + ", 图片数: " + totalImages);
        ctx.close();
    }

    private static String getExtension(String filename) {
        int i = filename.lastIndexOf('.');
        return i > 0 ? filename.substring(i + 1).toLowerCase() : "png";
    }
}
```

## 六、执行步骤

1. **确认配置正确**：`application.yml` 中 OSS 配置无误，数据库可连接
2. **确认目录存在**：本地图片文件夹路径正确
3. **将工具类放入 `src/test/java`**（执行完删除）
4. **直接运行 main 方法**（IDEA 右键 → Run）
5. **观察控制台日志**，确认无大量失败
6. **验证**：查数据库确认 URL 已写入，浏览器访问 OSS URL 确认可打开
7. **删除工具类**，避免误提交

## 七、注意事项

| 注意事项 | 说明 |
|----------|------|
| **幂等性** | 重复执行会覆盖 OSS 同名文件；`school_image` 脚本已含删除旧记录逻辑 |
| **异常处理** | 单条失败不应中断整体，用 try-catch 包裹单个文件处理 |
| **文件扩展名** | OSS 路径保留原始扩展名，确保浏览器能正确渲染 |
| **中文文件名** | 本地路径可含中文，但 OSS 路径用数字 ID 避免编码问题 |
| **OSS 费用** | 注意 PUT 请求次数和下行流量，大量文件建议分批 |
| **Spring 容器开销** | 每次执行会启动完整 Spring Boot，约 3-5 秒，仅作一次性脚本使用 |

## 八、两种场景对比

| | Logo 上传 | 卡片图上传 |
|------|-----------|-------------|
| 本地结构 | 扁平文件 | 文件夹嵌套 |
| 匹配方式 | 文件名 = 学校名 | 文件夹名 = 学校名 |
| 目标表 | `school.logo_url` | `school_image`（多条） |
| 1校对应 | 1 张图 | 多张图 |
| OSS 路径 | `logo/{id}.png` | `school-gallery/{id}/{序号}.png` |
| 数据库操作 | `updateById` | `insert`（先删旧再插新） |
