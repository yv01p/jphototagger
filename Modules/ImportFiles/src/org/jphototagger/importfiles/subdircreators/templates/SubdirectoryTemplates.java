package org.jphototagger.importfiles.subdircreators.templates;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Container for {@link SubdirectoryTemplate}s.
 *
 * @author Elmar Baumann
 */
@XmlRootElement(name = "SubdirectoryTemplates")
@XmlAccessorType(XmlAccessType.NONE)
public final class SubdirectoryTemplates {

    @XmlElement(name = "template")
    @XmlElementWrapper(name = "templates")
    private final List<SubdirectoryTemplate> templates = new ArrayList<>();

    /**
     * @return templates for modification
     */
    public List<SubdirectoryTemplate> getTemplates() {
        return templates;
    }
}
