package org.jphototagger.maintainance.browse;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * @author Elmar Baumann
 */
@XmlAccessorType(XmlAccessType.NONE)
public final class SqlCommand {

    @XmlElement(name = "description")
    private String description;

    @XmlElement(name = "sql")
    private String sql;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    @Override
    public String toString() {
        return description;
    }
}
