package simplemonatserechnung.model.data;

import org.mustangproject.SubjectCode;

public class Freitext {
    private String text;
    private SubjectCode subjectCode;

    public String getText() {
        return text;
    }

    public SubjectCode getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(SubjectCode subjectCode) {
        this.subjectCode = subjectCode;
    }

    public void setText(String text) {
        this.text = text;
    }
}
