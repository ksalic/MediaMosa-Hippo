package org.onehippo.forge.externalresource.reports.temp;

import java.io.Serializable;

public class Cluster implements Serializable {

    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private String name;

    private Long total;

    public Cluster(String name, Long total) {
        this.name = name;
        this.total = total;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(final Long total) {
        this.total = total;
    }
}
