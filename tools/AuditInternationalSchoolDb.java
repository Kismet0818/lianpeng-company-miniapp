import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.regex.*;

public final class AuditInternationalSchoolDb {
  public static void main(String[] args) throws Exception {
    String y=Files.readString(Path.of("src/main/resources/application.yml"),StandardCharsets.UTF_8);
    String url=v(y,"url").replace("192.168.3.5","127.0.0.1");
    try(Connection c=DriverManager.getConnection(url,v(y,"username"),v(y,"password"))){
      try(var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) total,COUNT(DISTINCT name) names,SUM(is_active=1) active FROM school")){r.next();System.out.printf("school total=%d names=%d active=%d%n",r.getInt(1),r.getInt(2),r.getInt(3));}
      try(var s=c.prepareStatement("SELECT column_name,column_type,is_nullable,column_key FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='school' ORDER BY ordinal_position");var r=s.executeQuery()){while(r.next())System.out.printf("%s\t%s\t%s\t%s%n",r.getString(1),r.getString(2),r.getString(3),r.getString(4));}
    }
  }
  static String v(String y,String k){var m=Pattern.compile("(?m)^\\s*"+k+":\\s*([^\\r\\n#]+)").matcher(y);if(!m.find())throw new IllegalStateException(k);String x=m.group(1).trim();var p=Pattern.compile("\\$\\{([^:}]+)(?::([^}]*))?}").matcher(x);if(!p.matches())return x;String e=System.getenv(p.group(1));return e!=null&&!e.isBlank()?e:p.group(2);}
}
