package org.tallison.tika.app.fx.emitters;

import java.util.Optional;

public class ValidationResult {

    public enum VALIDITY {
        OK,
        NOT_OK
    }

    public static ValidationResult OK = new ValidationResult(VALIDITY.OK);

    private final VALIDITY validity;
    private final Optional<String> title;
    private final Optional<String> header;
    private final Optional<String> msg;

    public ValidationResult(VALIDITY validity) {
        this.validity = validity;
        this.msg = Optional.empty();
        this.title = Optional.empty();
        this.header = Optional.empty();
    }

    public ValidationResult(VALIDITY validity, String title, String header, String msg) {
        this.validity = validity;
        this.title = Optional.of(title);
        this.header = Optional.of(header);
        this.msg = Optional.of(msg);
    }

    public VALIDITY getValidity() {
        return validity;
    }

    public Optional<String> getTitle() {
        return title;
    }

    public Optional<String> getHeader() {
        return header;
    }

    public Optional<String> getMsg() {
        return msg;
    }

    @Override
    public String toString() {
        return "ValidationResult{" + "validity=" + validity + ", title=" + title + ", header=" +
                header + ", msg=" + msg + '}';
    }
}
