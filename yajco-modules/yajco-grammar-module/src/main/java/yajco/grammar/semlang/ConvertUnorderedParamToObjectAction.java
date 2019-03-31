package yajco.grammar.semlang;

import yajco.model.type.Type;
import yajco.model.type.UnorderedParamType;

public class ConvertUnorderedParamToObjectAction extends ConvertAction {
    private final UnorderedParamType resultType;

    public ConvertUnorderedParamToObjectAction(UnorderedParamType resultType, RValue rValue) {
        super(rValue);
        this.resultType = resultType;
    }

    public Type getResultInnerType() {
        return resultType.getComponentType();
    }

    public Type getResultType() {
        return resultType;
    }

    @Override
    public ActionType getActionType() {
        return ActionType.CONVERT_UNORDERED_PARAM_TO_OBJECT;
    }
}
