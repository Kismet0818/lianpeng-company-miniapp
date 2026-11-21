import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

/** Fetches the public Shanghai international-school list and detail endpoints without auth data. */
public final class FetchXueweiguiSchools {
  static final ObjectMapper J=new ObjectMapper();
  static final HttpClient H=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  static final String CITY="-1,2703,2704,2705,2706,2707,2708,2709,2710,2711,2712,2713,2714,2715,2716,2717,2718,2719,2720,2721";
  public static void main(String[] a)throws Exception{
    Path dir=Path.of("tmp/xueweigui-schools-20260914"), out=Path.of("tmp/xueweigui-schools-20260914.json");Files.createDirectories(dir);
    LinkedHashMap<String,ObjectNode> rows=new LinkedHashMap<>();int page=1,total=Integer.MAX_VALUE;
    while(rows.size()<total){JsonNode root=get("https://v.xueweigui.com/ApiIntlV1/schoolList?city="+URLEncoder.encode(CITY,StandardCharsets.UTF_8)+"&current="+page);JsonNode data=root.path("data");total=data.path("total").asInt();for(JsonNode n:data.path("data")){ObjectNode o=(ObjectNode)n.deepCopy();rows.put(o.path("id").asText(),o);}J.writerWithDefaultPrettyPrinter().writeValue(dir.resolve("list-"+page+".json").toFile(),root);System.out.printf("LIST page=%d unique=%d total=%d%n",page,rows.size(),total);page++;Thread.sleep(350);}
    ArrayNode all=J.createArrayNode();int done=0;
    for(ObjectNode row:rows.values()){String id=row.path("id").asText();Path f=dir.resolve("detail-"+id+".json");JsonNode detail;if(Files.exists(f)){detail=J.readTree(f.toFile());}else{detail=get("https://v.xueweigui.com/ApiIntlV1/schoolDetail?id="+id);J.writerWithDefaultPrettyPrinter().writeValue(f.toFile(),detail);Thread.sleep(350);}ObjectNode item=J.createObjectNode();item.set("list",row);item.set("detail",detail.path("data"));all.add(item);done++;if(done%10==0||done==rows.size())System.out.printf("DETAIL %d/%d%n",done,rows.size());}
    ObjectNode result=J.createObjectNode();result.put("source","xueweigui-public-api");result.put("city","上海");result.put("count",all.size());result.set("schools",all);J.writerWithDefaultPrettyPrinter().writeValue(out.toFile(),result);System.out.println("OUTPUT="+out.toAbsolutePath());
  }
  static JsonNode get(String u)throws Exception{Exception last=null;for(int i=1;i<=3;i++){try{HttpRequest q=HttpRequest.newBuilder(URI.create(u)).timeout(Duration.ofSeconds(25)).header("Accept","application/json").header("User-Agent","Mozilla/5.0 (compatible; lpsx-data-import/1.0)").GET().build();HttpResponse<String> r=H.send(q,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));if(r.statusCode()!=200)throw new IllegalStateException("HTTP "+r.statusCode());JsonNode j=J.readTree(r.body());if(!"9999".equals(j.path("e").asText()))throw new IllegalStateException("API e="+j.path("e").asText());return j;}catch(Exception e){last=e;Thread.sleep(1000L*i);}}throw last;}
}
