#!/bin/bash
# Generate minimal test JPEG files using ImageMagick or Java

cd "$(dirname "$0")"

# If ImageMagick is available
if command -v convert &> /dev/null; then
    convert -size 100x100 xc:red test-photo-01.jpg
    convert -size 100x100 xc:green test-photo-02.jpg
    convert -size 100x100 xc:blue test-photo-03.jpg
    echo "Created test photos with ImageMagick"
    exit 0
fi

echo "ImageMagick not found. Creating test photos with Java."

# Create a temporary Java file to generate the photos
cat > GenerateTestPhotos.java << 'JAVA_EOF'
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class GenerateTestPhotos {
    public static void main(String[] args) throws Exception {
        createPhoto("test-photo-01.jpg", Color.RED);
        createPhoto("test-photo-02.jpg", Color.GREEN);
        createPhoto("test-photo-03.jpg", Color.BLUE);
        System.out.println("Successfully created all test photos");
    }
    
    static void createPhoto(String filename, Color color) throws Exception {
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, 100, 100);
        g.dispose();
        ImageIO.write(img, "JPEG", new File(filename));
        System.out.println("Created " + filename);
    }
}
JAVA_EOF

javac GenerateTestPhotos.java
java GenerateTestPhotos
rm GenerateTestPhotos.java GenerateTestPhotos.class
