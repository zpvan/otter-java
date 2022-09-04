package com.ot.visual;

/**
 * https://operational-transformation.github.io/index.html
 */
public class Opt {

    public final Ation action;

    public int revision;

    public int clientId;

    public final int position;

    public String content;

    public Opt(Action action, int position, String content) {
        this(action, 0, position, content);
    }

    public Opt(Action action, int revisiton, int position, String content) {
        this.action = action;
        this.revision = revision;
        this.position = position;
        this.content = content;
    }

    @Override
    public String toString() {
        String manText = content.replace('\n', '\u00b6');
        return action + "(" + position + ", \"" + manText + "\"){" + revision + "}";
    }

    public enum Action {
        INSERT,
        DELETE,
        RETAIN,
        UPDATE,
        QUERY
    }
}