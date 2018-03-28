package com.github.pires.obd.commands.pressure;

import com.github.pires.obd.enums.AvailableCommandNames;

public class FuelBasicRailPressureCommand extends PressureCommand {

    public FuelBasicRailPressureCommand() {
        super("01 22");
        useImperialUnits(false);
    }

    @Override
    protected final int preparePressureValue() {
        return (int) 0.079 * ((256 * buffer.get(2)) + buffer.get(3));
    }

    @Override
    public String getName() {
        return AvailableCommandNames.FUEL_BASIC_RAIL_PRESSURE.getValue();
    }
}
