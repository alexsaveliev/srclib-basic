package com.sourcegraph.toolchain.cpp;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedList;
import java.util.List;

import static com.sourcegraph.toolchain.cpp.antlr4.CPP14Parser.Identifier;

/**
 * Namespace-aware path
 */
class NSPath {

    /**
     * Individual path components (foo, bar, baz) for ::foo::bar::baz
     */
    LinkedList<String> components = new LinkedList<>();

    /**
     * Individual terminal nodes (foo, bar, baz) for ::foo::bar::baz
     */
    LinkedList<TerminalNode> nodes = new LinkedList<>();
    /**
     * Indicates if path is absolute (starts with ::)
     */
    boolean absolute;
    /**
     * Normalized path foo.bar.baz for (foo::bar::baz)
     */
    String path;

    /**
     * Local name (last component)
     */
    String local;

    /**
     * Local name (last component)
     */
    TerminalNode localCtx;

    NSPath(ParserRuleContext ctx) {

        if (ctx == null) {
            absolute = false;
            path = StringUtils.EMPTY;
            return;
        }

        List<TerminalNode> nodes = getNestedComponents(ctx);
        absolute = "::".equals(ctx.getStart().getText());
        StringBuilder pathBuilder = new StringBuilder();
        for (TerminalNode node : nodes) {
            String ident = node.getText();
            components.add(ident);
            this.nodes.add(node);
            if (pathBuilder.length() > 0) {
                pathBuilder.append(CPPParseTreeListener.PATH_SEPARATOR);
            }
            pathBuilder.append(ident);
            local = ident;
            localCtx = node;
        }
        path = pathBuilder.toString();
    }

    /**
     * Extracts nested components (identifiers) (foo, bar, baz from foo::bar::baz)
     */
    private static List<TerminalNode> getNestedComponents(ParserRuleContext ctx) {
        List<TerminalNode> ret = new LinkedList<>();
        collectNestedComponents(ctx, ret);
        return ret;
    }

    /**
     * Extracts nested components (identiiers) (foo, bar, baz from foo::bar::baz)
     */
    private static void collectNestedComponents(ParseTree ctx, List<TerminalNode> ret) {
        int count = ctx.getChildCount();
        for (int i = 0; i < count; i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof TerminalNode) {
                TerminalNode terminalNode = (TerminalNode) child;
                int type = terminalNode.getSymbol().getType();
                if (type == Identifier) {
                    ret.add(terminalNode);
                }
            } else {
                collectNestedComponents(child, ret);
            }
        }
    }

    NSPath parent() {
        NSPath ret = new NSPath(null);
        ret.absolute = absolute;
        if (!components.isEmpty()) {
            ret.components = new LinkedList<>(components.subList(0, components.size() - 1));
            if (!ret.components.isEmpty()) {
                ret.local = ret.components.getLast();
            }
        }
        if (!nodes.isEmpty()) {
            ret.nodes = new LinkedList<>(nodes.subList(0, nodes.size() - 1));
            if (!ret.nodes.isEmpty()) {
                ret.localCtx = ret.nodes.getLast();
            }
        }
        ret.path = StringUtils.join(ret.components, CPPParseTreeListener.PATH_SEPARATOR);
        return ret;
    }
}
