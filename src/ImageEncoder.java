import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class ImageEncoder {

    public static String encodeImageToBase64(String imagePath) throws IOException {
        try {
            BufferedImage image = ImageIO.read(new File(imagePath));

            // Create a ByteArrayOutputStream to hold the encoded image bytes
            ByteArrayOutputStream os = new ByteArrayOutputStream();

            // Write the image to the ByteArrayOutputStream in PNG format (you can change the format if needed)
            ImageIO.write(image, "png", os);

            // Get the byte array from the ByteArrayOutputStream
            byte[] imageBytes = os.toByteArray();

            // Use Base64 encoding to convert the byte array to a Base64-encoded string
            return Base64.getEncoder().encodeToString(imageBytes);
        } 
        catch (IOException e) { 
            e.printStackTrace();
            return null; 
        }
    }
}
