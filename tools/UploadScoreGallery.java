import com.example.lpsx.LpsxApplication;
import com.example.lpsx.service.impl.OssServiceImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/** Upload one score-query school's gallery without changing any database rows. */
public class UploadScoreGallery {
    public static void main(String[] args) throws Exception {
        if (args.length == 0 || args.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "Usage: UploadScoreGallery <schoolId> <imageDirectory> [<schoolId> <imageDirectory> ...]");
        }

        SpringApplication app = new SpringApplication(LpsxApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        try (ConfigurableApplicationContext context = app.run()) {
            OssServiceImpl oss = context.getBean(OssServiceImpl.class);
            for (int argIndex = 0; argIndex < args.length; argIndex += 2) {
                int schoolId = Integer.parseInt(args[argIndex]);
                Path directory = Path.of(args[argIndex + 1]).toAbsolutePath().normalize();
                uploadDirectory(oss, schoolId, directory);
            }
        }
    }

    private static void uploadDirectory(OssServiceImpl oss, int schoolId, Path directory) throws Exception {
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException("Image directory does not exist: " + directory);
        }

        List<Path> images;
        try (var paths = Files.list(directory)) {
            images = paths.filter(Files::isRegularFile)
                    .filter(UploadScoreGallery::isImage)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }
        if (images.isEmpty()) {
            throw new IllegalArgumentException("No images found in: " + directory);
        }

        System.out.println("SCHOOL_BEGIN\t" + schoolId + "\t" + directory.getFileName());
            for (int index = 0; index < images.size(); index++) {
                Path image = images.get(index);
                String extension = extension(image.getFileName().toString());
                String objectName = "score-query/school-gallery/" + schoolId + "/"
                        + String.format("%02d", index + 1) + extension;
                String url = oss.uploadLocalFile(image.toString(), objectName);
                System.out.println("UPLOAD_RESULT\t" + schoolId + "\t" + (index + 1) + "\t" + url);
            }
        System.out.println("SCHOOL_END\t" + schoolId + "\t" + images.size());
    }

    private static boolean isImage(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg")
                || name.endsWith(".jpeg") || name.endsWith(".webp");
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot).toLowerCase();
    }
}
