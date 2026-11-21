import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import java.nio.file.*;
import java.util.*;

/** Replaces selected OSS content objects with reviewed branded copies. Local originals remain untouched. */
public class PublishBrandedContentAssets {
    private record Item(Path file,String object){}
    public static void main(String[] args) throws Exception{
        if(args.length!=2) throw new IllegalArgumentException("Usage: <branded-image-dir> <branded-pdf-dir>");
        Path images=Path.of(args[0]), pdfs=Path.of(args[1]);
        List<Item> items=new ArrayList<>();
        String[][] imgs={{"doc_0701.png","07/0701.png"},{"doc_0702.png","07/0702.png"},{"doc_0703.png","07/0703.png"},{"doc_0704.png","07/0704.png"},
            {"doc_0801.png","08/0801.png"},{"doc_0802.png","08/0802.png"},{"doc_0803.png","08/0803.png"},{"doc_0901.png","09/0901.png"},{"doc_0902.png","09/0902.png"},
            {"doc_1101.png","11/1101.png"},{"doc_1102.png","11/1102.png"},{"doc_1103.png","11/1103.png"},{"doc_1104.png","11/1104.png"},
            {"2023上海中考科目和分值设置_无水印重制版.png","missing-subject-score/01.png"}};
        for(String[] x:imgs) items.add(new Item(images.resolve(x[0]),"admission-content/"+x[1]));
        items.add(new Item(pdfs.resolve("262_16区部分学校市重率_联朋品牌版.pdf"),"education-policy/19/262_16区部分学校市重率.pdf"));
        items.add(new Item(pdfs.resolve("276_上海16区小升初入学方式_联朋品牌版.pdf"),"education-policy/15/276_上海16区小升初入学方式.pdf"));
        items.add(new Item(pdfs.resolve("333_2026年到区分数线_联朋品牌版.pdf"),"education-policy/21/333_2026年到区分数线.pdf"));
        items.add(new Item(pdfs.resolve("337_26各区名额到校分数线(1)_联朋品牌版.pdf"),"education-policy/22/337_26各区名额到校分数线(1).pdf"));
        for(Item i:items) if(!Files.isRegularFile(i.file())||Files.size(i.file())==0) throw new IllegalStateException("Missing/empty: "+i.file());
        OSS oss=new OSSClientBuilder().build("oss-cn-shanghai.aliyuncs.com",env("OSS_ACCESS_KEY_ID"),env("OSS_SECRET"));
        try{
            for(Item i:items){oss.putObject("lianpeng",i.object(),i.file().toFile()); long n=oss.getObjectMetadata("lianpeng",i.object()).getContentLength();
                if(n!=Files.size(i.file())) throw new IllegalStateException("Length mismatch: "+i.object()); System.out.println("OK\t"+i.object()+"\t"+n);}
        }finally{oss.shutdown();}
    }
    private static String env(String n){String v=System.getenv(n);if(v==null||v.isBlank())throw new IllegalStateException("Missing "+n);return v;}
}
