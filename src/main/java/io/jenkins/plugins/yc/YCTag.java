package io.jenkins.plugins.yc;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.Objects;

public class YCTag extends AbstractDescribableImpl<YCTag> {
    private final String name;
    private final String value;

    /**
     * Tag name for the specific jenkins agent type tag, used to identify the YC instances provisioned by this plugin.
     */

    @DataBoundConstructor
    public YCTag(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /* Constructor from Amazon Tag *//*
    public YCTag(Tag t) {
        name = t.getKey();
        value = t.getValue();
    }*/

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "EC2Tag: " + name + "->" + value;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (this.getClass() != o.getClass())
            return false;

        YCTag other = (YCTag) o;
        if ((name == null && other.name != null) || (name != null && !name.equals(other.name)))
            return false;
        if ((value == null && other.value != null) || (value != null && !value.equals(other.value)))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<YCTag> {
        @Override
        public String getDisplayName() {
            return "";
        }
    }

    /* Helper method to convert lists of Amazon tags into internal format *//*
    public static List<YCTag> fromAmazonTags(List<Tag> amazonTags) {
        if (null == amazonTags) {
            return null;
        }

        LinkedList<YCTag> result = new LinkedList<YCTag>();
        for (Tag t : amazonTags) {
            result.add(new YCTag(t));
        }

        return result;
    }*/
}
