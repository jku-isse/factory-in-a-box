package fiab.turntable.message;

public class ApplyWiringFromFile {

    private final String fileName;

    public ApplyWiringFromFile(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
