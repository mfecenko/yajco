package yajco.model.pattern.impl;

import yajco.annotation.After;
import yajco.annotation.Before;
import yajco.annotation.Exclude;
import yajco.model.pattern.NotationPartPattern;

public class Shared extends NotationPartPattern {

    private String value;

    @Before({"Shared", "part", "("})
    @After(")")
    public Shared(String sharedPart) {
        super(null);
        this.value = sharedPart;
    }

    @Exclude
    public Shared() {
        super(null);
    }

    @Exclude
    public Shared(String sharedPart, Object sourceElement) {
        super(sourceElement);
    }

    public String getValue() {
        return this.value;
    }
}
