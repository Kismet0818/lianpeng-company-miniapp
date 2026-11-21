import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;

/** Imports only page one of each PDF directly into MySQL; no OSS code. */
public class ImportFirstPageJdbc {
  public static void main(String[] args) throws Exception {
    if(args.length!=1) throw new IllegalArgumentException("Usage: ImportFirstPageJdbc <root>");
    Path root=Path.of(args[0]).toAbsolutePath().normalize(); List<Path> files;
    try(var s=Files.walk(root)){files=s.filter(Files::isRegularFile).filter(p->"pdf".equals(ext(p))).sorted().toList();}
    String url="jdbc:mysql://192.168.3.5:3306/lpsx?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true";
    String sql="INSERT INTO zhongkao_resource_file (resource_code,title,resource_category,subject_category,resource_type,target_year,source_year,original_file_name,relative_path,file_extension,mime_type,file_size,file_sha256,file_content,oss_url,source_batch,sort_order,status,is_deleted) VALUES (?,?,?,?,?,'2026',?,?,?,?,?,?,?,?,NULL,?,?,1,0) ON DUPLICATE KEY UPDATE title=VALUES(title),file_content=VALUES(file_content),file_size=VALUES(file_size),source_batch=VALUES(source_batch),sort_order=VALUES(sort_order),status=1,is_deleted=0";
    try(Connection c=DriverManager.getConnection(url,"root","Datou123456!")){c.setAutoCommit(false);int n=0;
      for(Path p:files){byte[] first=firstPage(p);String hash=sha(first), name=p.getFileName().toString(), title=name.substring(0,name.length()-4), cat=p.toString().contains("志愿填报")?"志愿填报工具包":(p.toString().contains("真题卷")?"中考真题卷":"自招配套资料"), code="z2026-first-"+hash.substring(0,16).toLowerCase(Locale.ROOT);
        try(PreparedStatement st=c.prepareStatement(sql);ByteArrayInputStream in=new ByteArrayInputStream(first)){int i=1;st.setString(i++,code);st.setString(i++,title);st.setString(i++,cat);st.setString(i++,cat);st.setString(i++,"PDF第一页");st.setString(i++,"");st.setString(i++,name);st.setString(i++,root.relativize(p).toString().replace('\\','/'));st.setString(i++,"pdf");st.setString(i++,"application/pdf");st.setInt(i++,first.length);st.setString(i++,hash);st.setBinaryStream(i++,in,first.length);st.setString(i++,"2026-first-page-import");st.setInt(i++,++n);st.executeUpdate();}
      } c.commit();System.out.println("SUMMARY files="+files.size()+" imported="+n);
    }
  }
  static byte[] firstPage(Path p)throws IOException{try(PDDocument src=Loader.loadPDF(p.toFile());PDDocument out=new PDDocument();ByteArrayOutputStream b=new ByteArrayOutputStream()){out.importPage(src.getPage(0));out.save(b);return b.toByteArray();}}
  static String ext(Path p){String n=p.getFileName().toString().toLowerCase(Locale.ROOT);int i=n.lastIndexOf('.');return i<0?"":n.substring(i+1);}
  static String sha(byte[] b)throws Exception{MessageDigest d=MessageDigest.getInstance("SHA-256");return HexFormat.of().formatHex(d.digest(b)).toUpperCase(Locale.ROOT);}
}
