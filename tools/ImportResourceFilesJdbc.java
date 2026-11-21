import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;

/** Database-only importer. It deliberately has no OSS dependency or upload code. */
public class ImportResourceFilesJdbc {
  public static void main(String[] args) throws Exception {
    if (args.length != 2) throw new IllegalArgumentException("Usage: ImportResourceFilesJdbc <root> <category>");
    Path root=Path.of(args[0]).toAbsolutePath().normalize(); String category=args[1];
    List<Path> files; try(var s=Files.walk(root)){ files=s.filter(Files::isRegularFile).filter(p->Set.of("pdf","docx").contains(ext(p))).sorted().toList(); }
    String url="jdbc:mysql://192.168.3.5:3306/lpsx?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true";
    try(Connection c=DriverManager.getConnection(url,"root","Datou123456!")) {
      c.setAutoCommit(false); int order=1, inserted=0, reused=0;
      String find="SELECT id FROM zhongkao_resource_file WHERE file_sha256=?";
      String sql="INSERT INTO zhongkao_resource_file (resource_code,title,resource_category,subject_category,resource_type,target_year,source_year,original_file_name,relative_path,file_extension,mime_type,file_size,file_sha256,file_content,oss_url,source_batch,sort_order,status,is_deleted) VALUES (?,?,?,?,?,'2026',?,?,?,?,?,?,?,?,NULL,?,?,1,0) ON DUPLICATE KEY UPDATE title=VALUES(title),resource_category=VALUES(resource_category),file_content=VALUES(file_content),file_size=VALUES(file_size),source_batch=VALUES(source_batch),sort_order=VALUES(sort_order),status=1,is_deleted=0";
      for(Path p:files){ String hash=sha(p); try(PreparedStatement q=c.prepareStatement(find)){q.setString(1,hash); try(ResultSet rs=q.executeQuery()){if(rs.next()){reused++; continue;}}}
        String name=p.getFileName().toString(), extension=ext(p), title=name.substring(0,name.length()-extension.length()-1), code="z2026-"+hash.substring(0,16).toLowerCase(Locale.ROOT);
        try(PreparedStatement st=c.prepareStatement(sql); InputStream in=Files.newInputStream(p)){int i=1; st.setString(i++,code);st.setString(i++,title);st.setString(i++,category);st.setString(i++,category);st.setString(i++,extension.equals("pdf")?"资料文件":"文档模板");st.setString(i++,"");st.setString(i++,name);st.setString(i++,root.relativize(p).toString().replace('\\','/'));st.setString(i++,extension);st.setString(i++,extension.equals("pdf")?"application/pdf":"application/vnd.openxmlformats-officedocument.wordprocessingml.document");st.setLong(i++,Files.size(p));st.setString(i++,hash);st.setBinaryStream(i++,in,Files.size(p));st.setString(i++,"2026-resource-import");st.setInt(i++,order++);st.executeUpdate();inserted++;}
      }
      c.commit(); System.out.println("SUMMARY files="+files.size()+" inserted="+inserted+" reused="+reused+" category="+category);
    }
  }
  static String ext(Path p){String n=p.getFileName().toString().toLowerCase(Locale.ROOT);int i=n.lastIndexOf('.');return i<0?"":n.substring(i+1);}
  static String sha(Path p)throws Exception{MessageDigest d=MessageDigest.getInstance("SHA-256");try(InputStream in=Files.newInputStream(p)){byte[] b=new byte[1024*1024];for(int n;(n=in.read(b))>0;)d.update(b,0,n);}return HexFormat.of().formatHex(d.digest()).toUpperCase(Locale.ROOT);}
}
