import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 从 policy-list / policy-detail HAR 响应建表并幂等导入政策及附件。 */
public final class ImportPoliciesFromHar {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern YAML_VALUE = Pattern.compile("(?m)^\\s*(url|username|password):\\s*([^\\r\\n#]+)");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String SOURCE = "anroader";
    private static final String SOURCE_BASE = "https://www.anroader.com/upload/";

    public static void main(String[] args) throws Exception {
        if (args.length == 0) throw new IllegalArgumentException("Usage: ImportPoliciesFromHar <政策.har> [--apply] [--cloud]");
        boolean apply = java.util.Arrays.asList(args).contains("--apply");
        boolean cloud = java.util.Arrays.asList(args).contains("--cloud");
        Map<Long, JsonNode> policies = readHar(Path.of(args[0]));
        if (policies.isEmpty()) throw new IllegalStateException("HAR 中没有找到政策数据");
        int attachmentCount = policies.values().stream().mapToInt(p -> {
            int count=0; for(JsonNode file:p.path("filePath")) if(shouldImportAttachment(file)) count++; return count;
        }).sum();
        System.out.printf("mode=%s policies=%d attachments=%d%n", apply ? "APPLY" : "PREVIEW", policies.size(), attachmentCount);
        policies.values().forEach(p -> System.out.printf("- source#%d [%s/%s] %s (%s) files=%d%n",
                p.path("id").asLong(), text(p, "category"), text(p, "level"), text(p, "name"), text(p, "publicDate"), p.path("filePath").size()));
        if (!apply) { System.out.println("PREVIEW ONLY: 数据库未修改。确认后追加 --apply。"); return; }

        Path config = Path.of(cloud ? "src/main/resources/application-cloud.yml" : "src/main/resources/application.yml");
        Map<String, String> db = readDatabaseConfig(Files.readString(config, StandardCharsets.UTF_8));
        String passwordOverride = System.getenv("DB_PASSWORD");
        if (passwordOverride != null && !passwordOverride.isBlank()) db.put("password", passwordOverride);
        // 本机 MySQL 的 root 通常只允许 localhost；LAN 地址主要供其他电脑访问后端 HTTP。
        if (db.get("url").startsWith("jdbc:mysql://192.168.3.5:")) {
            db.put("url", db.get("url").replace("jdbc:mysql://192.168.3.5:", "jdbc:mysql://127.0.0.1:"));
        }
        try (Connection connection = DriverManager.getConnection(db.get("url"), db.get("username"), db.get("password"))) {
            connection.setAutoCommit(false);
            try {
                executeSchema(connection);
                for (JsonNode policy : policies.values()) upsertPolicy(connection, policy);
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
            verify(connection, policies.size(), attachmentCount);
        }
    }

    private static Map<Long, JsonNode> readHar(Path path) throws Exception {
        String raw = Files.readString(path, StandardCharsets.UTF_8);
        if (!raw.isEmpty() && raw.charAt(0) == '\uFEFF') raw = raw.substring(1);
        JsonNode root = JSON.readTree(raw);
        Map<Long, JsonNode> result = new LinkedHashMap<>();
        for (JsonNode entry : root.path("log").path("entries")) {
            String url = entry.path("request").path("url").asText();
            if (!url.contains("/api/mini/policy-list") && !url.contains("/api/mini/policy-detail")) continue;
            JsonNode content = entry.path("response").path("content");
            String body = content.path("text").asText("");
            if ("base64".equalsIgnoreCase(content.path("encoding").asText())) body = new String(Base64.getDecoder().decode(body), StandardCharsets.UTF_8);
            if (body.isBlank()) continue;
            JsonNode data;
            try { data = JSON.readTree(body).path("data"); } catch (Exception ignored) { continue; }
            if (url.contains("policy-list")) for (JsonNode row : data.path("records")) add(result, row);
            else add(result, data);
        }
        return result;
    }

    private static void add(Map<Long, JsonNode> result, JsonNode row) {
        long id = row.path("id").asLong();
        if (id <= 0 || text(row, "name") == null) return;
        JsonNode old = result.get(id);
        if (old == null || row.path("filePath").size() >= old.path("filePath").size()) result.put(id, row);
    }

    private static void executeSchema(Connection c) throws Exception {
        String sql = Files.readString(Path.of("tools/add_education_policy.sql"), StandardCharsets.UTF_8);
        try (Statement s = c.createStatement()) {
            for (String part : sql.split(";\\s*(?:\\r?\\n|$)")) if (!part.isBlank()) s.execute(part);
        }
    }

    private static void upsertPolicy(Connection c, JsonNode p) throws Exception {
        String sql = "INSERT INTO education_policy(source_platform,source_id,name,category,education_level,policy_type,province,city,address,summary,content,public_date,policy_year,status,source_create_time,source_update_time,is_deleted) " +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE name=VALUES(name),category=VALUES(category),education_level=VALUES(education_level),policy_type=VALUES(policy_type),province=VALUES(province),city=VALUES(city),address=VALUES(address),summary=VALUES(summary),content=VALUES(content),public_date=VALUES(public_date),policy_year=VALUES(policy_year),status=VALUES(status),source_create_time=VALUES(source_create_time),source_update_time=VALUES(source_update_time),is_deleted=VALUES(is_deleted)";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            int i=1; s.setString(i++, SOURCE); s.setLong(i++, p.path("id").asLong()); s.setString(i++, text(p,"name"));
            s.setString(i++, text(p,"category")); s.setString(i++, text(p,"level")); s.setString(i++, text(p,"type"));
            String province = normalizeProvince(text(p,"province"));
            s.setString(i++, province); s.setString(i++, normalizeCity(text(p,"city"), province)); s.setString(i++, normalizeAddress(text(p,"address")));
            s.setString(i++, text(p,"provinceDesc")); s.setString(i++, text(p,"provinceContent"));
            setTime(s,i++,text(p,"publicDate")); Integer year=year(text(p,"publicDate")); if(year==null)s.setNull(i++,java.sql.Types.SMALLINT);else s.setInt(i++,year);
            s.setString(i++, text(p,"status")); setTime(s,i++,text(p,"createDate")); setTime(s,i++,text(p,"updateDate")); s.setInt(i,p.path("isDeleted").asInt(0)); s.executeUpdate();
        }
        long policyId;
        try (PreparedStatement s=c.prepareStatement("SELECT id FROM education_policy WHERE source_platform=? AND source_id=?")){s.setString(1,SOURCE);s.setLong(2,p.path("id").asLong());try(ResultSet r=s.executeQuery()){r.next();policyId=r.getLong(1);}}
        int order=0;
        for(JsonNode file:p.path("filePath")) {
            if (shouldImportAttachment(file)) upsertAttachment(c,policyId,file,order++);
        }
    }

