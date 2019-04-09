package ru.merkulyevsasha.chartscontest.sources;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Example {

    @SerializedName("columns")
    @Expose
    private List<List<String>> columns = null;
    @SerializedName("types")
    @Expose
    private Types types;
    @SerializedName("names")
    @Expose
    private Names names;
    @SerializedName("colors")
    @Expose
    private Colors colors;
    @SerializedName("percentage")
    @Expose
    private Boolean percentage;
    @SerializedName("stacked")
    @Expose
    private Boolean stacked;

    public List<List<String>> getColumns() {
        return columns;
    }

    public void setColumns(List<List<String>> columns) {
        this.columns = columns;
    }

    public Types getTypes() {
        return types;
    }

    public void setTypes(Types types) {
        this.types = types;
    }

    public Names getNames() {
        return names;
    }

    public void setNames(Names names) {
        this.names = names;
    }

    public Colors getColors() {
        return colors;
    }

    public void setColors(Colors colors) {
        this.colors = colors;
    }

    public Boolean getPercentage() {
        return percentage;
    }

    public void setPercentage(Boolean percentage) {
        this.percentage = percentage;
    }

    public Boolean getStacked() {
        return stacked;
    }

    public void setStacked(Boolean stacked) {
        this.stacked = stacked;
    }
}
