package de.uniba.dsg.serverless.functions.mocky;


import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Response {

    private String message;

    @XmlElement
    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

}
