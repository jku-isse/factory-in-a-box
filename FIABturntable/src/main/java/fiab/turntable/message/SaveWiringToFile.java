package fiab.turntable.message;

public class SaveWiringToFile {

    private final String fileName;

    public SaveWiringToFile(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
