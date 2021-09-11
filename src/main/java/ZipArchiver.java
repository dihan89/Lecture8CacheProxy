import java.io.*;
import java.util.zip.*;

public class ZipArchiver {
    final static int bufferLength = 1024;

    public static File compress(byte[] byteArrayInput, File directory, String outputFileName) {
        File outputFile = new File(directory, outputFileName);
        try (ZipOutputStream zipOutputStream =
                     new ZipOutputStream(new FileOutputStream(outputFile))) {
            ZipEntry zipEntry = new ZipEntry(outputFileName);
            zipOutputStream.putNextEntry(zipEntry);
            zipOutputStream.write(byteArrayInput, 0, byteArrayInput.length);
        } catch (IOException exc) {
            System.out.println("Cannot create zip-archive " + outputFile.getName() + ": " + exc.getMessage());
        }
        return outputFile;
    }

    public static byte[] expand(File inputFile) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        File outputFile = new File(inputFile.getName().replace(".zip", ".zzz"));
       try (ZipInputStream zipInputStream =
                     new ZipInputStream(new FileInputStream(inputFile));
             FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[bufferLength];
            while (zipInputStream.getNextEntry() != null) {
                int bufLength;
                while ((bufLength = zipInputStream.read(buffer, 0, bufferLength)) != -1) {
                    bos.write(buffer, 0, bufLength);
                }
            }
        } catch (FileNotFoundException exc) {
            System.out.println(String.format("File %s is not found", inputFile.getName()));
        } catch (IOException exc) {
            System.out.println("Cannot create zip-archive " + outputFile.getName() + ": " + exc.getMessage());
        }
        return bos.toByteArray();
    }
}




