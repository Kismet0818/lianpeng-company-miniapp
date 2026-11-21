import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/** 对已知同源公开学校分页路径做小规模只读探测，不携带用户身份。 */
public final class ProbeShangnaxueSchoolEndpoints {
  static final ObjectMapper J=new ObjectMapper();
  static final HttpClient H=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  public static void main(String[] args)throws Exception{
    String b="https://api.shangnaxue.net/cis/api/ma/school/";
    List<String> urls=List.of(
      b+"topList?size=2&current=1&cityId=101&segmentGradeId=2",
      b+"topList?size=2&current=1&cityId=1&segmentGradeId=2",
      b+"topList?size=20&current=1&cityId=101&segmentGradeId=2&isSysAppointment=&isSysMidway=",
      b+"topList?size=20&current=1&cityId=101&segmentGradeId=2&isSysAppointment=0&isSysMidway=0",
      b+"page?size=20&current=1&cityId=101&segmentGradeId=2",
      b+"pageApi?size=20&current=1&cityId=101&segmentGradeId=2",
      b+"list?size=20&current=1&cityId=101&segmentGradeId=2",
      b+"schoolPage?size=20&current=1&cityId=101&segmentGradeId=2",
      b+"queryPage?size=20&current=1&cityId=101&segmentGradeId=2"
    );
    for(String u:urls){
      HttpRequest q=HttpRequest.newBuilder(URI.create(u)).timeout(Duration.ofSeconds(20))
        .header("Accept","application/json").header("User-Agent","Mozilla/5.0").GET().build();
      try{
        HttpResponse<String> r=H.send(q,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        JsonNode j;try{j=J.readTree(r.body());}catch(Exception e){j=J.createObjectNode();}
        JsonNode d=j.path("data");
        System.out.printf("HTTP=%d code=%s total=%s records=%d path=%s%n",r.statusCode(),j.path("code").asText(),
          d.path("total").asText(),d.path("records").isArray()?d.path("records").size():0,URI.create(u).getPath()+"?"+URI.create(u).getQuery());
        if (URI.create(u).getPath().endsWith("/list") && d.path("records").isArray()) {
          for (JsonNode row : d.path("records")) System.out.printf("  id=%s name=%s city=%s area=%s level=%s levelId=%s%n",
            row.path("schoolId").asText(), row.path("name").asText(), row.path("cityName").asText(),
            row.path("areaName").asText(), row.path("segmentGradeName").asText(), row.path("segmentGradeId").asText());
        }
      }catch(Exception e){System.out.println("ERROR="+e.getClass().getSimpleName()+" path="+URI.create(u).getPath());}
      Thread.sleep(500);
    }
  }
}
