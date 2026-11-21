import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 将本地政策附件（PDF）迁移到项目 OSS，并回写本地附件表 file_url。 */
public final class UploadPolicyAttachments {
    record Attachment(long id,long sourceId,long sourceAttachmentId,String fileName,String sourceUrl) {}
    public static void main(String[] args) throws Exception {
        String yaml=Files.readString(Path.of("src/main/resources/application.yml"),StandardCharsets.UTF_8);
        Map<String,String> db=config(yaml); String pwd=System.getenv("DB_PASSWORD"); if(pwd!=null&&!pwd.isBlank())db.put("password",pwd);
        String endpoint=value(yaml,"endpoint"),bucket=value(yaml,"bucket-name"),ak=resolved(yaml,"access-key-id"),sk=resolved(yaml,"access-key-secret");
        List<Attachment> rows=new ArrayList<>();
        try(Connection c=DriverManager.getConnection(db.get("url").replace("192.168.3.5","127.0.0.1"),db.get("username"),db.get("password"));PreparedStatement s=c.prepareStatement("SELECT a.id,p.source_id,a.source_attachment_id,a.file_name,a.source_url FROM education_policy_attachment a JOIN education_policy p ON p.id=a.policy_id WHERE a.media_type='pdf' AND a.is_deleted=0 AND (a.file_url IS NULL OR a.file_url='') ORDER BY p.source_id,a.sort_order,a.id");ResultSet r=s.executeQuery()){while(r.next())rows.add(new Attachment(r.getLong(1),r.getLong(2),r.getLong(3),r.getString(4),r.getString(5)));}
        System.out.println("PDF_ATTACHMENTS="+rows.size()); if(rows.isEmpty())return;
        Path temp=Files.createTempDirectory("policy-pdf-"); HttpClient http=HttpClient.newBuilder().build(); OSS oss=new OSSClientBuilder().build(endpoint,ak,sk);
        try(Connection c=DriverManager.getConnection(db.get("url").replace("192.168.3.5","127.0.0.1"),db.get("username"),db.get("password"))){c.setAutoCommit(false);try{for(Attachment a:rows){if(a.sourceUrl()==null||a.fileName()==null)throw new IllegalStateException("附件缺少来源地址或文件名 id="+a.id());String safe=a.fileName().replaceAll("[\\\\/:*?\"<>|]","_");String object="education-policy/"+a.sourceId()+"/"+a.sourceAttachmentId()+"_"+safe;String url="https://"+bucket+"."+endpoint+"/"+object;Path local=temp.resolve(a.id()+".pdf");HttpResponse<Path> response=http.send(HttpRequest.newBuilder(URI.create(a.sourceUrl())).timeout(java.time.Duration.ofSeconds(60)).GET().build(),HttpResponse.BodyHandlers.ofFile(local));if(response.statusCode()!=200)throw new IllegalStateException("下载失败 HTTP "+response.statusCode()+" id="+a.id());if(Files.size(local)<100)throw new IllegalStateException("下载文件过小 id="+a.id());oss.putObject(bucket,object,local.toFile());if(!oss.doesObjectExist(bucket,object))throw new IllegalStateException("OSS 上传后不存在 id="+a.id());try(PreparedStatement u=c.prepareStatement("UPDATE education_policy_attachment SET file_url=? WHERE id=?")){u.setString(1,url);u.setLong(2,a.id());u.executeUpdate();}System.out.println("UPLOADED\t"+a.id()+"\t"+url);}c.commit();}catch(Exception e){c.rollback();throw e;}}finally{oss.shutdown();try(var files=Files.list(temp)){files.forEach(p->{try{Files.deleteIfExists(p);}catch(Exception ignored){}});}Files.deleteIfExists(temp);}
        System.out.println("APPLIED AND VERIFIED: pdfs="+rows.size());
    }
    static String value(String y,String k){Matcher m=Pattern.compile("(?m)^\\s*"+Pattern.quote(k)+":\\s*([^\\r\\n#]+)").matcher(y);if(!m.find())throw new IllegalStateException("缺少配置 "+k);return m.group(1).trim();}
    static String resolved(String y,String k){String v=value(y,k);Matcher m=Pattern.compile("\\$\\{([^:}]+):([^}]+)}").matcher(v);if(!m.matches())return v;String e=System.getenv(m.group(1));return e==null||e.isBlank()?m.group(2):e;}
    static Map<String,String> config(String y){Map<String,String> m=new HashMap<>();for(String k:List.of("url","username","password"))m.put(k,value(y,k).replaceAll("^[\"']|[\"']$",""));return m;}
}
