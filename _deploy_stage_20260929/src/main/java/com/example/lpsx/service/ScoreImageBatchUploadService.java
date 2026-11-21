package com.example.lpsx.service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.lpsx.entity.ScoreQueryToolSchool;
import com.example.lpsx.entity.ScoreQueryToolSchoolImage;
import com.example.lpsx.mapper.ScoreQueryToolSchoolImageMapper;
import com.example.lpsx.mapper.ScoreQueryToolSchoolMapper;
import com.example.lpsx.service.impl.OssServiceImpl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * 查分工具-学校校徽&风光图批量上传服务（一次性工具）
 * <p>
 * 校徽目录  ：C:/Users/Admin/Desktop/校徽2          —— 每校 1 张 jpg → 上传后写 score_query_tool_school.logo
 * 风光图目录：C:/Users/Admin/Desktop/score_query_tool_schoolcard —— 每校 1 个文件夹、多张图 → 上传后写 score_query_tool_school_image
 * <p>
 * OSS 存放在独立顶层目录 {@code score-query/} 下，避免与桶内原有文件冲突：
 * 校徽   → score-query/school-logo/{schoolId}.{ext}
 * 风光图 → score-query/school-gallery/{schoolId}/{序号}.{ext}
 */
@Slf4j
@Service
public class ScoreImageBatchUploadService {

    @Value("${score-image.logo-dir:C:/Users/Admin/Desktop/校徽2}")
    private String logoDir;

    @Value("${score-image.gallery-dir:C:/Users/Admin/Desktop/score_query_tool_schoolcard}")
    private String galleryDir;

    /** OSS 独立顶层目录（避免与桶内原有文件冲突） */
    private static final String OSS_ROOT = "score-query";

    @Resource
    private OssServiceImpl ossService;

    @Resource
    private ScoreQueryToolSchoolMapper scoreQueryToolSchoolMapper;

    @Resource
    private ScoreQueryToolSchoolImageMapper scoreQueryToolSchoolImageMapper;

