package com.github.pires.obd.commands.fuel;

import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.enums.AvailableCommandNames;

public class FuelInjectionTimingCommand extends ObdCommand {

    private float fuelInjectionTiming = -1;

    public FuelInjectionTimingCommand() {
        super("01 5D");
    }

    public FuelInjectionTimingCommand(FuelInjectionTimingCommand other) {
        super(other);
    }

    @Override
    protected void performCalculations() {
        fuelInjectionTiming = (((256 * buffer.get(2)) + buffer.get(3)) / 128) - 120;
    }

    @Override
    public String getFormattedResult() {
        return String.format("%f%s", fuelInjectionTiming, getResultUnit());
    }

    @Override
    public String getCalculatedResult() {
        return String.valueOf(fuelInjectionTiming);
    }

    @Override
    public String getName() {
        return AvailableCommandNames.FUEL_INJECTION_TIMING.getValue();
    }

    @Override
    public String getResultUnit() {
        return "°";
    }
}
