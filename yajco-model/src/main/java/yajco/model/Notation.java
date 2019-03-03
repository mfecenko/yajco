package yajco.model;

import java.util.ArrayList;
import java.util.List;
import yajco.annotation.After;
import yajco.annotation.Before;
import yajco.annotation.Exclude;
import yajco.annotation.Optional;
import yajco.annotation.Range;
import yajco.annotation.Separator;
import yajco.model.utilities.Utilities;
import yajco.model.pattern.NotationPattern;
import yajco.model.pattern.PatternSupport;

/**
 * Reprezentuje zapis jedneho konceptu (teda pravu stranu pravidla).
 * Sklada sa z notation part.
 */
public class Notation extends PatternSupport<NotationPattern> {

    private List<NotationPart> parts;
    private boolean unordered;

    public Notation(
            @Range(minOccurs = 1) NotationPart[] parts,
            @Optional @Before("{") @After("}") NotationPattern[] patterns) {
        super(Utilities.asList(patterns), null);
        this.parts = Utilities.asList(parts);
    }

    public Notation(
            @Range(minOccurs = 1) NotationPart[] parts) {
        super(new ArrayList<NotationPattern>(), null);
        this.parts = Utilities.asList(parts);
    }

    @Exclude
    public Notation(Object sourceElement, boolean unordered) {
        super(sourceElement);
        parts = new ArrayList<NotationPart>();
        this.unordered = unordered;
    }
    
    //needed for XML binding
    @Exclude
    private Notation() {
        super(null);
    }

    public List<NotationPart> getParts() {
        return parts;
    }

    public void addPart(NotationPart part) {
        parts.add(part);
    }

    public boolean isUnordered() {
        return unordered;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (NotationPart notationPart : parts) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(notationPart.toString());
            if (first) {
                first = false;
            }
            
        }
        return sb.toString();
    }
    
}