    /**
     * 执行批量上传
     *
     * @param mode all=校徽+风光图，logo=仅校徽，gallery=仅风光图
     */
    public Map<String, Object> upload(String mode) {
        // 构建 name → id 映射（含容错：去"上海市"前缀、括号归一化）
        Map<String, Integer> idByExact = new HashMap<>();
        Map<String, Integer> idByNoPrefix = new HashMap<>();
        Map<String, Integer> idByCanonical = new HashMap<>();
        List<ScoreQueryToolSchool> schools = scoreQueryToolSchoolMapper.selectList(null);
        for (ScoreQueryToolSchool s : schools) {
            String name = s.getName() == null ? "" : s.getName().trim();
            if (name.isEmpty()) {
                continue;
            }
            idByExact.putIfAbsent(name, s.getId());
            idByNoPrefix.putIfAbsent(name.replace("上海市", ""), s.getId());
            idByCanonical.putIfAbsent(canonical(name), s.getId());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        if ("all".equals(mode) || "logo".equals(mode)) {
            result.put("logo", uploadLogos(idByExact, idByNoPrefix, idByCanonical));
        }
        if ("all".equals(mode) || "gallery".equals(mode)) {
            result.put("gallery", uploadGalleries(idByExact, idByNoPrefix, idByCanonical));
        }
        return result;
    }

    // ==================== 校徽 ====================

    private Map<String, Object> uploadLogos(Map<String, Integer> exact,
                                            Map<String, Integer> noPrefix,
                                            Map<String, Integer> canonical) {
        Map<String, Object> stat = new LinkedHashMap<>();
        File dir = new File(logoDir);
        if (!dir.isDirectory()) {
            stat.put("error", "校徽目录不存在: " + logoDir);
            return stat;
        }
        File[] files = dir.listFiles((d, n) -> isImage(n));
        if (files == null) {
            files = new File[0];
        }
        Arrays.sort(files, Comparator.comparing(File::getName));

        List<String> unmatched = new ArrayList<>();
        int success = 0;
        int failed = 0;
        for (File f : files) {
            String fileName = stripExt(f.getName());
            Integer id = matchId(fileName, exact, noPrefix, canonical);
            if (id == null) {
                unmatched.add(f.getName());
                continue;
            }
            String objectName = OSS_ROOT + "/school-logo/" + id + extOf(f.getName());
            try {
                String url = ossService.uploadLocalFile(f.getAbsolutePath(), objectName);
                scoreQueryToolSchoolMapper.update(null, new LambdaUpdateWrapper<ScoreQueryToolSchool>()
                        .eq(ScoreQueryToolSchool::getId, id)
                        .set(ScoreQueryToolSchool::getLogo, url));
                success++;
            } catch (Exception e) {
                failed++;
                log.error("校徽上传失败: {} -> {}", f.getName(), e.getMessage(), e);
            }
        }
        stat.put("total", files.length);
        stat.put("success", success);
        stat.put("failed", failed);
        stat.put("unmatched", unmatched);
        return stat;
    }

    // ==================== 风光图 ====================

    private Map<String, Object> uploadGalleries(Map<String, Integer> exact,
                                                Map<String, Integer> noPrefix,
                                                Map<String, Integer> canonical) {
        Map<String, Object> stat = new LinkedHashMap<>();
        File dir = new File(galleryDir);
        if (!dir.isDirectory()) {
            stat.put("error", "风光图目录不存在: " + galleryDir);
            return stat;
        }
        File[] folders = dir.listFiles(File::isDirectory);
        if (folders == null) {
            folders = new File[0];
        }
        Arrays.sort(folders, Comparator.comparing(File::getName));

        List<String> unmatched = new ArrayList<>();
        int schoolDone = 0;
        int imageInserted = 0;
        int failed = 0;
        for (File folder : folders) {
            String folderName = folder.getName().trim();
            Integer id = matchId(folderName, exact, noPrefix, canonical);
            if (id == null) {
                unmatched.add(folder.getName());
                continue;
            }
            File[] imgs = folder.listFiles((d, n) -> isImage(n));
            if (imgs == null || imgs.length == 0) {
                unmatched.add(folder.getName() + "（空文件夹）");
                continue;
            }
            Arrays.sort(imgs, Comparator.comparing(File::getName));

            int order = 1;
            for (File img : imgs) {
                String objectName = OSS_ROOT + "/school-gallery/" + id
                        + "/" + String.format("%02d", order) + extOf(img.getName());
                try {
                    String url = ossService.uploadLocalFile(img.getAbsolutePath(), objectName);
                    ScoreQueryToolSchoolImage rec = new ScoreQueryToolSchoolImage();
                    rec.setSchoolId(id);
                    rec.setImageUrl(url);
                    rec.setSortOrder(order);
                    rec.setCreatedTime(LocalDateTime.now());
                    scoreQueryToolSchoolImageMapper.insert(rec);
                    imageInserted++;
                    order++;
                } catch (Exception e) {
                    failed++;
                    log.error("风光图上传失败: {} -> {}", img.getName(), e.getMessage(), e);
                }
            }
            schoolDone++;
        }
        stat.put("schoolTotal", folders.length);
        stat.put("schoolDone", schoolDone);
        stat.put("imageInserted", imageInserted);
        stat.put("failed", failed);
        stat.put("unmatched", unmatched);
        return stat;
    }

    // ==================== 匹配工具 ====================

    /**
     * 文件名/文件夹名 → schoolId 容错匹配
     * 顺序：精确 → 去"上海市"前缀 → 括号归一化 → 归一化+补右括号 → 去前缀+归一化
     */
    private Integer matchId(String rawName,
                            Map<String, Integer> exact,
                            Map<String, Integer> noPrefix,
                            Map<String, Integer> canonical) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty()) {
            return null;
        }
        // 1. 精确匹配
        Integer id = exact.get(name);
        if (id != null) {
            return id;
        }
        // 2. 去"上海市"前缀
        String noPre = name.replace("上海市", "");
        if (!noPre.isEmpty()) {
            id = exact.get(noPre);
            if (id == null) {
                id = noPrefix.get(name);
            }
            if (id != null) {
                return id;
            }
        }
        // 3. 括号归一化
        String c = canonical(name);
        id = canonical.get(c);
        if (id != null) {
            return id;
        }
        // 4. 归一化 + 补右括号（如 "华东师范大学第二附属中学（普陀校区" → "(普陀校区)"）
        if (!c.endsWith(")") && c.contains("(")) {
            id = canonical.get(c + ")");
            if (id != null) {
                return id;
            }
        }
        // 5. 去前缀 + 归一化
        String c3 = canonical(noPre);
        id = canonical.get(c3);
        if (id != null) {
            return id;
        }
        if (!c3.endsWith(")") && c3.contains("(")) {
            id = canonical.get(c3 + ")");
            if (id != null) {
                return id;
            }
        }
        return null;
    }

    /** 括号归一化：全角→半角、去空白、（若以左括号结尾则补右括号） */
    private String canonical(String s) {
        String t = s.trim().replace('（', '(').replace('）', ')')
                .replace(" ", "").replace("　", "");
        if (t.endsWith("(")) {
            t = t + ")";
        }
        return t;
    }

    /** 去掉扩展名 */
    private String stripExt(String name) {
        int i = name.lastIndexOf('.');
        return i > 0 ? name.substring(0, i) : name;
    }

    /** 取扩展名（含点，小写） */
    private String extOf(String name) {
        int i = name.lastIndexOf('.');
        return i >= 0 ? name.substring(i).toLowerCase() : "";
    }

    /** 是否为图片文件 */
    private boolean isImage(String name) {
        String e = extOf(name);
        return ".jpg".equals(e) || ".jpeg".equals(e) || ".png".equals(e)
                || ".webp".equals(e) || ".bmp".equals(e);
    }
}
