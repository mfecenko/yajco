package yajco.model.type;

import yajco.annotation.After;
import yajco.annotation.Before;
import yajco.annotation.Exclude;

public class ListType extends ComponentType {
    private boolean hasSharedPart = false;

    @Before({"list", "of"})
    public ListType(Type componentType) {
        super(componentType);
    }

    @Before({"list", "of"})
    @After({"with", "shared", "part"})
    public ListType(Type componentType, boolean hasSharedPart) {
        super(componentType);
        this.hasSharedPart = hasSharedPart;
    }

    @Exclude
    public ListType(Type componentType, Object sourceElement) {
        super(componentType, sourceElement);
    }
    
    //needed for XML binding
    @Exclude
    private ListType() {
        super(null);
    }

    public boolean hasSharedPart() {
        return this.hasSharedPart;
    }
}
