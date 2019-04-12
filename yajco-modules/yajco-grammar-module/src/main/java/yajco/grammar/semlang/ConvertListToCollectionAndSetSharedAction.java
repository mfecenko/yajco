package yajco.grammar.semlang;

import yajco.grammar.Symbol;
import yajco.model.type.ComponentType;
import yajco.model.type.Type;

public class ConvertListToCollectionAndSetSharedAction extends ConvertAction {
    private final ComponentType resultCollectionType;
    private final Symbol sharedSymbol;

    public ConvertListToCollectionAndSetSharedAction(ComponentType resultCollectionType, RValue rValue, Symbol sharedSymbol) {
        super(rValue);
        this.resultCollectionType = resultCollectionType;
        this.sharedSymbol = sharedSymbol;
    }

    public Type getResultCollectionInnerType() {
        return resultCollectionType.getComponentType();
    }

    public ComponentType getResultCollectionType() {
        return resultCollectionType;
    }

    public Symbol getSharedSymbol() {
        return sharedSymbol;
    }

    @Override
    public ActionType getActionType() {
        return ActionType.CONVERT_LIST_TO_COLLECTION_AND_SET_SHARED;
    }
}
