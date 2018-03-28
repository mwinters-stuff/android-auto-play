package com.github.pires.obd.commands.pressure;

import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.enums.AvailableCommandNames;

public class HybridBatteryPackRemaingLifeCommand extends ObdCommand {

    private float level = 0.0f;

    public HybridBatteryPackRemaingLifeCommand() {
        super("01 5B");
    }

    public HybridBatteryPackRemaingLifeCommand(ObdCommand other) {
        super(other);
    }

    @Override
    protected void performCalculations() {
        final int numerator = 100;
        final int denominator = 255;
        level = (numerator / denominator) * buffer.get(2);
    }

    @Override
    public String getFormattedResult() {
        return String.format("%f%s", level, getResultUnit());
    }

    @Override
    public String getCalculatedResult() {
        return String.valueOf(level);
    }

    @Override
    public String getName() {
        return AvailableCommandNames.HYBRID_BATTERY_PACK_REMAING_LIFE.getValue();
    }

    @Override
    public String getResultUnit() {
        return "%";
    }
}
