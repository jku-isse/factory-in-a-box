package fiab.turntable.message;

public class DeleteWiringInfoFile {

    private final String fileName;

    public DeleteWiringInfoFile(String fileName){
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
