package com.finance.ui;

public final class Fmt {
    private Fmt() {}

    public static String money(double v) { return String.format("₹%,.2f", v); }

    public static String signedPct(Double v) { return v == null ? "—" : String.format("%+.1f%%", v); }

    public static String signedMoney(Double v) { return v == null ? "—" : String.format("%+,.0f", v); }
}
