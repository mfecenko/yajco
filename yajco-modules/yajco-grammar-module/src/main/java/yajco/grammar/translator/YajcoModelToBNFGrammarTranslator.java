package yajco.grammar.translator;

import yajco.grammar.NonterminalSymbol;
import yajco.grammar.Symbol;
import yajco.grammar.TerminalSymbol;
import yajco.grammar.bnf.Alternative;
import yajco.grammar.bnf.Grammar;
import yajco.grammar.bnf.Production;
import yajco.grammar.semlang.SemLangFactory;
import yajco.model.*;
import yajco.model.pattern.Pattern;
import yajco.model.pattern.impl.*;
import yajco.model.type.*;
import yajco.model.utilities.Utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class YajcoModelToBNFGrammarTranslator {

    public static final String DEFAULT_SYMBOL_NAME = "SYMBOL";
    private static final String DEFAULT_VAR_NAME = "val";
    private static final String DEFAULT_LIST_NAME = "list";
    private static final String DEFAULT_ELEMENT_NAME = "elem";
    private static final YajcoModelToBNFGrammarTranslator instance = new YajcoModelToBNFGrammarTranslator();
    private Language language;
    private Grammar grammar;
    private int arrayID;
    private int optionalID;

    private YajcoModelToBNFGrammarTranslator() {
        language = null;
        grammar = null;
        arrayID = 1;
        optionalID = 1;
    }

    public Grammar translate(Language language) {
        if (language == null) {
            throw new IllegalArgumentException("Parameter 'language' cannot be null!");
        }

        this.language = language;
        arrayID = 1;
        optionalID = 1;

        Concept mainConcept = language.getConcepts().get(0);
        NonterminalSymbol startSymbol = new NonterminalSymbol(mainConcept.getConceptName(), new ReferenceType(Utilities.getTopLevelParent(mainConcept), null), toPatternList(mainConcept.getPatterns()));

        //TODO: toto je Dominikov test, aby boli terminali vlozene v poradi v akom su zadefinovane prv
        // problem ak mame terminal, ktory nie je pouzity !!!
        grammar = new Grammar(startSymbol);
        for (TokenDef tokenDef : language.getTokens()) {
            grammar.addTerminal(new TerminalSymbol(tokenDef.getName(), null), tokenDef.getRegexp());
        }
        //koniec

        grammar.addNonterminal(startSymbol);
        for (int i = 1; i < language.getConcepts().size(); i++) {
            Concept concept = language.getConcepts().get(i);
            if (concept.getPattern(Operator.class) != null) {
                continue;
            }

            NonterminalSymbol conceptNonterminal = new NonterminalSymbol(concept.getConceptName(), new ReferenceType(concept, null), toPatternList(concept.getPatterns()));
            grammar.addNonterminal(conceptNonterminal);
        }

        for (Concept c : language.getConcepts()) {
            if (c.getPattern(Operator.class) == null) {
                grammar.addProduction(translateConcept(c));
            }
        }

        if (grammar.getOperatorPool().containsKey(0)) {
            processParenthesesOperator();
        }

        return grammar;
    }

    private Production translateConcept(Concept concept) {
        if (concept.getConcreteSyntax().isEmpty()) {
            return translateAbstractConcept(concept);
        }
        if (concept.getPattern(yajco.model.pattern.impl.Enum.class) != null) {
            return translateEnumConcept(concept);
        } else {
            return translateNonAbstractConcept(concept);
        }
    }

    private Production translateAbstractConcept(Concept concept) {
        NonterminalSymbol conceptNonterminal = grammar.getNonterminal(concept.getConceptName());
        List<Alternative> alternatives = new ArrayList<Alternative>();

        Parentheses parPattern = (Parentheses) concept.getPattern(Parentheses.class);
        if (parPattern != null) {
            Alternative parAlternative = new Alternative();
            NonterminalSymbol nonterminal = new NonterminalSymbol(conceptNonterminal.getName(), conceptNonterminal.getReturnType(), DEFAULT_VAR_NAME);

            parAlternative.addSymbol(getTerminalFor(parPattern.getLeft()));
            parAlternative.addSymbol(nonterminal);
            parAlternative.addSymbol(getTerminalFor(parPattern.getRight()));
            parAlternative.addActions(SemLangFactory.createReturnSymbolValueActions(nonterminal));

            alternatives.add(parAlternative);
            grammar.addOperatorAlternative(0, parAlternative);
        }

        for (Concept ddc : Utilities.getDirectDescendantConcepts(concept, language)) {
            Operator opPattern = (Operator) ddc.getPattern(Operator.class);
            if (opPattern != null) {
                Alternative opAlternative = translateNotation(ddc.getConcreteSyntax().get(0), ddc);
                opAlternative.addPattern(opPattern);
                alternatives.add(opAlternative);
                grammar.addOperatorAlternative(opPattern.getPriority(), opAlternative);
            } else {
                translateDescendatConcept(alternatives, ddc);
            }
        }

        return new Production(conceptNonterminal, alternatives, toPatternList(concept.getPatterns()));
    }

    private void translateDescendatConcept(List<Alternative> alternatives, Concept ddc) {
        NonterminalSymbol ddcNonterminal = new NonterminalSymbol(ddc.getConceptName(), new ReferenceType(ddc, null));
        ddcNonterminal.setVarName(DEFAULT_VAR_NAME);
        Alternative ddcAlternative = new Alternative();
        ddcAlternative.addSymbol(ddcNonterminal);
        ddcAlternative.addActions(SemLangFactory.createReturnSymbolValueActions(ddcNonterminal));
        alternatives.add(ddcAlternative);
    }

    private Production translateNonAbstractConcept(Concept concept) {
        NonterminalSymbol conceptNonterminal = grammar.getNonterminal(concept.getConceptName());
        List<Alternative> alternatives = new ArrayList<Alternative>();

        for (Notation notation : concept.getConcreteSyntax()) {
            alternatives.add(translateNotation(notation, concept));
        }
        for (Concept ddc : Utilities.getDirectDescendantConcepts(concept, language)) {
            translateDescendatConcept(alternatives, ddc);
        }

        return new Production(conceptNonterminal, alternatives, toPatternList(concept.getPatterns()));
    }

    private Production translateEnumConcept(Concept concept) {
        NonterminalSymbol conceptNonterminal = grammar.getNonterminal(concept.getConceptName());
        String enumType = Utilities.getFullConceptClassName(language, concept);
        List<Alternative> alternatives = new ArrayList<Alternative>(concept.getConcreteSyntax().size());

        for (Notation notation : concept.getConcreteSyntax()) {
            Alternative alternative = new Alternative();
            TokenPart tokenPart = (TokenPart) notation.getParts().get(0);
            alternative.addSymbol(translateTokenNotationPart(tokenPart));
            alternative.addActions(SemLangFactory.createEnumInstanceAndReturnActions(enumType, tokenPart.getToken()));

            alternatives.add(alternative);
        }

        return new Production(conceptNonterminal, alternatives, toPatternList(concept.getPatterns()));
    }

    private Alternative translateNotation(Notation notation, Concept concept) {
        Alternative alternative = new Alternative(null, null, toPatternList(notation.getPatterns()));
        List<Symbol> parameters = new ArrayList<Symbol>(notation.getParts().size());

        for (NotationPart part : notation.getParts()) {
            Symbol symbol;
            if (part instanceof TokenPart) {
                symbol = translateTokenNotationPart((TokenPart) part);
            } else if (part instanceof PropertyReferencePart) {
                symbol = translatePropertyRefNotationPart((PropertyReferencePart) part);
                parameters.add(symbol);
            } else if (part instanceof LocalVariablePart) {
                symbol = translateLocalVarPart((LocalVariablePart) part);
                parameters.add(symbol);
            } else if (part instanceof OptionalPart) {
                symbol = translateOptionalPart(concept, (OptionalPart) part);
                parameters.add(symbol);
            } else {
                throw new IllegalArgumentException("Unknown notation part: '" + part.getClass().getCanonicalName() + "'!");
            }
            alternative.addSymbol(symbol);
        }

//        Operator opPattern = (Operator) concept.getPattern(Operator.class);
        Factory factoryPattern = (Factory) notation.getPattern(Factory.class);
        if (factoryPattern != null) {
//			if (opPattern == null) {
            alternative.addActions(SemLangFactory.createRefResolverFactoryClassInstRegisterAndReturnActions(Utilities.getFullConceptClassName(language, concept), factoryPattern.getName(), parameters));
//			} else {
//				alternative.addActions(SemLangFactory.createFactoryClassInstanceAndReturnActions(Utilities.getFullConceptClassName(language, concept), factoryPattern.getName(), parameters));
//			}
        } else {
//			if (opPattern == null) {
            alternative.addActions(SemLangFactory.createRefResolverNewClassInstRegisterAndReturnActions(Utilities.getFullConceptClassName(language, concept), parameters));
//			} else {
//				alternative.addActions(SemLangFactory.createNewClassInstanceAndReturnActions(Utilities.getFullConceptClassName(language, concept), parameters));
//			}
        }

        return alternative;
    }

    private Symbol translateOptionalPart(Concept concept, OptionalPart optionalPart) {
        for (NotationPart notationPart : optionalPart.getParts()) {
            if (notationPart instanceof LocalVariablePart) {
                return translateOptionalLocalVariablePart(concept, (LocalVariablePart) notationPart);
            }
            if (notationPart instanceof PropertyReferencePart) {
                Symbol conceptNonterminal = translateOptionalPropertyReferencePart(concept, optionalPart, (PropertyReferencePart) notationPart);
                if (conceptNonterminal != null) {
                    return conceptNonterminal;
                }
            }
        }
        throw new RuntimeException("Cannot read Optional type!");
    }

    private Symbol translateOptionalPropertyReferencePart(Concept concept, OptionalPart optionalPart, PropertyReferencePart notationPart) {
        Type type = notationPart.getProperty().getType();
        if (type instanceof ComponentType) {
            ComponentType cmpType = (ComponentType) notationPart.getProperty().getType();

            Type innerType = cmpType.getComponentType();
            String name;
            if (innerType instanceof ReferenceType) {
                ReferenceType refType = (ReferenceType) innerType;
                name = refType.getConcept().getConceptName();
            } else if (innerType instanceof ComponentType) {
                name = notationPart.getProperty().getName();
            } else {
                TokenDef token = getDefinedToken(notationPart.getProperty().getName());
                name = token != null ? token.getName() : null;
            }

            List<Alternative> alternatives = new ArrayList<Alternative>();

            Alternative alternative1 = new Alternative();
            List<Symbol> symbols = new ArrayList<Symbol>(1);
            for (NotationPart part : optionalPart.getParts()) {
                Symbol symbol;
                if (part instanceof TokenPart) {
                    symbol = translateTokenNotationPart((TokenPart) part);
                } else if (part instanceof PropertyReferencePart) {
                    symbol = translatePropertyRefNotationPart((PropertyReferencePart) part);
                    alternative1.addActions(SemLangFactory.createNewOptionalClassInstanceAndReturnActions(Collections.singletonList(symbol)));
                } else if (part instanceof LocalVariablePart) {
                    symbol = translateLocalVarPart((LocalVariablePart) part);
                } else {
                    throw new IllegalArgumentException("Unknown notation part: '" + optionalPart.getClass().getCanonicalName() + "'!");
                }
                symbols.add(symbol);
            }

            alternative1.addSymbols(symbols);
            alternatives.add(alternative1);

            symbols = new ArrayList<Symbol>(1);
            Alternative alternative2 = new Alternative();
            alternative2.addSymbols(symbols);
            alternative2.addActions(SemLangFactory.createNewOptionalClassInstanceAndReturnActions(symbols));
            alternatives.add(alternative2);

            NonterminalSymbol conceptNonterminal = new NonterminalSymbol("Optional" + name + "_" +optionalID++,
                    new OptionalType(cmpType.getComponentType()), DEFAULT_ELEMENT_NAME+optionalID);

            Production production = new Production(conceptNonterminal, alternatives, toPatternList(concept.getPatterns()));
            Production existingProduction = grammar.getExistingProductionForOptionalNonterminal(conceptNonterminal.getName(), production);

            return addProductionAndGetNonterminal(conceptNonterminal, production, existingProduction);
        }
        return null;
    }

    private Symbol translateOptionalLocalVariablePart(Concept concept, LocalVariablePart notationPart) {
        if (!(notationPart.getType() instanceof PrimitiveType)) {
            throw new IllegalArgumentException("Type " + notationPart.getType() + " is not primitive!");
        }

        Token tokenPattern = (Token) notationPart.getPattern(Token.class);
        TokenDef token = getDefinedToken(tokenPattern != null ? tokenPattern.getName() : notationPart.getName());
        TerminalSymbol terminal = null;

        if (token != null) {
            terminal = new TerminalSymbol(token.getName(), notationPart.getType(), notationPart.getName(), toPatternList(notationPart.getPatterns()));
        }

        if (token != null && !grammar.getTerminals().containsKey(token.getName())) {
            grammar.addTerminal(terminal, token.getRegexp());
        }

        List<Alternative> alternatives = new ArrayList<Alternative>();

        Alternative alternative1 = new Alternative();
        List<Symbol> symbols = new ArrayList<Symbol>(1);

        symbols.add(terminal);

        alternative1.addSymbols(symbols);
        alternative1.addActions(SemLangFactory.createNewOptionalClassInstanceAndReturnActions(symbols));
        alternatives.add(alternative1);

        symbols = new ArrayList<Symbol>(1);
        Alternative alternative2 = new Alternative();
        alternative2.addSymbols(symbols);
        alternative2.addActions(SemLangFactory.createNewOptionalClassInstanceAndReturnActions(symbols));
        alternatives.add(alternative2);

        NonterminalSymbol conceptNonterminal = new NonterminalSymbol("Optional" + notationPart.getName() + "_" +optionalID++,
                new OptionalType(notationPart.getType()), DEFAULT_ELEMENT_NAME+optionalID);

        Production production = new Production(conceptNonterminal, alternatives, toPatternList(concept.getPatterns()));
        Production existingProduction = grammar.getExistingProductionForOptionalNonterminal(conceptNonterminal.getName(), production);

        return addProductionAndGetNonterminal(conceptNonterminal, production, existingProduction);
    }

    private Symbol addProductionAndGetNonterminal(NonterminalSymbol conceptNonterminal, Production production, Production existingProduction) {
        if (existingProduction != null) {
            grammar.addNonterminal(existingProduction.getLhs());
            grammar.addProduction(existingProduction);
            optionalID--;
            return existingProduction.getLhs();
        } else {
            grammar.addProduction(production);
            grammar.addNonterminal(conceptNonterminal);
            return conceptNonterminal;
        }
    }



    private TerminalSymbol translateTokenNotationPart(TokenPart part) {
        return getTerminalFor(part.getToken());
    }

    private TerminalSymbol getTerminalFor(String terminal) {
        if (terminal == null || terminal.isEmpty()) {
            return null;
        }
        TokenDef token = getDefinedToken(terminal);
        TerminalSymbol terminalSymbol;
        if (token != null) {
            terminalSymbol = new TerminalSymbol(token.getName(), null);
            grammar.addTerminal(terminalSymbol, token.getRegexp());
        } else {
            terminalSymbol = createTerminalFor(terminal);
        }

        return terminalSymbol;
    }

    private Symbol translatePropertyRefNotationPart(PropertyReferencePart part) {
        Type type = part.getProperty().getType();
        Symbol symbol;

        if (type instanceof ReferenceType) {
            ReferenceType refType = (ReferenceType) type;
            symbol = new NonterminalSymbol(refType.getConcept().getConceptName(), refType);
        } else if (type instanceof ComponentType) {
            symbol = translateComponentTypePropertyRef(part);
        } else {
            PrimitiveType primType = (PrimitiveType) type;
            Token tokenPattern = (Token) part.getPattern(Token.class);
            String tokenName = tokenPattern != null ? tokenPattern.getName() : part.getProperty().getName();
            TokenDef token = getDefinedToken(tokenName);
            TerminalSymbol terminal = null;
            if (token != null) {
                terminal = new TerminalSymbol(token.getName(), primType);
            }
            if (token != null && !grammar.getTerminals().containsKey(token.getName())) {
                grammar.addTerminal(terminal, token.getRegexp());
            }
            symbol = terminal;
        }

        if (symbol != null) {
            symbol.setVarName(part.getProperty().getName());
        }
        return symbol;
    }

    private Symbol translateLocalVarPart(LocalVariablePart part) {
        if (!(part.getType() instanceof PrimitiveType)) {
            throw new IllegalArgumentException("Type " + part.getType() + " is not primitive!");
        }

        Token tokenPattern = (Token) part.getPattern(Token.class);
        TokenDef token = getDefinedToken(tokenPattern != null ? tokenPattern.getName() : part.getName());
        TerminalSymbol terminal = null;
        if (token != null) {
            terminal = new TerminalSymbol(token.getName(), part.getType(), part.getName(), toPatternList(part.getPatterns()));
        }
        if (token != null && !grammar.getTerminals().containsKey(token.getName())) {
            grammar.addTerminal(terminal, token.getRegexp());
        }

        return terminal;
    }

    private Symbol translateComponentTypePropertyRef(PropertyReferencePart part) {
        ComponentType cmpType = (ComponentType) part.getProperty().getType();
        Type innerType = cmpType.getComponentType();
        Symbol symbol;
        String separator;
        int min, max;

        if (innerType instanceof ReferenceType) {
            ReferenceType refType = (ReferenceType) innerType;
            symbol = new NonterminalSymbol(refType.getConcept().getConceptName(), refType, DEFAULT_ELEMENT_NAME);
        } else if (innerType instanceof ComponentType) {
            symbol = translateOptionalComponentTypePropertyRef((ComponentType) innerType, part);
            symbol.setVarName("Optional" + part.getProperty().getName());
        } else {
            TokenDef token = getDefinedToken(part.getProperty().getName());
            symbol = new TerminalSymbol(token != null ? token.getName() : null, innerType, DEFAULT_ELEMENT_NAME);
            if (token != null && !grammar.getTerminals().containsKey(token.getName())) {
                grammar.addTerminal((TerminalSymbol) symbol, token.getRegexp());
            }
        }

        Separator sepPattern = (Separator) part.getPattern(Separator.class);
        Range rangePattern = (Range) part.getPattern(Range.class);
        separator = sepPattern != null ? sepPattern.getValue() : "";
        min = rangePattern != null ? rangePattern.getMinOccurs() : 0;
        max = rangePattern != null ? rangePattern.getMaxOccurs() : Range.INFINITY;

        NonterminalSymbol nonterminal = grammar.getSequenceNonterminalFor(symbol.toString(), min, max, separator);
        if (nonterminal != null) {
            return new NonterminalSymbol(nonterminal.getName(), cmpType, nonterminal.getVarName());
        } else {
            if (cmpType instanceof OptionalType) {
                return symbol;
            } else {
                return createSequenceProductionFor(symbol, min, max, separator, cmpType);
            }
        }
    }

    private NonterminalSymbol translateOptionalComponentTypePropertyRef(ComponentType cmpType, PropertyReferencePart part) {
        Type innerType = cmpType.getComponentType();
        Symbol symbol;
        String separator;
        int min, max;

        if (innerType instanceof ReferenceType) {
            ReferenceType refType = (ReferenceType) innerType;
            symbol = new NonterminalSymbol(refType.getConcept().getConceptName(), refType, DEFAULT_ELEMENT_NAME);
        } else {
            TokenDef token = getDefinedToken(part.getProperty().getName());
            symbol = new TerminalSymbol(token != null ? token.getName() : null, innerType, DEFAULT_ELEMENT_NAME);
            if (token != null && !grammar.getTerminals().containsKey(token.getName())) {
                grammar.addTerminal((TerminalSymbol) symbol, token.getRegexp());
            }
        }

        Separator sepPattern = (Separator) part.getPattern(Separator.class);
        Range rangePattern = (Range) part.getPattern(Range.class);
        separator = sepPattern != null ? sepPattern.getValue() : "";
        min = rangePattern != null ? rangePattern.getMinOccurs() : 1;
        max = rangePattern != null ? rangePattern.getMaxOccurs() : Range.INFINITY;

        NonterminalSymbol nonterminal = grammar.getSequenceNonterminalFor(symbol.toString(), min, max, separator);
        if (nonterminal != null) {
            return new NonterminalSymbol(nonterminal.getName(), cmpType, nonterminal.getVarName());
        } else {
            return createSequenceProductionFor(symbol, min, max, separator, cmpType);

        }
    }

    private NonterminalSymbol createSequenceProductionFor(Symbol symbol, int minOccurs, int maxOccurs, String separator, ComponentType cmpType) {
        NonterminalSymbol lhs = new NonterminalSymbol(symbol.getName() + "Array" + arrayID++, new ListType(cmpType.getComponentType()));
        grammar.addNonterminal(lhs);

        TerminalSymbol sepTerminal = getTerminalFor(separator);

        Production production = new Production(lhs);

        NonterminalSymbol rhsNonterminal = new NonterminalSymbol(lhs.getName(), lhs.getReturnType(), DEFAULT_LIST_NAME);
        if ((minOccurs == 0 || minOccurs == 1) && maxOccurs == Range.INFINITY) {
            Alternative alternative1 = new Alternative();
            Alternative alternative2 = new Alternative();
            Alternative alternative3 = new Alternative();

            alternative1.addSymbol(rhsNonterminal);
            if (sepTerminal != null) {
                alternative1.addSymbol(sepTerminal);
            }
            alternative1.addSymbol(symbol);
            alternative1.addActions(SemLangFactory.createAddElementToCollectionAndReturnActions(rhsNonterminal, symbol));

            if (minOccurs == 1) {
                alternative2.addSymbol(symbol);
                alternative2.addActions(SemLangFactory.createListAndAddElementAndReturnActions(cmpType.getComponentType(), DEFAULT_LIST_NAME, symbol));
            } else {
                alternative2.addActions(SemLangFactory.createListAndReturnActions(cmpType.getComponentType()));
                if (sepTerminal != null) {
                    alternative3.addSymbol(symbol);
                    alternative3.addActions(SemLangFactory.createListAndAddElementAndReturnActions(cmpType.getComponentType(), DEFAULT_LIST_NAME, symbol));
                }
            }

            production.addAlternative(alternative1);
            production.addAlternative(alternative2);
            if (!alternative3.isEmpty()) {
                production.addAlternative(alternative3);
            }
        } else {
            int symID = 1;
            List<Symbol> symbols = new ArrayList<Symbol>(maxOccurs);
            for (int i = 0; i < minOccurs; i++) {
                symbols.add(symbol instanceof NonterminalSymbol ? new NonterminalSymbol(symbol.getName(), symbol.getReturnType(), DEFAULT_VAR_NAME + symID++) : new TerminalSymbol(symbol.getName(), symbol.getReturnType(), DEFAULT_VAR_NAME + symID++));
            }

            for (int i = minOccurs; i <= maxOccurs; i++) {
                Alternative alternative = new Alternative();
                alternative.addSymbols(symbols);
                alternative.addActions(SemLangFactory.createListAndAddElementsAndReturnActions(cmpType.getComponentType(), DEFAULT_LIST_NAME, symbols));
                production.addAlternative(alternative);

                symbols.add(symbol instanceof NonterminalSymbol ? new NonterminalSymbol(symbol.getName(), symbol.getReturnType(), DEFAULT_VAR_NAME + symID++) : new TerminalSymbol(symbol.getName(), symbol.getReturnType(), DEFAULT_VAR_NAME + symID++));
            }
        }

        grammar.addProduction(production);
        grammar.addSequence(symbol.toString(), minOccurs, maxOccurs, separator, lhs);

        return new NonterminalSymbol(lhs.getName(), cmpType);
    }

    private void processParenthesesOperator() {
        List<Integer> priorities = new ArrayList(grammar.getOperatorPool().keySet());
        Collections.sort(priorities);
        Collections.reverse(priorities);
        priorities.remove(priorities.size() - 1);

        Integer foundPriority = -1;
        for (Integer priority : priorities) {
            Alternative opAlternative = grammar.getOperatorPool().get(priority).get(0);
            Operator opPattern = (Operator) opAlternative.getPattern(Operator.class);

            if (opPattern.getAssociativity() != Associativity.LEFT) {
                continue;
            }

            foundPriority = priority;
            break;
        }

        if (foundPriority != -1) {
            Alternative parAlternative = grammar.getOperatorPool().get(0).get(0);
            grammar.getOperatorPool().remove(0);

            parAlternative.addPattern(new Operator(foundPriority, Associativity.LEFT));
            parAlternative.addPattern(new Parentheses());
            grammar.getOperatorPool().get(foundPriority).add(parAlternative);
        } else {
            int newPriority = priorities.get(0) + 1;
            grammar.getOperatorPool().put(newPriority, grammar.getOperatorPool().get(0));
            grammar.getOperatorPool().remove(0);

            Alternative parAlternative = grammar.getOperatorPool().get(newPriority).get(0);
            parAlternative.addPattern(new Operator(newPriority, Associativity.LEFT));
            parAlternative.addPattern(new Parentheses());
        }
    }

    private List<Pattern> toPatternList(List<? extends Pattern> list) {
        List<Pattern> newList = new ArrayList<Pattern>(list.size());
        newList.addAll(list);

        return newList;
    }

    //	private List<Symbol> toSymbolList(Symbol symbol) {
//		List<Symbol> list = new ArrayList<Symbol>(1);
//		list.add(symbol);
//
//		return list;
//	}
    private TokenDef getDefinedToken(String name) {
        String upperCaseNotation = Utilities.toUpperCaseNotation(name);
        for (TokenDef token : language.getTokens()) {
            if (token.getName().equals(name) || token.getName().equals(upperCaseNotation)) {
                return token;
            }
        }

        if (name.endsWith("s")) {
            return getDefinedToken(name.substring(0, name.length() - 1));
        }

        return null;
    }

    private TerminalSymbol createTerminalFor(String regex) {
        TerminalSymbol terminal = grammar.getTerminal(regex);
        if (terminal == null) {
            terminal = new TerminalSymbol(DEFAULT_SYMBOL_NAME + regexToName(regex), null);
            grammar.addTerminal(terminal, regex);
        }

        return terminal;
    }

    private String regexToName(String regex) {
        boolean keepName = Character.isLetter(regex.charAt(0));
        if (keepName) {
            for (int i = 1; i < regex.length(); i++) {
                if (!Character.isLetterOrDigit(regex.charAt(i))) {
                    keepName = false;
                    break;
                }
            }
        }

        if (keepName) {
            return regex.toUpperCase();
        }

        StringBuilder builder = new StringBuilder(2 * regex.length());
        for (int i = 0; i < regex.length(); i++) {
            char sym = regex.charAt(i);
            if (Character.isLetterOrDigit(sym)) {
                builder.append(sym);
            } else {
                builder.append("_");
                builder.append(Integer.toString((int) sym));
                builder.append("_");
            }
        }
        if (builder.charAt(builder.length() - 1) == '_') {
            builder.setLength(builder.length() - 1);
        }

        return builder.toString();
    }

    public static YajcoModelToBNFGrammarTranslator getInstance() {
        return instance;
    }
}