    private static void upsertAttachment(Connection c,long policyId,JsonNode f,int order)throws Exception{
        String path=text(f,"filePath"), ext=extension(path), media=("png".equals(ext)||"jpg".equals(ext)||"jpeg".equals(ext)||"webp".equals(ext))?"image":"pdf".equals(ext)?"pdf":"file";
        String sql="INSERT INTO education_policy_attachment(policy_id,source_attachment_id,file_name,file_path,source_url,file_extension,media_type,is_cover,sort_order,is_deleted,source_create_time,source_update_time) VALUES(?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE file_name=VALUES(file_name),file_path=VALUES(file_path),source_url=VALUES(source_url),file_extension=VALUES(file_extension),media_type=VALUES(media_type),is_cover=VALUES(is_cover),sort_order=VALUES(sort_order),is_deleted=VALUES(is_deleted),source_create_time=VALUES(source_create_time),source_update_time=VALUES(source_update_time)";
        try(PreparedStatement s=c.prepareStatement(sql)){int i=1;s.setLong(i++,policyId);s.setLong(i++,f.path("id").asLong());s.setString(i++,text(f,"fileName"));s.setString(i++,path);s.setString(i++,path==null?null:SOURCE_BASE+path);s.setString(i++,ext);s.setString(i++,media);s.setInt(i++,order==0&&"image".equals(media)?1:0);s.setInt(i++,order);s.setInt(i++,f.path("isDeleted").asInt(0));setTime(s,i++,text(f,"createDate"));setTime(s,i,text(f,"updateDate"));s.executeUpdate();}
    }

    private static void verify(Connection c,int expectedPolicies,int expectedFiles)throws Exception{
        try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT (SELECT COUNT(*) FROM education_policy WHERE source_platform='anroader') policies,(SELECT COUNT(*) FROM education_policy_attachment a JOIN education_policy p ON p.id=a.policy_id WHERE p.source_platform='anroader') attachments")){r.next();int pc=r.getInt(1),ac=r.getInt(2);System.out.printf("APPLIED AND VERIFIED: policies=%d attachments=%d%n",pc,ac);if(pc!=expectedPolicies||ac!=expectedFiles)throw new IllegalStateException("导入条数不符合预期");}
    }
    private static String text(JsonNode n,String field){String v=n.path(field).isNull()?null:n.path(field).asText(null);return v==null||v.isBlank()?null:v.trim();}
    private static Integer year(String value){if(value==null||value.length()<4)return null;try{return Integer.parseInt(value.substring(0,4));}catch(Exception e){return null;}}
    private static String extension(String path){if(path==null)return null;int dot=path.lastIndexOf('.');return dot<0?null:path.substring(dot+1).toLowerCase();}
    private static boolean shouldImportAttachment(JsonNode file){
        String path=text(file,"filePath"), ext=extension(path), name=text(file,"fileName");
        boolean image="png".equals(ext)||"jpg".equals(ext)||"jpeg".equals(ext)||"webp".equals(ext)||"gif".equals(ext);
        if(!image)return true;
        if(name==null||name.startsWith("微信图片"))return false;
        int dot=name.lastIndexOf('.');
        String stem=dot<0?name:name.substring(0,dot);
        return !stem.matches("[0-9A-Za-z_-]+");
    }
    private static String normalizeProvince(String value){return value!=null&&("上海".equals(value)||"上海市".equals(value))?"上海市":value;}
    private static String normalizeCity(String value,String province){if(value==null)return null;String normalized=normalizeProvince(value);return normalized.equals(province)?null:value;}
    private static String normalizeAddress(String value){if(value==null)return null;String compact=value.replace(".","").replace("·","").replace(" ","").replace("市","");return compact.matches("上海(上海)?")?null:value;}
    private static void setTime(PreparedStatement s,int index,String value)throws Exception{if(value==null)s.setNull(index,java.sql.Types.TIMESTAMP);else s.setObject(index,LocalDateTime.parse(value,TIME));}
    private static Map<String,String> readDatabaseConfig(String yaml){Map<String,String> values=new LinkedHashMap<>();Matcher m=YAML_VALUE.matcher(yaml);while(m.find()&&!values.keySet().containsAll(java.util.Set.of("url","username","password")))values.putIfAbsent(m.group(1),unquote(m.group(2).trim()));for(String key:java.util.List.of("url","username","password"))if(!values.containsKey(key))throw new IllegalStateException("application.yml 缺少 datasource."+key);return values;}
    private static String unquote(String value){if(value.length()>1&&((value.startsWith("\"")&&value.endsWith("\""))||(value.startsWith("'")&&value.endsWith("'"))))return value.substring(1,value.length()-1);return value;}
}
