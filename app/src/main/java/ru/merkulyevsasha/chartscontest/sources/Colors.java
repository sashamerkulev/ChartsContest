package ru.merkulyevsasha.chartscontest.sources;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Colors {

    @SerializedName("y0")
    @Expose
    private String y0;
    @SerializedName("y1")
    @Expose
    private String y1;
    @SerializedName("y2")
    @Expose
    private String y2;
    @SerializedName("y3")
    @Expose
    private String y3;
    @SerializedName("y4")
    @Expose
    private String y4;
    @SerializedName("y5")
    @Expose
    private String y5;

    public String getY0() {
        return y0;
    }

    public void setY0(String y0) {
        this.y0 = y0;
    }

    public String getY1() {
        return y1;
    }

    public void setY1(String y1) {
        this.y1 = y1;
    }

    public String getY2() {
        return y2;
    }

    public void setY2(String y2) {
        this.y2 = y2;
    }

    public String getY3() {
        return y3;
    }

    public void setY3(String y3) {
        this.y3 = y3;
    }

    public String getY4() {
        return y4;
    }

    public void setY4(String y4) {
        this.y4 = y4;
    }

    public String getY5() {
        return y5;
    }

    public void setY5(String y5) {
        this.y5 = y5;
    }
}
